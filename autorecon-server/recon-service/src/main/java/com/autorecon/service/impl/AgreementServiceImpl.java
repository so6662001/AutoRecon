package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.AgreementConfirmDTO;
import com.autorecon.domain.dto.AgreementPublishDTO;
import com.autorecon.domain.entity.AgreementConfirmation;
import com.autorecon.domain.entity.AgreementVersion;
import com.autorecon.domain.entity.SysUser;
import com.autorecon.domain.vo.AgreementStatusVO;
import com.autorecon.domain.vo.AgreementVersionVO;
import com.autorecon.mapper.AgreementConfirmationMapper;
import com.autorecon.mapper.AgreementVersionMapper;
import com.autorecon.mapper.SysUserMapper;
import com.autorecon.service.AgreementService;
import com.autorecon.service.OssStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 协议版本与确认记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class AgreementServiceImpl implements AgreementService {

    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_DEPRECATED = 2;

    private static final int TYPE_USER = 1;
    private static final int TYPE_PRIVACY = 2;

    private static final int CONFIRM_LOGIN_POPUP = 1;

    private final AgreementVersionMapper agreementVersionMapper;
    private final AgreementConfirmationMapper agreementConfirmationMapper;
    private final SysUserMapper sysUserMapper;
    private final OssStorageService ossStorageService;

    @Override
    public Long publishAgreement(AgreementPublishDTO dto) {
        Integer require = dto.getRequireReconfirm() != null ? dto.getRequireReconfirm() : 1;
        AgreementVersion version = AgreementVersion.builder()
                .agreementType(dto.getAgreementType())
                .versionNo(dto.getVersionNo().trim())
                .title(dto.getTitle().trim())
                .content(dto.getContent())
                .summary(dto.getSummary())
                .effectiveDate(dto.getEffectiveDate())
                .publishedBy(SecurityUtil.getCurrentUserId())
                .publishedAt(LocalDateTime.now())
                .status(STATUS_PUBLISHED)
                .requireReconfirm(require)
                .build();
        agreementVersionMapper.insert(version);

        if (StringUtils.hasText(dto.getContent())) {
            String url = archiveToOss(version.getId(), dto.getContent());
            if (url != null) {
                version.setContentUrl(url);
                agreementVersionMapper.updateById(version);
            }
        }

        LambdaUpdateWrapper<AgreementVersion> deprecate = new LambdaUpdateWrapper<>();
        deprecate.eq(AgreementVersion::getAgreementType, dto.getAgreementType())
                .ne(AgreementVersion::getId, version.getId())
                .set(AgreementVersion::getStatus, STATUS_DEPRECATED);
        agreementVersionMapper.update(null, deprecate);

        log.info("Published agreement type={} version={} id={}", dto.getAgreementType(), dto.getVersionNo(), version.getId());
        return version.getId();
    }

    @Override
    public List<AgreementVersion> listVersions(Integer agreementType) {
        LambdaQueryWrapper<AgreementVersion> q = new LambdaQueryWrapper<>();
        if (agreementType != null) {
            q.eq(AgreementVersion::getAgreementType, agreementType);
        }
        q.orderByDesc(AgreementVersion::getEffectiveDate).orderByDesc(AgreementVersion::getId);
        return agreementVersionMapper.selectList(q);
    }

    @Override
    public AgreementVersion getVersion(Long versionId) {
        return agreementVersionMapper.selectById(versionId);
    }

    @Override
    public AgreementStatusVO checkAgreementStatus(Long userId) {
        AgreementStatusVO vo = AgreementStatusVO.builder()
                .needConfirm(false)
                .unconfirmedAgreements(new ArrayList<>())
                .build();
        if (userId == null) {
            return vo;
        }
        for (int type : new int[]{TYPE_USER, TYPE_PRIVACY}) {
            AgreementVersion current = findCurrentPublished(type);
            if (current == null) {
                continue;
            }
            if (current.getRequireReconfirm() != null && current.getRequireReconfirm() == 0) {
                continue;
            }
            if (hasConfirmedVersion(userId, current.getId())) {
                continue;
            }
            vo.getUnconfirmedAgreements().add(toVo(current));
        }
        vo.setNeedConfirm(!vo.getUnconfirmedAgreements().isEmpty());
        return vo;
    }

    @Override
    public void confirmAgreement(Long userId, AgreementConfirmDTO dto, String ip, String userAgent) {
        AgreementVersion version = agreementVersionMapper.selectById(dto.getAgreementVersionId());
        if (version == null) {
            throw new BizException(ErrorCode.AGREEMENT_VERSION_NOT_FOUND);
        }
        if (!version.getAgreementType().equals(dto.getAgreementType())) {
            throw new BizException(ErrorCode.AGREEMENT_TYPE_MISMATCH);
        }
        if (version.getStatus() == null || version.getStatus() != STATUS_PUBLISHED) {
            throw new BizException(ErrorCode.AGREEMENT_VERSION_NOT_FOUND);
        }

        SysUser user = sysUserMapper.selectById(userId);
        Long enterpriseId = user != null ? user.getEnterpriseId() : null;

        String body = StringUtils.hasText(version.getContent()) ? version.getContent() : "";
        String snapshotPath = buildConfirmationSnapshotPath(userId, dto.getAgreementType(), version.getVersionNo());
        String snapshotUrl = ossStorageService.upload(snapshotPath, body);

        AgreementConfirmation row = AgreementConfirmation.builder()
                .userId(userId)
                .enterpriseId(enterpriseId)
                .agreementVersionId(version.getId())
                .agreementType(dto.getAgreementType())
                .versionNo(version.getVersionNo())
                .confirmedAt(LocalDateTime.now())
                .confirmMethod(CONFIRM_LOGIN_POPUP)
                .ipAddress(truncate(ip, 50))
                .userAgent(truncate(userAgent, 500))
                .contentSnapshotUrl(snapshotUrl)
                .build();
        agreementConfirmationMapper.insert(row);
        log.info("User {} confirmed agreement version {}", userId, version.getId());
    }

    @Override
    public AgreementVersion getCurrentVersion(Integer agreementType) {
        return findCurrentPublished(agreementType);
    }

    @Override
    public List<AgreementConfirmation> getUserConfirmations(Long userId) {
        LambdaQueryWrapper<AgreementConfirmation> q = new LambdaQueryWrapper<>();
        q.eq(AgreementConfirmation::getUserId, userId)
                .orderByDesc(AgreementConfirmation::getConfirmedAt);
        return agreementConfirmationMapper.selectList(q);
    }

    @Override
    public String archiveToOss(Long versionId, String content) {
        AgreementVersion version = agreementVersionMapper.selectById(versionId);
        if (version == null) {
            return null;
        }
        String folder = version.getAgreementType() == TYPE_USER ? "user-agreement" : "privacy-policy";
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String safeVer = version.getVersionNo() == null ? "v" : version.getVersionNo().replaceAll("[^a-zA-Z0-9._-]", "_");
        String path = String.format("agreements/%s/%s_%s.html", folder, safeVer, datePart);
        return ossStorageService.upload(path, content != null ? content : "");
    }

    private AgreementVersion findCurrentPublished(Integer agreementType) {
        LambdaQueryWrapper<AgreementVersion> q = new LambdaQueryWrapper<>();
        q.eq(AgreementVersion::getAgreementType, agreementType)
                .eq(AgreementVersion::getStatus, STATUS_PUBLISHED)
                .orderByDesc(AgreementVersion::getPublishedAt)
                .orderByDesc(AgreementVersion::getId)
                .last("LIMIT 1");
        return agreementVersionMapper.selectOne(q);
    }

    private boolean hasConfirmedVersion(Long userId, Long versionId) {
        LambdaQueryWrapper<AgreementConfirmation> q = new LambdaQueryWrapper<>();
        q.eq(AgreementConfirmation::getUserId, userId)
                .eq(AgreementConfirmation::getAgreementVersionId, versionId);
        return agreementConfirmationMapper.selectCount(q) > 0;
    }

    private static AgreementVersionVO toVo(AgreementVersion v) {
        return AgreementVersionVO.builder()
                .id(v.getId())
                .agreementType(v.getAgreementType())
                .versionNo(v.getVersionNo())
                .title(v.getTitle())
                .content(v.getContent())
                .summary(v.getSummary())
                .effectiveDate(v.getEffectiveDate())
                .publishedAt(v.getPublishedAt())
                .contentUrl(v.getContentUrl())
                .build();
    }

    private static String buildConfirmationSnapshotPath(Long userId, Integer agreementType, String versionNo) {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String safeVer = versionNo == null ? "v" : versionNo.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("confirmations/%d/%d_v%s_%s.html", userId, agreementType, safeVer, ts);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    @Override
    public void deprecateVersion(Long versionId) {
        AgreementVersion version = agreementVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BizException(ErrorCode.AGREEMENT_VERSION_NOT_FOUND);
        }
        version.setStatus(STATUS_DEPRECATED);
        agreementVersionMapper.updateById(version);
        log.info("Deprecated agreement version id={}", versionId);
    }
}
