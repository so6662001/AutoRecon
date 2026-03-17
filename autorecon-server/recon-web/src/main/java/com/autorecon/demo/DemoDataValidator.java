package com.autorecon.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Logs demo mode info on startup.
 */
@Slf4j
@Component
@Profile("demo")
public class DemoDataValidator implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("  AutoRecon DEMO MODE - Steel Industry  ");
        log.info("========================================");
        log.info("");
        log.info("Demo accounts (password):");
        log.info("  - admin / admin123   (platform admin)");
        log.info("  - seller1 / 123456   (seller - 华东钢铁)");
        log.info("  - seller2 / 123456   (seller - 华北钢材)");
        log.info("  - buyer1 / 123456    (buyer - 江苏建筑)");
        log.info("  - buyer2 / 123456    (buyer - 上海加工)");
        log.info("  - buyer3 / 123456    (buyer - 杭州金属)");
        log.info("");
        log.info("API documentation: http://localhost:8080/doc.html");
        log.info("H2 console:        http://localhost:8080/h2-console");
        log.info("  JDBC URL: jdbc:h2:mem:autorecon_demo");
        log.info("  User: sa, Password: (empty)");
        log.info("");
        log.info("Key API endpoints:");
        log.info("  POST /api/v1/users/login     - Login");
        log.info("  GET  /api/v1/recon/bills    - List bills");
        log.info("  GET  /api/v1/recon/dashboard - Dashboard stats");
        log.info("  GET  /api/v1/enterprises    - List enterprises");
        log.info("");
        log.info("========================================");
    }
}
