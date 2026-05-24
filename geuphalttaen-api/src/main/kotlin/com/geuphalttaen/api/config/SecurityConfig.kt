package com.geuphalttaen.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.geuphalttaen.common.response.ApiResponse
import com.geuphalttaen.domain.auth.JwtProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { ex ->
                // 미인증 요청 → 401 (기본값 403 방지)
                ex.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpStatus.UNAUTHORIZED.value()
                    response.contentType = "application/json;charset=UTF-8"
                    response.writer.write(
                        objectMapper.writeValueAsString(
                            ApiResponse.error<Nothing>("UNAUTHORIZED", "인증이 필요합니다")
                        )
                    )
                }
                // 권한 부족 요청 → 403
                ex.accessDeniedHandler { _, response, _ ->
                    response.status = HttpStatus.FORBIDDEN.value()
                    response.contentType = "application/json;charset=UTF-8"
                    response.writer.write(
                        objectMapper.writeValueAsString(
                            ApiResponse.error<Nothing>("FORBIDDEN", "접근 권한이 없습니다")
                        )
                    )
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.GET, "/api/v1/toilets").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/toilets/*/reviews").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/toilets/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/directions").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/geocode/**").permitAll()
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/api/v1/admin/auth/**").permitAll()
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .requestMatchers(
                        "/actuator/health",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}

class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)
        if (token != null && jwtProvider.isValid(token)) {
            val tokenType = jwtProvider.getTokenType(token)
            // REFRESH 토큰 등 비정상 타입으로 API 인증을 시도하는 경우를 차단한다
            if (tokenType == "ACCESS" || tokenType == "ADMIN_ACCESS") {
                val userId = jwtProvider.getUserId(token)
                val roles = jwtProvider.getRoles(token)
                val auth = JwtAuthentication(userId, roles)
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization") ?: return null
        return if (bearer.startsWith("Bearer ")) bearer.removePrefix("Bearer ") else null
    }
}
