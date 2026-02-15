package com.space.space_bundle.out.automation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AutomationConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}