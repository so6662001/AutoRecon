package com.autorecon.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据安全与主权状态 VO
 */
@Data
public class SecurityStatusVO {

    private Boolean dataIsolation = true;
    private String encryptionLevel = "AES-256";
    private String transferEncryption = "TLS 1.3";
    private LocalDateTime lastBackupAt;
    private String backupFrequency = "每日";
    private Integer totalAccessCount;
}
