package com.minidoodle.infrastructure.scheduling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** No manual OpenAPI YAML — the schema is generated from the controllers/DTOs. */
@Configuration
class SchedulingOpenApiConfig {

    @Bean
    OpenAPI schedulingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Mini-Doodle Scheduling API")
                .description("Booking-page / 1:1 scheduling engine: slots, bookings, and free/busy availability.")
                .version("v1"));
    }
}
