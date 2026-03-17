package com.autorecon.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * AutoRecon module configuration properties.
 */
@ConfigurationProperties(prefix = "autorecon")
@Data
public class AutoReconProperties {

    private String apiPrefix = "/api";
    private boolean demoMode = false;

    private Module module = new Module();

    @Data
    public static class Module {
        private boolean reconEnabled = true;
        private boolean signEnabled = true;
        private boolean collectionEnabled = true;
        private boolean financeEnabled = true;
        private boolean billingEnabled = true;
        private boolean engagementEnabled = true;
    }

    private Auth auth = new Auth();

    @Data
    public static class Auth {
        private boolean enabled = true;
        private String tokenHeader = "Authorization";
        private String tokenPrefix = "Bearer ";
        private List<String> excludePaths = List.of("/api/v1/guest/**", "/api/v1/users/login");
    }
}
