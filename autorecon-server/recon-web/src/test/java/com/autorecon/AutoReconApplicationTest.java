package com.autorecon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.sql.init.mode=always",
    "spring.rabbitmq.listener.simple.auto-startup=false"
})
class AutoReconApplicationTest {

    @Test
    void contextLoads() {
        // Just verify Spring context loads successfully
    }
}
