package com.pickupexpress.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ConditionalOnProperty(prefix = "pickup-express", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.pickupexpress")
@MapperScan("com.pickupexpress.mapper")
@EnableConfigurationProperties(PickupExpressProperties.class)
public class PickupExpressAutoConfiguration {
}
