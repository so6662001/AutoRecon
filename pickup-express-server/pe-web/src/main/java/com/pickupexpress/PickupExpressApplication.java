package com.pickupexpress;

import com.pickupexpress.common.config.PickupExpressProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Pickup Express (提货通) - Evidence Chain System
 * Application entry point.
 */
@SpringBootApplication(scanBasePackages = "com.pickupexpress")
@EnableConfigurationProperties(PickupExpressProperties.class)
public class PickupExpressApplication {

    public static void main(String[] args) {
        SpringApplication.run(PickupExpressApplication.class, args);
    }
}
