package com.autorecon.common.config;

import com.autorecon.common.util.TenantUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AutoRecon module configuration.
 */
@Configuration
@EnableConfigurationProperties(AutoReconProperties.class)
public class AutoReconConfig {

    @Bean
    public TenantUtilConfigurer tenantUtilConfigurer(AutoReconProperties properties) {
        TenantUtil.setAutoReconProperties(properties);
        return new TenantUtilConfigurer();
    }

    static class TenantUtilConfigurer {}
}
