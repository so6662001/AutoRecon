package com.autorecon.service;

import com.autorecon.domain.dto.PaymentAllocateDTO;
import com.autorecon.domain.dto.PaymentCreateDTO;
import com.autorecon.domain.entity.Payment;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 付款服务接口
 */
public interface PaymentService extends IService<Payment> {

    Long createPayment(PaymentCreateDTO dto);

    void allocatePayment(PaymentAllocateDTO dto);

    void autoAllocateFIFO(Long paymentId);

    void autoAllocateProportional(Long paymentId);

    BigDecimal getBalance(Long sellerId, Long buyerId);

    List<Payment> listPayments(Long payerId, Long payeeId, LocalDate start, LocalDate end);
}
