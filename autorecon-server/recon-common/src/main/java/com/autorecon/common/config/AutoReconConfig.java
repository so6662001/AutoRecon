package com.autorecon.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AutoRecon module configuration.
 */
@Configuration
@EnableConfigurationProperties(AutoReconProperties.class)
public class AutoReconConfig {
}
