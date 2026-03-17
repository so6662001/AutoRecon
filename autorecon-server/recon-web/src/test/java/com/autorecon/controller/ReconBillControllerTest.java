package com.autorecon.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("demo")
class ReconBillControllerTest {

    @Autowired
    TestRestTemplate restTemplate;

    private static org.springframework.http.HttpEntity<Void> authHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer demo_token_admin");
        return new org.springframework.http.HttpEntity<>(headers);
    }

    @Test
    void testQueryBills_returns200() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/recon/bills/?pageNum=1&pageSize=10",
                org.springframework.http.HttpMethod.GET, authHeaders(), String.class);
        assertEquals(200, response.getStatusCode().value());
        org.assertj.core.api.Assertions.assertThat(response.getBody()).contains("\"code\":200");
    }

    @Test
    void testGetDashboard_returns200() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/recon/dashboard/",
                org.springframework.http.HttpMethod.GET, authHeaders(), String.class);
        assertEquals(200, response.getStatusCode().value());
        org.assertj.core.api.Assertions.assertThat(response.getBody()).contains("\"code\":200");
    }

    @Test
    void testListTemplates_returns200() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/recon/templates/",
                org.springframework.http.HttpMethod.GET, authHeaders(), String.class);
        assertEquals(200, response.getStatusCode().value());
        org.assertj.core.api.Assertions.assertThat(response.getBody()).contains("\"code\":200");
    }

    @Test
    void testGuestViewWithInvalidToken_returns200WithError() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/guest/view/invalid-token", String.class);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testCreateBill_withValidData() throws Exception {
        String json = """
            {
                "buyerId": 3,
                "templateId": 1,
                "periodStart": "2026-01-01",
                "periodEnd": "2026-01-31",
                "matchMode": 1,
                "autoSend": false,
                "includePayment": true,
                "paymentAllocStrategy": 1,
                "items": [{
                    "productName": "螺纹钢",
                    "spec": "Φ20",
                    "material": "HRB400",
                    "quantity": 100,
                    "weight": 98.5,
                    "unitPrice": 4200,
                    "amount": 413700
                }]
            }
            """;
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer demo_token_admin");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/recon/bills/",
                new org.springframework.http.HttpEntity<>(json, headers), String.class);
        assertEquals(200, response.getStatusCode().value());
        org.assertj.core.api.Assertions.assertThat(response.getBody()).contains("\"code\":200");
    }

    @Test
    void testCreateBill_withoutRequiredFields_returns400() throws Exception {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer demo_token_admin");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/recon/bills/",
                new org.springframework.http.HttpEntity<>("{}", headers), String.class);
        assertEquals(200, response.getStatusCode().value());
        org.assertj.core.api.Assertions.assertThat(response.getBody()).contains("\"code\":400");
    }
}
