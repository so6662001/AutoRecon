package com.autorecon.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据服务使用的授权检查结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationCheckResult {

    private Boolean allBasicAuthorized;

    @Builder.Default
    private List<String> authorizedTypes = new ArrayList<>();
}
