package com.autorecon;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.type=com.zaxxer.hikari.HikariDataSource",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:authflow_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:db/schema-h2.sql",
    "spring.sql.init.data-locations=classpath:db/data-demo.sql",
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "spring.data.redis.repositories.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "autorecon.demo-mode=false",
    "autorecon.auth.enabled=true"
})
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void testProtectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/recon/bills"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGuestEndpoint_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/guest/view/test-token"))
                .andExpect(status().isOk());
    }

    @Test
    void testLoginEndpoint_withoutToken_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk());
    }
}
