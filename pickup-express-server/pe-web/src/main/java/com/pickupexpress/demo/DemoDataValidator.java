package com.pickupexpress.demo;

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
        log.info("============================================");
        log.info("   提货通 Demo 模式已启动");
        log.info("============================================");
        log.info("API 文档: http://localhost:8081/doc.html");
        log.info("H2 控制台: http://localhost:8081/h2-console");
        log.info("");
        log.info("Demo 账号:");
        log.info("  admin   / admin123  (平台管理员)");
        log.info("  seller1 / 123456    (卖方管理员-张三)");
        log.info("  buyer1  / 123456    (买方管理员-李四)");
        log.info("  buyer2  / 123456    (买方管理员-王五)");
        log.info("============================================");
    }
}
