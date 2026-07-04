package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.AuthorizationItemDTO;
import com.autorecon.domain.entity.AuthorizationChangeNotification;
import com.autorecon.domain.entity.Enterprise;
import com.autorecon.domain.entity.EnterpriseDataAuthorization;
import com.autorecon.domain.entity.SysUser;
import com.autorecon.domain.vo.AuthorizationCheckResult;
import com.autorecon.domain.vo.EnterpriseAuthorizationVO;
import com.autorecon.mapper.AuthorizationChangeNotificationMapper;
import com.autorecon.mapper.EnterpriseDataAuthorizationMapper;
import com.autorecon.mapper.EnterpriseMapper;
import com.autorecon.mapper.SysUserMapper;
import com.autorecon.service.AuditLogService;
import com.autorecon.service.DataAuthorizationService;
import com.autorecon.service.OssStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 企业数据授权
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class DataAuthorizationServiceImpl implements DataAuthorizationService {

    public static final String TYPE_BASIC_SERVICE = "BASIC_SERVICE";
    public static final String TYPE_AI_TRAINING = "AI_TRAINING";
    public static final String TYPE_INDUSTRY_INDEX = "INDUSTRY_INDEX";
    public static final String TYPE_CREDIT_ASSESSMENT = "CREDIT_ASSESSMENT";
    public static final String TYPE_BUSINESS_INSIGHT = "BUSINESS_INSIGHT";
    public static final String TYPE_ANOMALY_DETECTION = "ANOMALY_DETECTION";

    private static final int METHOD_REGISTER = 1;
    private static final int METHOD_ADMIN = 2;
    private static final int METHOD_CHANGE = 3;

    private static final int RESPONSE_PENDING = 0;
    private static final int RESPONSE_ACCEPTED = 1;
    private static final int RESPONSE_REJECTED = 2;

    /** seller_admin, buyer_admin, platform_admin */
    private static final int ROLE_SELLER_ADMIN = 1;
    private static final int ROLE_BUYER_ADMIN = 4;
    private static final int ROLE_PLATFORM_ADMIN = 6;

    private static final Map<String, String> TYPE_NAMES = new LinkedHashMap<>();

    static {
        TYPE_NAMES.put(TYPE_BASIC_SERVICE, "基础服务(必选)");
        TYPE_NAMES.put(TYPE_AI_TRAINING, "AI模型训练");
        TYPE_NAMES.put(TYPE_INDUSTRY_INDEX, "行业指数与基准");
        TYPE_NAMES.put(TYPE_CREDIT_ASSESSMENT, "信用评估与风控");
        TYPE_NAMES.put(TYPE_BUSINESS_INSIGHT, "商业洞察与推荐");
        TYPE_NAMES.put(TYPE_ANOMALY_DETECTION, "异常交易监测");
    }

    private final EnterpriseDataAuthorizationMapper enterpriseDataAuthorizationMapper;
    private final AuthorizationChangeNotificationMapper authorizationChangeNotificationMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final SysUserMapper sysUserMapper;
    private final OssStorageService ossStorageService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Override
    public void initializeForEnterprise(Long enterpriseId, Long adminUserId, String ip, String userAgent) {
        assertCanManageEnterprise(enterpriseId);
        if (!needsInitialAuthorization(enterpriseId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String ipSafe = truncate(ip, 50);
        String uaSafe = truncate(userAgent, 500);
        for (String type : TYPE_NAMES.keySet()) {
            EnterpriseDataAuthorization row = EnterpriseDataAuthorization.builder()
                    .enterpriseId(enterpriseId)
                    .authorizationType(type)
                    .authorized(1)
                    .authorizedBy(adminUserId)
                    .authorizedAt(now)
                    .authorizationMethod(METHOD_REGISTER)
                    .ipAddress(ipSafe)
                    .userAgent(uaSafe)
                    .revoked(0)
                    .versionNo("v1.0")
                    .build();
            enterpriseDataAuthorizationMapper.insert(row);
        }
        Map<String, Boolean> allOn = new LinkedHashMap<>();
        for (String type : TYPE_NAMES.keySet()) {
            allOn.put(type, true);
        }
        String snapshot = buildSnapshotJson(enterpriseId, allOn);
        String url = uploadSnapshot(enterpriseId, "init", snapshot);
        if (url != null) {
            LambdaQueryWrapper<EnterpriseDataAuthorization> q = new LambdaQueryWrapper<>();
            q.eq(EnterpriseDataAuthorization::getEnterpriseId, enterpriseId);
            List<EnterpriseDataAuthorization> list = enterpriseDataAuthorizationMapper.selectList(q);
            for (EnterpriseDataAuthorization e : list) {
                e.setSnapshotUrl(url);
                e.setVersionNo("v1.0");
                enterpriseDataAuthorizationMapper.updateById(e);
            }
        }
        auditLogService.log("data_authorization", "initialize", "enterprise", enterpriseId,
                "初始化企业数据授权", ipSafe, uaSafe);
    }

    @Override
    public EnterpriseAuthorizationVO getAuthorizationStatus(Long enterpriseId) {
        assertEnterpriseScope(enterpriseId);
        Enterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        String enterpriseName = enterprise != null ? enterprise.getCompanyName() : null;

        LambdaQueryWrapper<EnterpriseDataAuthorization> q = new LambdaQueryWrapper<>();
        q.eq(EnterpriseDataAuthorization::getEnterpriseId, enterpriseId);
        List<EnterpriseDataAuthorization> rows = enterpriseDataAuthorizationMapper.selectList(q);
        Map<String, EnterpriseDataAuthorization> byType = rows.stream()
                .collect(Collectors.toMap(EnterpriseDataAuthorization::getAuthorizationType, r -> r, (a, b) -> a));

        List<EnterpriseAuthorizationVO.AuthorizationDetail> details = new ArrayList<>();
        for (Map.Entry<String, String> e : TYPE_NAMES.entrySet()) {
            String type = e.getKey();
            EnterpriseDataAuthorization row = byType.get(type);
            boolean auth = row != null && intEq(row.getAuthorized(), 1) && !intEq(row.getRevoked(), 1);
            details.add(EnterpriseAuthorizationVO.AuthorizationDetail.builder()
                    .type(type)
                    .typeName(e.getValue())
                    .authorized(auth)
                    .authorizedAt(row != null ? row.getAuthorizedAt() : null)
                    .revocable(!TYPE_BASIC_SERVICE.equals(type))
                    .build());
        }

        LambdaQueryWrapper<AuthorizationChangeNotification> nq = new LambdaQueryWrapper<>();
        nq.eq(AuthorizationChangeNotification::getEnterpriseId, enterpriseId)
                .eq(AuthorizationChangeNotification::getResponseStatus, RESPONSE_PENDING)
                .orderByDesc(AuthorizationChangeNotification::getNotifiedAt)
                .orderByDesc(AuthorizationChangeNotification::getId);
        List<AuthorizationChangeNotification> pending = authorizationChangeNotificationMapper.selectList(nq);
        List<EnterpriseAuthorizationVO.ChangeNotification> pendingVo = pending.stream()
                .map(this::toChangeVo)
                .collect(Collectors.toList());

        return EnterpriseAuthorizationVO.builder()
                .enterpriseId(enterpriseId)
                .enterpriseName(enterpriseName)
                .authorizations(details)
                .pendingChanges(pendingVo)
                .build();
    }

    @Override
    public void updateAuthorizations(Long enterpriseId, List<AuthorizationItemDTO> items, String ip, String userAgent) {
        assertCanManageEnterprise(enterpriseId);
        if (items == null || items.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        Map<String, Boolean> wanted = new LinkedHashMap<>();
        for (AuthorizationItemDTO item : items) {
            if (item.getAuthorizationType() == null || !TYPE_NAMES.containsKey(item.getAuthorizationType())) {
                throw new BizException(ErrorCode.BAD_REQUEST);
            }
            wanted.put(item.getAuthorizationType(), Boolean.TRUE.equals(item.getAuthorized()));
        }
        if (wanted.containsKey(TYPE_BASIC_SERVICE) && !wanted.get(TYPE_BASIC_SERVICE)) {
            throw new BizException(ErrorCode.DATA_AUTH_BASIC_CANNOT_REVOKE);
        }

        LambdaQueryWrapper<EnterpriseDataAuthorization> q = new LambdaQueryWrapper<>();
        q.eq(EnterpriseDataAuthorization::getEnterpriseId, enterpriseId);
        List<EnterpriseDataAuthorization> existing = enterpriseDataAuthorizationMapper.selectList(q);
        if (existing.isEmpty()) {
            throw new BizException(ErrorCode.DATA_AUTH_NOT_INITIALIZED);
        }
        Map<String, EnterpriseDataAuthorization> byType = existing.stream()
                .collect(Collectors.toMap(EnterpriseDataAuthorization::getAuthorizationType, r -> r, (a, b) -> a));

        Long userId = SecurityUtil.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        String ipSafe = truncate(ip, 50);
        String uaSafe = truncate(userAgent, 500);

        Map<String, Boolean> fullState = new LinkedHashMap<>();
        for (String type : TYPE_NAMES.keySet()) {
            fullState.put(type, byType.containsKey(type) && intEq(byType.get(type).getAuthorized(), 1) && !intEq(byType.get(type).getRevoked(), 1));
        }
        for (Map.Entry<String, Boolean> e : wanted.entrySet()) {
            fullState.put(e.getKey(), e.getValue());
        }
        fullState.put(TYPE_BASIC_SERVICE, true);

        String snapshotJson = buildSnapshotJson(enterpriseId, fullState);
        String snapshotUrl = uploadSnapshot(enterpriseId, "update", snapshotJson);

        for (Map.Entry<String, Boolean> e : wanted.entrySet()) {
            String type = e.getKey();
            EnterpriseDataAuthorization row = byType.get(type);
            if (row == null) {
                continue;
            }
            boolean on = e.getValue();
            row.setAuthorized(on ? 1 : 0);
            row.setAuthorizedBy(userId);
            row.setAuthorizedAt(now);
            row.setAuthorizationMethod(METHOD_ADMIN);
            row.setIpAddress(ipSafe);
            row.setUserAgent(uaSafe);
            if (!on && !TYPE_BASIC_SERVICE.equals(type)) {
                row.setRevoked(1);
                row.setRevokedAt(now);
                row.setRevokedBy(userId);
            } else {
                row.setRevoked(0);
                row.setRevokedAt(null);
                row.setRevokedBy(null);
            }
            if (snapshotUrl != null) {
                row.setSnapshotUrl(snapshotUrl);
            }
            row.setVersionNo("v1.0");
            enterpriseDataAuthorizationMapper.updateById(row);
        }

        auditLogService.log("data_authorization", "update", "enterprise", enterpriseId,
                "更新数据授权: " + snapshotJson, ipSafe, uaSafe);
    }

    @Override
    public void revokeAuthorization(Long enterpriseId, String authorizationType) {
        assertCanManageEnterprise(enterpriseId);
        if (TYPE_BASIC_SERVICE.equals(authorizationType)) {
            throw new BizException(ErrorCode.DATA_AUTH_BASIC_CANNOT_REVOKE);
        }
        if (!TYPE_NAMES.containsKey(authorizationType)) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        LambdaQueryWrapper<EnterpriseDataAuthorization> q = new LambdaQueryWrapper<>();
        q.eq(EnterpriseDataAuthorization::getEnterpriseId, enterpriseId)
                .eq(EnterpriseDataAuthorization::getAuthorizationType, authorizationType);
        EnterpriseDataAuthorization row = enterpriseDataAuthorizationMapper.selectOne(q);
        if (row == null) {
            throw new BizException(ErrorCode.DATA_AUTH_NOT_INITIALIZED);
        }
        Long userId = SecurityUtil.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        row.setAuthorized(0);
        row.setRevoked(1);
        row.setRevokedAt(now);
        row.setRevokedBy(userId);
        row.setAuthorizedBy(userId);
        row.setAuthorizedAt(now);
        row.setAuthorizationMethod(METHOD_ADMIN);
        enterpriseDataAuthorizationMapper.updateById(row);
        auditLogService.log("data_authorization", "revoke", "authorization_type", row.getId(),
                "撤回授权类型: " + authorizationType, null, null);
    }

    @Override
    public boolean isAuthorized(Long enterpriseId, String authorizationType) {
        if (enterpriseId == null || !StringUtils.hasText(authorizationType)) {
            return false;
        }
        LambdaQueryWrapper<EnterpriseDataAuthorization> q = new LambdaQueryWrapper<>();
        q.eq(EnterpriseDataAuthorization::getEnterpriseId, enterpriseId)
                .eq(EnterpriseDataAuthorization::getAuthorizationType, authorizationType)
                .eq(EnterpriseDataAuthorization::getAuthorized, 1)
                .eq(EnterpriseDataAuthorization::getRevoked, 0);
        return enterpriseDataAuthorizationMapper.selectCount(q) > 0;
    }

    @Override
    public boolean needsInitialAuthorization(Long enterpriseId) {
        if (enterpriseId == null) {
            return true;
        }
        LambdaQueryWrapper<EnterpriseDataAuthorization> q = new LambdaQueryWrapper<>();
        q.eq(EnterpriseDataAuthorization::getEnterpriseId, enterpriseId);
        return enterpriseDataAuthorizationMapper.selectCount(q) == 0;
    }

    @Override
    public void notifyAuthorizationChange(String changeType, String summary, String newTypes) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.DATA_AUTH_FORBIDDEN);
        }
        SysUser admin = findEnterpriseAdminUser(enterpriseId);
        Long notifiedUserId = admin != null ? admin.getId() : SecurityUtil.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        AuthorizationChangeNotification n = AuthorizationChangeNotification.builder()
                .enterpriseId(enterpriseId)
                .changeType(changeType)
                .changeSummary(summary)
                .newAuthorizationTypes(newTypes)
                .notifiedUserId(notifiedUserId)
                .notifiedAt(now)
                .responseStatus(RESPONSE_PENDING)
                .build();
        authorizationChangeNotificationMapper.insert(n);
    }

    @Override
    public void respondToChange(Long notificationId, boolean accept, String detail) {
        Long enterpriseId = SecurityUtil.getCurrentEnterpriseId();
        Long userId = SecurityUtil.getCurrentUserId();
        if (enterpriseId == null || userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        AuthorizationChangeNotification n = authorizationChangeNotificationMapper.selectById(notificationId);
        if (n == null || !Objects.equals(n.getEnterpriseId(), enterpriseId)) {
            throw new BizException(ErrorCode.DATA_AUTH_NOTIFICATION_NOT_FOUND);
        }
        SysUser me = sysUserMapper.selectById(userId);
        if (!canRespondToNotification(me, n)) {
            throw new BizException(ErrorCode.DATA_AUTH_FORBIDDEN);
        }
        n.setResponseStatus(accept ? RESPONSE_ACCEPTED : RESPONSE_REJECTED);
        n.setRespondedAt(LocalDateTime.now());
        n.setResponseDetail(detail);
        authorizationChangeNotificationMapper.updateById(n);
        auditLogService.log("data_authorization", accept ? "change_accept" : "change_reject",
                "authorization_notification", notificationId, detail != null ? detail : "", null, null);
    }

    @Override
    public AuthorizationCheckResult checkAuthorization(Long enterpriseId) {
        boolean basic = isAuthorized(enterpriseId, TYPE_BASIC_SERVICE);
        List<String> types = new ArrayList<>();
        for (String type : TYPE_NAMES.keySet()) {
            if (isAuthorized(enterpriseId, type)) {
                types.add(type);
            }
        }
        return AuthorizationCheckResult.builder()
                .allBasicAuthorized(basic)
                .authorizedTypes(types)
                .build();
    }

    private EnterpriseAuthorizationVO.ChangeNotification toChangeVo(AuthorizationChangeNotification n) {
        return EnterpriseAuthorizationVO.ChangeNotification.builder()
                .id(n.getId())
                .changeType(n.getChangeType())
                .changeSummary(n.getChangeSummary())
                .newAuthorizationTypes(n.getNewAuthorizationTypes())
                .responseStatus(n.getResponseStatus())
                .notifiedAt(n.getNotifiedAt())
                .build();
    }

    private void assertEnterpriseScope(Long enterpriseId) {
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        Long current = SecurityUtil.getCurrentEnterpriseId();
        SysUser user = currentUser();
        if (user != null && isPlatformAdmin(user)) {
            return;
        }
        if (!Objects.equals(current, enterpriseId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }

    private void assertCanManageEnterprise(Long enterpriseId) {
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        SysUser user = currentUser();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (isPlatformAdmin(user)) {
            return;
        }
        if (!Objects.equals(user.getEnterpriseId(), enterpriseId)) {
            throw new BizException(ErrorCode.DATA_AUTH_FORBIDDEN);
        }
        if (!isEnterpriseAdmin(user)) {
            throw new BizException(ErrorCode.DATA_AUTH_FORBIDDEN);
        }
    }

    private SysUser currentUser() {
        Long uid = SecurityUtil.getCurrentUserId();
        return uid == null ? null : sysUserMapper.selectById(uid);
    }

    private static boolean isPlatformAdmin(SysUser user) {
        return user.getRoleType() != null && user.getRoleType() == ROLE_PLATFORM_ADMIN;
    }

    private static boolean isEnterpriseAdmin(SysUser user) {
        Integer rt = user.getRoleType();
        return rt != null && (rt == ROLE_SELLER_ADMIN || rt == ROLE_BUYER_ADMIN || rt == ROLE_PLATFORM_ADMIN);
    }

    private boolean canRespondToNotification(SysUser user, AuthorizationChangeNotification n) {
        if (user == null) {
            return false;
        }
        if (isPlatformAdmin(user)) {
            return true;
        }
        if (n.getNotifiedUserId() != null && Objects.equals(n.getNotifiedUserId(), user.getId())) {
            return true;
        }
        return isEnterpriseAdmin(user) && Objects.equals(user.getEnterpriseId(), n.getEnterpriseId());
    }

    private SysUser findEnterpriseAdminUser(Long enterpriseId) {
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<>();
        q.eq(SysUser::getEnterpriseId, enterpriseId)
                .in(SysUser::getRoleType, ROLE_SELLER_ADMIN, ROLE_BUYER_ADMIN)
                .orderByAsc(SysUser::getId)
                .last("LIMIT 1");
        return sysUserMapper.selectOne(q);
    }

    private String buildSnapshotJson(Long enterpriseId, Map<String, Boolean> state) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("enterpriseId", enterpriseId);
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("authorizations", state);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String uploadSnapshot(Long enterpriseId, String suffix, String json) {
        String path = String.format("data-authorization/%d/%s_%d.json", enterpriseId, suffix, System.currentTimeMillis());
        return ossStorageService.upload(path, json);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static boolean intEq(Integer a, int b) {
        return a != null && a == b;
    }
}
