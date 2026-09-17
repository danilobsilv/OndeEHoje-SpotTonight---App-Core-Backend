package com.ondeehoje.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondeehoje.auth.security.AuthProperties;
import com.ondeehoje.shared.error.ApiErrorFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import java.util.function.Supplier;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfiguration {
    @Bean
    @Order(1)
    SecurityFilterChain authFilterChain(HttpSecurity http, ObjectMapper mapper, ApiErrorFactory errors,
            AuthProperties properties) throws Exception {
        // Separate chain: resource-server defaults exempt Bearer requests from CSRF.
        // Cookie-authenticated refresh/logout must never inherit that exemption.
        http.securityMatcher("/api/v1/auth/**");
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
                        "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                .anyRequest().denyAll());
        configureStateless(http, mapper, errors);
        var csrfCookies = new CookieCsrfTokenRepository();
        csrfCookies.setCookieName(properties.cookieSecure() ? "__Host-csrf" : "csrf");
        csrfCookies.setCookiePath("/");
        csrfCookies.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(properties.cookieSecure())
                .sameSite("Strict"));
        var xorHandler = new XorCsrfTokenRequestAttributeHandler();
        var csrfHandler = new CsrfTokenRequestHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> token) {
                xorHandler.handle(request, response, token);
            }
            @Override
            public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
                return request.getHeader(token.getHeaderName()) == null ? null
                        : xorHandler.resolveCsrfTokenValue(request, token);
            }
        };
        http.csrf(csrf -> csrf.csrfTokenRepository(csrfCookies).csrfTokenRequestHandler(csrfHandler));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment,
            ObjectMapper mapper, ApiErrorFactory errors, JwtDecoder decoder) throws Exception {
        boolean local = environment.acceptsProfiles(Profiles.of("local"))
                && !environment.acceptsProfiles(Profiles.of("prod"));
        http.authorizeHttpRequests(authorize -> {
            authorize.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
            authorize.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
            authorize.requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated();
            if (local) {
                authorize.requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html",
                        "/swagger-ui/**").permitAll();
            }
            authorize.anyRequest().denyAll();
        });
        configureStateless(http, mapper, errors);
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/users/me", "/api/v1/admin/**"));
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var authentication = new JwtAuthenticationConverter();
        authentication.setJwtGrantedAuthoritiesConverter(authorities);
        http.oauth2ResourceServer(resource -> resource
                .jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(authentication))
                .authenticationEntryPoint((request, response, ex) -> writeError(request, response,
                        mapper, errors, 401, "UNAUTHORIZED", "Authentication is required."))
                .accessDeniedHandler((request, response, ex) -> writeError(request, response,
                        mapper, errors, 403, "FORBIDDEN", "Access is denied.")));
        return http.build();
    }

    private void configureStateless(HttpSecurity http, ObjectMapper mapper, ApiErrorFactory errors) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.requestCache(cache -> cache.disable());
        http.formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable());
        http.exceptionHandling(handling -> handling
                .authenticationEntryPoint((request, response, ex) -> writeError(request, response,
                        mapper, errors, 401, "UNAUTHORIZED", "Authentication is required."))
                .accessDeniedHandler((request, response, ex) -> writeError(request, response,
                        mapper, errors, 403, "FORBIDDEN", "Access is denied.")));
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, ObjectMapper mapper,
            ApiErrorFactory errors, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), errors.create(status, code, message, request.getRequestURI()));
    }
}
