package com.autorecon.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS configuration for development and production.
 * Set autorecon.cors-allowed-origins to actual frontend domain(s) in production.
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final AutoReconProperties autoReconProperties;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        String allowed = autoReconProperties.getCorsAllowedOrigins();
        if (allowed != null && !allowed.isEmpty()) {
            String trimmed = allowed.trim();
            if ("*".equals(trimmed)) {
                config.addAllowedOriginPattern("*");
            } else {
                for (String origin : allowed.split(",")) {
                    config.addAllowedOrigin(origin.trim());
                }
            }
        }
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
