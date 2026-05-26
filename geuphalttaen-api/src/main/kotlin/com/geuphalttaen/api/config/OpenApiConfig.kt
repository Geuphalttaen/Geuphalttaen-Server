package com.geuphalttaen.api.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger UI에 Bearer Token 인증 버튼을 표시하기 위한 OpenAPI 설정.
 * @SecurityRequirement(name = "bearerAuth")가 붙은 엔드포인트에 자물쇠 아이콘이 나타난다.
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("급할땐 API")
                .version("v1")
                .description("급할땐 백엔드 REST API 문서"),
        )
        .components(
            Components().addSecuritySchemes(
                "bearerAuth",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("관리자/사용자 JWT Access Token"),
            ),
        )
}
