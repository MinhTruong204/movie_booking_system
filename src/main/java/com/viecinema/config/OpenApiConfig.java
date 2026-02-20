package com.viecinema.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";
    @Value("${app.base-url}")
    private String baseUrl;

    @Bean
    public OpenAPI vieCinemaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VieCinema API")
                        .description("""
                                REST API documentation for **VieCinema** – a cinema booking system.
                                
                                ## Authentication
                                Most endpoints require a **JWT Bearer token**.  
                                1. Call `POST /api/auth/login` to obtain a token.  
                                2. Click **Authorize** at the top-right of this page.  
                                3. Enter: `Bearer <your_token>` and click **Authorize**.
                                """)
                        .contact(new Contact()
                                .name("VieCinema Developer (Trương Ngọc Minh)")
                                .email("truongocminh204@gmail.com"))
                        .license(new License()))
                .servers(List.of(
                        new Server().url(baseUrl).description("Local Development Server")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token. Example: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`")));
    }
}

