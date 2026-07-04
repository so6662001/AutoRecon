package com.pickupexpress;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.type=com.zaxxer.hikari.HikariDataSource",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:db/schema-h2.sql",
    "spring.sql.init.data-locations=classpath:db/data-demo.sql",
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "pickup-express.demo-mode=true",
    "pickup-express.auth.enabled=false"
})
class PickupExpressApplicationTest {
    @Test
    void contextLoads() {}
}
