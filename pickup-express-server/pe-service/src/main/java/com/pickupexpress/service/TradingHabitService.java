package com.pickupexpress.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pickupexpress.domain.entity.TradingHabitRecord;

import java.math.BigDecimal;
import java.util.List;

/**
 * 交易习惯服务
 */
public interface TradingHabitService extends IService<TradingHabitRecord> {

    void recordPickup(Long buyerId, String driverName, String driverPhone, String vehiclePlate, BigDecimal weight, BigDecimal amount);

    TradingHabitRecord getHabit(Long buyerId, String driverPhone);

    List<TradingHabitRecord> listByBuyer(Long buyerId);

    String generateHabitReport(Long buyerId);
}
