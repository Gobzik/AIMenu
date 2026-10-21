package org.seed.aimenu.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun aimenuOpenApi(): OpenAPI = OpenAPI()
        .info(Info().title("AIMenu API").version("1.0.0").description("API для выбора блюда, учета продуктов и получения рекомендаций. Количества указываются в единицах каталога: g, ml или piece. Аллергии сопоставляются по точному ID продукта."))
        .components(io.swagger.v3.oas.models.Components().addSecuritySchemes("bearerAuth", SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
}
