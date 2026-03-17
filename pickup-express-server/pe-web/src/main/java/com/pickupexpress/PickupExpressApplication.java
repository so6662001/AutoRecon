package com.pickupexpress;

import com.pickupexpress.common.config.PickupExpressProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.pickupexpress")
@MapperScan("com.pickupexpress.mapper")
@EnableConfigurationProperties(PickupExpressProperties.class)
public class PickupExpressApplication {

    public static void main(String[] args) {
        SpringApplication.run(PickupExpressApplication.class, args);
    }
}
