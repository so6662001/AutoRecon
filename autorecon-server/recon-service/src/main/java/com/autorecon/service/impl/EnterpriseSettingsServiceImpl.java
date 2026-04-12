package com.autorecon.service.impl;

import com.autorecon.common.exception.BizException;
import com.autorecon.common.exception.ErrorCode;
import com.autorecon.common.util.SecurityUtil;
import com.autorecon.domain.dto.NotificationTemplateDTO;
import com.autorecon.domain.dto.ReconRulesDTO;
import com.autorecon.domain.dto.TimeoutConfigDTO;
import com.autorecon.domain.entity.EnterpriseSettings;
import com.autorecon.mapper.EnterpriseSettingsMapper;
import com.autorecon.service.EnterpriseSettingsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class EnterpriseSettingsServiceImpl extends ServiceImpl<EnterpriseSettingsMapper, EnterpriseSettings>
        implements EnterpriseSettingsService {

    private final ObjectMapper objectMapper;

    private EnterpriseSettings loadOrCreate(Long enterpriseId) {
        if (enterpriseId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        LambdaQueryWrapper<EnterpriseSettings> w = new LambdaQueryWrapper<>();
        w.eq(EnterpriseSettings::getEnterpriseId, enterpriseId).last("LIMIT 1");
        EnterpriseSettings row = getBaseMapper().selectOne(w);
        if (row == null) {
            row = EnterpriseSettings.builder()
                    .enterpriseId(enterpriseId)
                    .noDiffDays(3)
                    .specChangeHours(24)
                    .overDiffHours(48)
                    .settleDays(3)
                    .reminderHours(6)
                    .timeoutCustomersJson("[]")
                    .notificationTemplatesJson(defaultNotificationTemplatesJson())
                    .reconRulesJson(defaultReconRulesJson())
                    .build();
            getBaseMapper().insert(row);
        } else {
            boolean needPatch = false;
            if (row.getTimeoutCustomersJson() == null || row.getTimeoutCustomersJson().isBlank()) {
                row.setTimeoutCustomersJson("[]");
                needPatch = true;
            }
            if (row.getNotificationTemplatesJson() == null || row.getNotificationTemplatesJson().isBlank()) {
                row.setNotificationTemplatesJson(defaultNotificationTemplatesJson());
                needPatch = true;
            }
            if (row.getReconRulesJson() == null || row.getReconRulesJson().isBlank()) {
                row.setReconRulesJson(defaultReconRulesJson());
                needPatch = true;
            }
            if (needPatch) {
                getBaseMapper().updateById(row);
            }
        }
        return row;
    }

    private String defaultNotificationTemplatesJson() {
        try {
            List<NotificationTemplateDTO.Item> list = new ArrayList<>();
            list.add(newItem("1", "对账单已发送", "SMS,EMAIL", "【对账】您有新的对账单待确认，单号：{billNo}", "您好，对账单 {billNo} 已发送，请登录查看。", "对账单待确认"));
            list.add(newItem("2", "异议待处理", "EMAIL,PUSH", "", "您好，对账单 {billNo} 存在异议，请及时处理。", "异议提醒"));
            list.add(newItem("3", "签章完成", "SMS", "【对账】对账单 {billNo} 已完成签章。", "", ""));
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private NotificationTemplateDTO.Item newItem(String id, String event, String ch, String sms, String email, String push) {
        NotificationTemplateDTO.Item i = new NotificationTemplateDTO.Item();
        i.setId(id);
        i.setEventType(event);
        i.setChannels(ch);
        i.setSmsTemplate(sms);
        i.setEmailTemplate(email);
        i.setPushTemplate(push);
        i.setStatus("ENABLED");
        return i;
    }

    private String defaultReconRulesJson() {
        try {
            ReconRulesDTO dto = new ReconRulesDTO();
            dto.setWeightTolerancePercent(new BigDecimal("0.50"));
            dto.setAmountToleranceAbs(new BigDecimal("100.00"));
            dto.setDateToleranceDays(1);
            dto.setAutoConfirmWithinTolerance(false);
            dto.setDefaultDeductionStrategy("按较小金额扣减");
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public TimeoutConfigDTO getTimeoutConfig(Long enterpriseId) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        EnterpriseSettings row = loadOrCreate(eid);
        TimeoutConfigDTO dto = new TimeoutConfigDTO();
        TimeoutConfigDTO.GlobalTimeout g = new TimeoutConfigDTO.GlobalTimeout();
        g.setNoDiffDays(row.getNoDiffDays() != null ? row.getNoDiffDays() : 3);
        g.setSpecChangeHours(row.getSpecChangeHours() != null ? row.getSpecChangeHours() : 24);
        g.setOverDiffHours(row.getOverDiffHours() != null ? row.getOverDiffHours() : 48);
        g.setSettleDays(row.getSettleDays() != null ? row.getSettleDays() : 3);
        g.setReminderHours(row.getReminderHours() != null ? row.getReminderHours() : 6);
        dto.setGlobal(g);
        try {
            List<TimeoutConfigDTO.CustomerTimeout> customers = objectMapper.readValue(
                    row.getTimeoutCustomersJson(),
                    new TypeReference<List<TimeoutConfigDTO.CustomerTimeout>>() {});
            dto.setCustomers(customers != null ? customers : new ArrayList<>());
        } catch (Exception e) {
            dto.setCustomers(new ArrayList<>());
        }
        return dto;
    }

    @Override
    public void saveTimeoutConfig(Long enterpriseId, TimeoutConfigDTO dto) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        EnterpriseSettings row = loadOrCreate(eid);
        TimeoutConfigDTO.GlobalTimeout g = dto.getGlobal();
        if (g != null) {
            row.setNoDiffDays(g.getNoDiffDays());
            row.setSpecChangeHours(g.getSpecChangeHours());
            row.setOverDiffHours(g.getOverDiffHours());
            row.setSettleDays(g.getSettleDays());
            row.setReminderHours(g.getReminderHours());
        }
        List<TimeoutConfigDTO.CustomerTimeout> customers = dto.getCustomers();
        if (customers != null) {
            for (TimeoutConfigDTO.CustomerTimeout c : customers) {
                if (c.getId() == null || c.getId().isBlank()) {
                    c.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
                }
            }
            try {
                row.setTimeoutCustomersJson(objectMapper.writeValueAsString(customers));
            } catch (Exception e) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "客户配置序列化失败");
            }
        }
        getBaseMapper().updateById(row);
        log.info("Saved timeout config for enterprise {}", eid);
    }

    @Override
    public NotificationTemplateDTO getNotificationTemplates(Long enterpriseId) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        EnterpriseSettings row = loadOrCreate(eid);
        NotificationTemplateDTO dto = new NotificationTemplateDTO();
        try {
            List<NotificationTemplateDTO.Item> list = objectMapper.readValue(
                    row.getNotificationTemplatesJson(),
                    new TypeReference<List<NotificationTemplateDTO.Item>>() {});
            dto.setTemplates(list != null ? list : new ArrayList<>());
        } catch (Exception e) {
            dto.setTemplates(new ArrayList<>());
        }
        return dto;
    }

    @Override
    public void saveNotificationTemplates(Long enterpriseId, NotificationTemplateDTO dto) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        EnterpriseSettings row = loadOrCreate(eid);
        List<NotificationTemplateDTO.Item> list = dto.getTemplates();
        if (list != null) {
            for (NotificationTemplateDTO.Item i : list) {
                if (i.getId() == null || i.getId().isBlank()) {
                    i.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
                }
            }
            try {
                row.setNotificationTemplatesJson(objectMapper.writeValueAsString(list));
            } catch (Exception e) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "通知模板序列化失败");
            }
        }
        getBaseMapper().updateById(row);
        log.info("Saved notification templates for enterprise {}", eid);
    }

    @Override
    public ReconRulesDTO getReconRules(Long enterpriseId) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        EnterpriseSettings row = loadOrCreate(eid);
        try {
            return objectMapper.readValue(row.getReconRulesJson(), ReconRulesDTO.class);
        } catch (Exception e) {
            ReconRulesDTO dto = new ReconRulesDTO();
            dto.setWeightTolerancePercent(new BigDecimal("0.50"));
            dto.setAmountToleranceAbs(new BigDecimal("100.00"));
            dto.setDateToleranceDays(1);
            dto.setAutoConfirmWithinTolerance(false);
            dto.setDefaultDeductionStrategy("按较小金额扣减");
            return dto;
        }
    }

    @Override
    public void saveReconRules(Long enterpriseId, ReconRulesDTO dto) {
        Long eid = enterpriseId != null ? enterpriseId : SecurityUtil.getCurrentEnterpriseId();
        EnterpriseSettings row = loadOrCreate(eid);
        try {
            row.setReconRulesJson(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "对账规则序列化失败");
        }
        getBaseMapper().updateById(row);
        log.info("Saved recon rules for enterprise {}", eid);
    }
}
