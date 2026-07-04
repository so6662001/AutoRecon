package com.pickupexpress.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("demo")
class ContractControllerTest {

    @Autowired
    org.springframework.boot.test.web.client.TestRestTemplate restTemplate;

    private static org.springframework.http.HttpEntity<Void> authHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer demo_token_admin");
        return new org.springframework.http.HttpEntity<>(headers);
    }

    @Test
    void testQueryContracts_returns200() throws Exception {
        org.springframework.http.ResponseEntity<String> response = restTemplate.exchange("/api/v1/evidence/contracts/?pageNum=1&pageSize=10",
                org.springframework.http.HttpMethod.GET, authHeaders(), String.class);
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        org.assertj.core.api.Assertions.assertThat(response.getBody()).contains("\"code\":200");
    }

    @Test
    void testGetDashboard_returns200() throws Exception {
        org.springframework.http.ResponseEntity<String> response = restTemplate.exchange("/api/v1/evidence/dashboard/",
                org.springframework.http.HttpMethod.GET, authHeaders(), String.class);
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        org.assertj.core.api.Assertions.assertThat(response.getBody()).contains("\"code\":200");
    }

    @Test
    void testListWarehouses_returns200() throws Exception {
        org.springframework.http.ResponseEntity<String> response = restTemplate.exchange("/api/v1/evidence/warehouse/",
                org.springframework.http.HttpMethod.GET, authHeaders(), String.class);
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        org.assertj.core.api.Assertions.assertThat(response.getBody()).contains("\"code\":200");
    }

    @Test
    void testListCarriers_returns200() throws Exception {
        org.springframework.http.ResponseEntity<String> response = restTemplate.exchange("/api/v1/evidence/carrier/",
                org.springframework.http.HttpMethod.GET, authHeaders(), String.class);
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        org.assertj.core.api.Assertions.assertThat(response.getBody()).contains("\"code\":200");
    }

    @Test
    void testVerifyPickupCode_invalidCode() throws Exception {
        org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/evidence/delivery/verify-code?pickupCode=INVALID&vehiclePlate=沪A12345",
                org.springframework.http.HttpMethod.POST, authHeaders(), String.class);
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testListTemplates_returns200() throws Exception {
        org.springframework.http.ResponseEntity<String> response = restTemplate.exchange("/api/v1/evidence/templates/",
                org.springframework.http.HttpMethod.GET, authHeaders(), String.class);
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        org.assertj.core.api.Assertions.assertThat(response.getBody()).contains("\"code\":200");
    }
}
