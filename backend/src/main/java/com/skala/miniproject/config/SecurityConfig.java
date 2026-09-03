package com.skala.miniproject.config;

import com.skala.miniproject.auth.jwt.JwtAuthenticationFilter;
import com.skala.miniproject.auth.jwt.JwtTokenProvider;
import com.skala.miniproject.auth.jwt.OriginCheckFilter;
import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.domain.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final JsonMapper jsonMapper;
    private final String allowedOrigin;

    public SecurityConfig(
            JwtTokenProvider tokenProvider,
            UserRepository userRepository,
            JsonMapper jsonMapper,
            @Value("${app.allowed-origin}") String allowedOrigin
    ) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.jsonMapper = jsonMapper;
        this.allowedOrigin = allowedOrigin;
    }

    @Bean
    @Order(100)
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(tokenProvider, userRepository);
        OriginCheckFilter originFilter = new OriginCheckFilter(allowedOrigin, jsonMapper);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/auth/signup",
                                "/auth/login",
                                "/auth/reissue",
                                "/auth/logout"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, cause) ->
                                writeError(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, cause) ->
                                writeError(response, ErrorCode.PRO_REQUIRED))
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(originFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(
                response.getWriter(),
                ApiResponse.fail(errorCode.name(), errorCode.getMessage())
        );
    }
}
