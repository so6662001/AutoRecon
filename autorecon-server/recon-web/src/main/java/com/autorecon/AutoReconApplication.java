package com.autorecon;

import com.autorecon.common.config.AutoReconProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AutoRecon - Steel Industry Reconciliation Platform
 * Application entry point.
 */
@SpringBootApplication(scanBasePackages = "com.autorecon")
@EnableScheduling
@EnableConfigurationProperties(AutoReconProperties.class)
public class AutoReconApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoReconApplication.class, args);
    }
}
