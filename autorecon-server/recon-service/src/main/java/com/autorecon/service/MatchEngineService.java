package com.autorecon.service;

import com.autorecon.domain.entity.ReconBillItem;
import com.autorecon.domain.vo.MatchResultVO;

import java.util.List;

/**
 * 匹配引擎服务接口
 */
public interface MatchEngineService {

    /**
     * 执行匹配
     */
    MatchResultVO executeMatch(Long billId);

    /**
     * 获取匹配结果
     */
    MatchResultVO getMatchResult(Long billId);

    /**
     * 重新匹配
     */
    void rematch(Long billId);

    /**
     * 获取差异明细
     */
    List<ReconBillItem> getDiffItems(Long billId);
}
