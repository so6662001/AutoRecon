package com.pickupexpress.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Pickup Express module configuration properties.
 */
@ConfigurationProperties(prefix = "pickup-express")
@Data
public class PickupExpressProperties {

    private boolean enabled = true;
    private String apiPrefix = "/api";
    private boolean demoMode = false;

    private Auth auth = new Auth();

    @Data
    public static class Auth {
        private boolean enabled = true;
        private String tokenHeader = "Authorization";
        private String tokenPrefix = "Bearer ";
        private List<String> excludePaths = List.of("/api/v1/guest/**", "/api/v1/users/login");
    }
}
