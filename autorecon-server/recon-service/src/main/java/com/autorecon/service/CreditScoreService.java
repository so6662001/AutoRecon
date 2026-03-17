package com.autorecon.service;

import com.autorecon.domain.entity.CreditScore;
import com.autorecon.domain.vo.CreditScoreDetailVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 信用评分服务接口
 */
public interface CreditScoreService extends IService<CreditScore> {

    CreditScore getScore(Long buyerId, Long sellerId);

    List<CreditScore> getTrend(Long buyerId, Long sellerId);

    CreditScoreDetailVO getScoreDetail(Long buyerId, Long sellerId);

    void adjustScore(Long buyerId, Long sellerId, BigDecimal newScore, String reason);

    List<CreditScore> getRanking(Long sellerId, Integer limit);

    void recalculateScore(Long buyerId, Long sellerId);
}
