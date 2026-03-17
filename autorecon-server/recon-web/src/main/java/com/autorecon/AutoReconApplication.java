package com.autorecon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AutoRecon - Steel Industry Reconciliation Platform
 * Application entry point.
 */
@SpringBootApplication(scanBasePackages = "com.autorecon")
@MapperScan("com.autorecon.mapper")
public class AutoReconApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoReconApplication.class, args);
    }
}
