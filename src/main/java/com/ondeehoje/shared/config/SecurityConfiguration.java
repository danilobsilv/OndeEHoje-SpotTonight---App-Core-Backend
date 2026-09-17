package com.ondeehoje.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondeehoje.shared.error.ApiErrorFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment,
            ObjectMapper mapper, ApiErrorFactory errors) throws Exception {
        boolean local = environment.acceptsProfiles(Profiles.of("local"))
                && !environment.acceptsProfiles(Profiles.of("prod"));
        http.authorizeHttpRequests(authorize -> {
            authorize.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
            if (local) {
                authorize.requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html",
                        "/swagger-ui/**").permitAll();
            }
            authorize.anyRequest().denyAll();
        });
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.requestCache(cache -> cache.disable());
        http.exceptionHandling(handling -> handling
                .authenticationEntryPoint((request, response, ex) -> writeError(request, response,
                        mapper, errors, 401, "UNAUTHORIZED", "Authentication is required."))
                .accessDeniedHandler((request, response, ex) -> writeError(request, response,
                        mapper, errors, 403, "FORBIDDEN", "Access is denied.")));
        return http.build();
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, ObjectMapper mapper,
            ApiErrorFactory errors, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), errors.create(status, code, message, request.getRequestURI()));
    }
}
