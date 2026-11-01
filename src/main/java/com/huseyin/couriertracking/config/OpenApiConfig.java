package com.huseyin.couriertracking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI courierTrackingApi() {
        return new OpenAPI().info(new Info()
                .title("Courier Tracking API")
                .version("v1")
                .description("Receives courier locations, logs store entrances and reports total travel distance. All distances are in meters."));
    }
}