package com.wxw.cloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Gateway CORS configuration.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        config.addAllowedOrigin("http://manage.wxw.com");
        config.addAllowedOrigin("http://www.wxw.com");
        config.addAllowedOrigin("http://wxw.com");
        addLocalDevOrigins(config, 8080, 9001, 9002, 9003, 9004, 8081, 8085, 8087);

        config.setAllowCredentials(true);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(configSource);
    }

    private void addLocalDevOrigins(CorsConfiguration config, int... ports) {
        for (int port : ports) {
            config.addAllowedOrigin("http://localhost:" + port);
            config.addAllowedOrigin("http://127.0.0.1:" + port);
        }
    }
}
