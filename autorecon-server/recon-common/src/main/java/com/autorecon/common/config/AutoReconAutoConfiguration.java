package com.autorecon.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ConditionalOnProperty(prefix = "autorecon", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.autorecon")
@MapperScan("com.autorecon.mapper")
@EnableConfigurationProperties(AutoReconProperties.class)
public class AutoReconAutoConfiguration {
    // Empty - just enables component scanning and config
}
