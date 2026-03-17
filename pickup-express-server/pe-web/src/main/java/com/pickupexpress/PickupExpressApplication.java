package com.pickupexpress;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.pickupexpress")
@MapperScan("com.pickupexpress.mapper")
public class PickupExpressApplication {

    public static void main(String[] args) {
        SpringApplication.run(PickupExpressApplication.class, args);
    }
}
