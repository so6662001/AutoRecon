package com.pickupexpress;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:authflow_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
    "pickup-express.demo-mode=false",
    "pickup-express.auth.enabled=true"
})
@org.springframework.test.context.ActiveProfiles("demo")
@AutoConfigureMockMvc
class PickupExpressAuthFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void testProtectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/contracts"))
                .andExpect(status().isUnauthorized());
    }
}
