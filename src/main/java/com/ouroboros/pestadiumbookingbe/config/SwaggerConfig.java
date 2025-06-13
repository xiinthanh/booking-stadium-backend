package com.ouroboros.pestadiumbookingbe.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("google-oauth", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("Google OAuth2 login")))
                .info(new Info()
                        .title("PE Stadium Booking API")
                        .version("1.0")
                        .description("API documentation for PE Stadium Booking application"));
    }
}

