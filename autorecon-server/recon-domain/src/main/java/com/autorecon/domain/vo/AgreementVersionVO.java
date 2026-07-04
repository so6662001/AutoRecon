package com.autorecon.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 协议版本展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementVersionVO {

    private Long id;
    private Integer agreementType;
    private String versionNo;
    private String title;
    private String content;
    private String summary;
    private LocalDate effectiveDate;
    private LocalDateTime publishedAt;
    private String contentUrl;
}
