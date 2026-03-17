package com.pickupexpress.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickupexpress.domain.entity.TradingHabitRecord;
import com.pickupexpress.mapper.TradingHabitRecordMapper;
import com.pickupexpress.service.TradingHabitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易习惯服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class TradingHabitServiceImpl extends ServiceImpl<TradingHabitRecordMapper, TradingHabitRecord>
        implements TradingHabitService {

    @Override
    public void recordPickup(Long buyerId, String driverName, String driverPhone, String vehiclePlate,
                             BigDecimal weight, BigDecimal amount) {
        TradingHabitRecord habit = getHabit(buyerId, driverPhone);
        LocalDateTime now = LocalDateTime.now();

        if (habit == null) {
            habit = TradingHabitRecord.builder()
                    .buyerId(buyerId)
                    .driverName(driverName)
                    .driverPhone(driverPhone)
                    .vehiclePlate(vehiclePlate)
                    .totalPickups(1)
                    .totalWeight(weight != null ? weight : BigDecimal.ZERO)
                    .totalAmount(amount != null ? amount : BigDecimal.ZERO)
                    .paidPickups(0)
                    .deniedPickups(0)
                    .lastPickupAt(now)
                    .firstPickupAt(now)
                    .build();
        } else {
            habit.setTotalPickups((habit.getTotalPickups() != null ? habit.getTotalPickups() : 0) + 1);
            habit.setTotalWeight(habit.getTotalWeight() != null ? habit.getTotalWeight().add(weight != null ? weight : BigDecimal.ZERO) : (weight != null ? weight : BigDecimal.ZERO));
            habit.setTotalAmount(habit.getTotalAmount() != null ? habit.getTotalAmount().add(amount != null ? amount : BigDecimal.ZERO) : (amount != null ? amount : BigDecimal.ZERO));
            habit.setLastPickupAt(now);
        }

        saveOrUpdate(habit);
    }

    @Override
    public TradingHabitRecord getHabit(Long buyerId, String driverPhone) {
        return getOne(new LambdaQueryWrapper<TradingHabitRecord>()
                .eq(TradingHabitRecord::getBuyerId, buyerId)
                .eq(TradingHabitRecord::getDriverPhone, driverPhone)
                .last("LIMIT 1"));
    }

    @Override
    public List<TradingHabitRecord> listByBuyer(Long buyerId) {
        return list(new LambdaQueryWrapper<TradingHabitRecord>()
                .eq(TradingHabitRecord::getBuyerId, buyerId)
                .orderByDesc(TradingHabitRecord::getLastPickupAt));
    }

    @Override
    public String generateHabitReport(Long buyerId) {
        List<TradingHabitRecord> habits = listByBuyer(buyerId);
        StringBuilder sb = new StringBuilder();
        sb.append("交易习惯报告 - 买方ID: ").append(buyerId).append("\n");
        sb.append("记录数: ").append(habits.size()).append("\n");
        for (TradingHabitRecord h : habits) {
            sb.append("- ").append(h.getDriverName()).append(" ").append(h.getDriverPhone())
                    .append(" 提货").append(h.getTotalPickups()).append("次, 总重量:")
                    .append(h.getTotalWeight()).append("吨, 总金额:¥").append(h.getTotalAmount()).append("\n");
        }
        return sb.toString();
    }
}
