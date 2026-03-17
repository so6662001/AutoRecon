package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.result.PageResult;
import com.autorecon.domain.entity.BuyerEngagement;
import com.autorecon.domain.vo.BuyerEngagementVO;
import com.autorecon.domain.vo.EngagementFunnelVO;
import com.autorecon.mapper.BuyerEngagementMapper;
import com.autorecon.service.EngagementService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 买方参与度服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class EngagementServiceImpl extends ServiceImpl<BuyerEngagementMapper, BuyerEngagement> implements EngagementService {

    private final BuyerEngagementMapper buyerEngagementMapper;

    @Override
    public PageResult<BuyerEngagementVO> listBuyers(Long sellerEnterpriseId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<BuyerEngagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuyerEngagement::getSellerEnterpriseId, sellerEnterpriseId)
                .orderByDesc(BuyerEngagement::getLastActiveAt);
        Page<BuyerEngagement> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        IPage<BuyerEngagement> result = buyerEngagementMapper.selectPage(page, wrapper);

        List<BuyerEngagementVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }

    @Override
    public BuyerEngagementVO getBuyerDetail(Long id) {
        BuyerEngagement engagement = buyerEngagementMapper.selectById(id);
        if (engagement == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "买方参与记录不存在");
        }
        return toVO(engagement);
    }

    @Override
    public EngagementFunnelVO getFunnel(Long sellerEnterpriseId) {
        LambdaQueryWrapper<BuyerEngagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuyerEngagement::getSellerEnterpriseId, sellerEnterpriseId);
        List<BuyerEngagement> all = buyerEngagementMapper.selectList(wrapper);

        EngagementFunnelVO vo = new EngagementFunnelVO();
        vo.setTotalSent((int) all.stream().filter(e -> e.getFirstLinkSentAt() != null).count());
        vo.setTotalOpened((int) all.stream().filter(e -> e.getFirstLinkOpenedAt() != null).count());
        vo.setTotalConfirmed((int) all.stream().filter(e -> e.getEngagementLevel() != null && e.getEngagementLevel() >= 2).count());
        vo.setTotalRegistered((int) all.stream().filter(e -> e.getRegisteredAt() != null).count());
        vo.setTotalDataSubmit((int) all.stream().filter(e -> e.getFirstDataSubmitAt() != null).count());
        vo.setTotalSealInit((int) all.stream().filter(e -> e.getSealInitializedAt() != null).count());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendInvite(Long engagementId) {
        BuyerEngagement engagement = buyerEngagementMapper.selectById(engagementId);
        if (engagement == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "买方参与记录不存在");
        }
        engagement.setTotalBillsSent(engagement.getTotalBillsSent() != null ? engagement.getTotalBillsSent() + 1 : 1);
        engagement.setLastActiveAt(LocalDateTime.now());
        if (engagement.getFirstLinkSentAt() == null) {
            engagement.setFirstLinkSentAt(LocalDateTime.now());
        }
        buyerEngagementMapper.updateById(engagement);
        log.info("Sent invite: engagementId={}", engagementId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchInvite(List<Long> engagementIds) {
        if (engagementIds != null) {
            for (Long id : engagementIds) {
                sendInvite(id);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEngagementLevel(Long buyerEnterpriseId, Long sellerEnterpriseId, Integer newLevel) {
        LambdaQueryWrapper<BuyerEngagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuyerEngagement::getBuyerEnterpriseId, buyerEnterpriseId)
                .eq(BuyerEngagement::getSellerEnterpriseId, sellerEnterpriseId);
        BuyerEngagement engagement = buyerEngagementMapper.selectOne(wrapper);
        if (engagement == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "买方参与记录不存在");
        }
        engagement.setEngagementLevel(newLevel);
        if (newLevel != null) {
            LocalDateTime now = LocalDateTime.now();
            switch (newLevel) {
                case 2 -> engagement.setFirstLinkOpenedAt(engagement.getFirstLinkOpenedAt() != null ? engagement.getFirstLinkOpenedAt() : now);
                case 3 -> engagement.setRegisteredAt(engagement.getRegisteredAt() != null ? engagement.getRegisteredAt() : now);
                case 4 -> engagement.setFirstDataSubmitAt(engagement.getFirstDataSubmitAt() != null ? engagement.getFirstDataSubmitAt() : now);
                case 5 -> engagement.setSealInitializedAt(engagement.getSealInitializedAt() != null ? engagement.getSealInitializedAt() : now);
                case 6 -> engagement.setErpConnectedAt(engagement.getErpConnectedAt() != null ? engagement.getErpConnectedAt() : now);
                default -> {}
            }
        }
        buyerEngagementMapper.updateById(engagement);
    }

    private BuyerEngagementVO toVO(BuyerEngagement e) {
        BuyerEngagementVO vo = new BuyerEngagementVO();
        BeanUtils.copyProperties(e, vo);
        vo.setSuggestedAction(suggestAction(e.getEngagementLevel()));
        return vo;
    }

    private String suggestAction(Integer level) {
        if (level == null) return "发送邀请";
        return switch (level) {
            case 0, 1 -> "发送邀请";
            case 2 -> "引导注册";
            case 3 -> "引导提交数据";
            case 4 -> "引导初始化签章";
            case 5 -> "引导连接ERP";
            default -> "维护关系";
        };
    }
}
