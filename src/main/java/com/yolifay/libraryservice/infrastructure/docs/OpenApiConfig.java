package com.yolifay.libraryservice.infrastructure.docs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Library Service API")
                        .description("ITSEC Backend Technical Test")
                        .version("v1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
    }

    /** Set semua endpoint butuh bearer auth, kecuali daftar whitelist. */
    @Bean
    public OpenApiCustomizer globalAuthCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, item) -> {
            boolean whitelist =
                    path.startsWith("/api/v1/auth/login") ||
                            path.startsWith("/api/v1/auth/register") ||
                            path.startsWith("/api/v1/auth/refresh") ||
                            path.startsWith("/api/v1/auth/request-otp") ||
                            path.startsWith("/api/v1/auth/login-otp") ||
                            path.startsWith("/v3/api-docs") ||
                            path.startsWith("/swagger-ui") ||
                            path.startsWith("/swagger") ||
                            path.startsWith("/actuator/health");
            if (!whitelist) {
                item.readOperations().forEach(op ->
                        op.addSecurityItem(new SecurityRequirement().addList("bearerAuth")));
            }
        });
    }
}
