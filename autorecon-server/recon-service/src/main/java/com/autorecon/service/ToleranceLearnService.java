package com.autorecon.service;

import com.autorecon.domain.entity.ToleranceLearn;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 容差学习服务接口
 */
public interface ToleranceLearnService extends IService<ToleranceLearn> {

    List<ToleranceLearn> getSuggestions(Long sellerId, Long buyerId);

    void adoptSuggestion(Long id);

    void rejectSuggestion(Long id);

    List<ToleranceLearn> listAll(Long sellerId);
}
