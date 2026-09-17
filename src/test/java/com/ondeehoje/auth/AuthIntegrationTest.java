package com.ondeehoje.auth;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondeehoje.auth.security.AuthProperties;
import com.ondeehoje.auth.service.AuthService;
import com.ondeehoje.auth.service.AccessTokenService;
import com.ondeehoje.auth.dto.RegisterRequest;
import com.ondeehoje.auth.service.RefreshTokenCodec;
import com.ondeehoje.shared.error.ApiException;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.mockito.Mockito.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@org.springframework.context.annotation.Import(AuthIntegrationTest.AdminFixture.class)
class AuthIntegrationTest {
    @org.springframework.web.bind.annotation.RestController
    static class AdminFixture {
        @org.springframework.web.bind.annotation.GetMapping("/api/v1/admin/probe")
        String admin() { return "admin"; }
    }
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired RefreshTokenCodec codec;
    @Autowired AuthService auth;
    @MockitoSpyBean AccessTokenService accessTokens;
    @Autowired JwtEncoder encoder;
    @Autowired AuthProperties properties;
    private static final String PASSWORD = "correct horse battery staple";
    private String email;
    private Cookie csrfCookie;
    private String csrfHeader;
    private String csrfValue;

    @BeforeEach
    void setup() throws Exception {
        email = UUID.randomUUID() + "@example.com";
        var response = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse();
        csrfCookie = response.getCookie("__Host-csrf");
        var body = mapper.readTree(response.getContentAsString());
        csrfHeader = body.get("headerName").asText();
        csrfValue = body.get("token").asText();
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.isHttpOnly()).isTrue();
        assertThat(csrfCookie.getSecure()).isTrue();
    }

    @Test
    void registrationNormalizesEmailHashesPasswordAndRejectsDuplicates() throws Exception {
        var result = mvc.perform(secured("/register").content(json(Map.of(
                "name", " Alice ", "email", "  " + email.toUpperCase(Locale.ROOT) + " ", "password", PASSWORD))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Alice")).andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        UUID.fromString(mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
        String hash = jdbc.queryForObject("select password_hash from users where email = ?", String.class, email);
        assertThat(hash).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, hash)).isTrue();
        mvc.perform(secured("/register").content(registration(email.toUpperCase(Locale.ROOT))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void rejectsInvalidInputAndClientAssignedRoles() throws Exception {
        for (String field : List.of("role", "roles", "status", "id", "passwordHash")) {
            var body = new HashMap<String, Object>(Map.of("name", "Alice", "email", email, "password", PASSWORD));
            body.put(field, field.equals("roles") ? List.of("ADMIN") : "ADMIN");
            mvc.perform(secured("/register").content(json(body))).andExpect(status().isBadRequest());
        }
        assertThat(jdbc.queryForObject("select count(*) from users where email = ?", Integer.class, email)).isZero();
        for (Map<String, String> body : List.of(
                Map.of("name", "", "email", email, "password", PASSWORD),
                Map.of("name", "Alice", "email", "invalid", "password", PASSWORD),
                Map.of("name", "Alice", "email", email, "password", "short"),
                Map.of("name", "Alice", "email", email, "password", "é".repeat(37)))) {
            mvc.perform(secured("/register").content(json(body))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void loginMeAuthorizationRefreshRotationAndLogout() throws Exception {
        register();
        var login = login();
        var body = mapper.readTree(login.getResponse().getContentAsString());
        String access = body.get("accessToken").asText();
        assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.get("expiresIn").asLong()).isEqualTo(900);
        assertThat(body.has("refreshToken")).isFalse();
        Cookie refresh = login.getResponse().getCookie("refresh_token");
        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(refresh.getSecure()).isTrue();
        assertThat(refresh.getPath()).isEqualTo("/api/v1/auth");
        assertThat(login.getResponse().getHeader("Set-Cookie")).contains("SameSite=Strict");
        assertThat(jdbc.queryForObject("select token_hash from refresh_tokens where token_hash = ?",
                String.class, codec.hash(refresh.getValue()))).isEqualTo(codec.hash(refresh.getValue()))
                .isNotEqualTo(refresh.getValue());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/v1/users/me").cookie(refresh)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/probe").header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/actuator/env").header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden());
        var rotated = mvc.perform(secured("/refresh").cookie(refresh)).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString()).andReturn().getResponse().getCookie("refresh_token");
        assertThat(rotated.getValue()).isNotEqualTo(refresh.getValue());
        mvc.perform(secured("/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
        mvc.perform(secured("/logout").cookie(rotated)).andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refresh_token", 0));
        mvc.perform(secured("/refresh").cookie(rotated)).andExpect(status().isUnauthorized());
        mvc.perform(secured("/logout").cookie(rotated)).andExpect(status().isNoContent());
        // ADR-003: logout does not revoke an already issued access JWT.
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + access)).andExpect(status().isOk());
    }

    @Test
    void invalidCredentialsAndDisabledAccountsCannotIssueTokens() throws Exception {
        register();
        for (String address : List.of(email, "missing@example.com")) {
            mvc.perform(secured("/login").content(json(Map.of("email", address, "password", "wrong password"))))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.message").value("Invalid email or password."));
        }
        Cookie refresh = login().getResponse().getCookie("refresh_token");
        jdbc.update("update users set status = 'DISABLED' where email = ?", email);
        mvc.perform(secured("/login").content(credentials())).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mvc.perform(secured("/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMalformedExpiredAndWrongIssuerOrAudienceAccessTokens() throws Exception {
        register();
        String id = jdbc.queryForObject("select id::text from users where email = ?", String.class, email);
        Instant now = Instant.now();
        for (String token : List.of("not-a-jwt",
                signed(id, properties.issuer(), properties.audience(), now.minusSeconds(100), now.minusSeconds(1)),
                signed(id, "wrong", properties.audience(), now.minusSeconds(1), now.plusSeconds(100)),
                signed(id, properties.issuer(), "wrong", now.minusSeconds(1), now.plusSeconds(100)),
                signed("not-a-uuid", properties.issuer(), properties.audience(), now.minusSeconds(1), now.plusSeconds(100)))) {
            mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.fieldErrors").isArray()).andExpect(header().exists("X-Request-Id"));
        }
        String valid = mapper.readTree(login().getResponse().getContentAsString()).get("accessToken").asText();
        String[] parts = valid.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "." + (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csrfIsRequiredForEveryAuthPostEvenWithBearerAndCookie() throws Exception {
        register();
        var login = login();
        String access = mapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
        Cookie refresh = login.getResponse().getCookie("refresh_token");
        for (String endpoint : List.of("/register", "/login", "/refresh", "/logout")) {
            mvc.perform(post("/api/v1/auth" + endpoint).cookie(refresh, csrfCookie)
                    .header("Authorization", "Bearer " + access)
                    .contentType(MediaType.APPLICATION_JSON).content(credentials()))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
            mvc.perform(post("/api/v1/auth" + endpoint).cookie(refresh, csrfCookie)
                    .header(csrfHeader, "invalid")).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/auth" + endpoint).cookie(refresh, csrfCookie)
                    .param("_csrf", csrfValue)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/auth/refresh").header(csrfHeader, csrfValue).cookie(refresh))
                .andExpect(status().isForbidden());
        mvc.perform(options("/api/v1/auth/refresh").header("Origin", "https://attacker.example")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", csrfHeader))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(secured("/refresh").cookie(refresh)).andExpect(status().isOk());
    }

    @Test
    void expiredMissingAndUnknownRefreshTokensAreRejected() throws Exception {
        register();
        Cookie refresh = login().getResponse().getCookie("refresh_token");
        jdbc.update("update refresh_tokens set created_at = now() - interval '2 days', "
                + "expires_at = now() - interval '1 day' where token_hash = ?", codec.hash(refresh.getValue()));
        mvc.perform(secured("/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
        mvc.perform(secured("/refresh")).andExpect(status().isUnauthorized());
        mvc.perform(secured("/refresh").cookie(new Cookie("refresh_token", codec.generate())))
                .andExpect(status().isUnauthorized());
        mvc.perform(secured("/logout")).andExpect(status().isNoContent());
    }

    @Test
    void concurrentRefreshHasExactlyOneWinner() throws Exception {
        register();
        String raw = login().getResponse().getCookie("refresh_token").getValue();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var gate = new CountDownLatch(1);
            Callable<Boolean> attempt = () -> {
                gate.await();
                try { auth.refresh(raw); return true; }
                catch (ApiException ex) { assertThat(ex.getCode()).isEqualTo("INVALID_REFRESH_TOKEN"); return false; }
            };
            var first = pool.submit(attempt);
            var second = pool.submit(attempt);
            gate.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(jdbc.queryForObject("select count(*) from refresh_tokens t join users u on u.id=t.user_id "
                + "where u.email=? and t.revoked_at is null", Integer.class, email)).isEqualTo(1);
    }

    @Test
    void databaseRejectsUnnormalizedEmailInvalidRolesAndRawTokens() throws Exception {
        register();
        UUID id = jdbc.queryForObject("select id from users where email=?", UUID.class, email);
        assertThatThrownBy(() -> jdbc.update("update users set email=? where id=?", email.toUpperCase(Locale.ROOT), id))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into user_roles(user_id,role) values (?,?)", id, "OWNER"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into refresh_tokens(id,user_id,token_hash,created_at,expires_at)"
                + " values (?,?,?,now(),now()+interval '1 day')", UUID.randomUUID(), id, codec.generate()))
                .isInstanceOf(DataIntegrityViolationException.class);
        String other = UUID.randomUUID() + "@example.com";
        mvc.perform(secured("/register").content(registration(other))).andExpect(status().isCreated());
        assertThatThrownBy(() -> jdbc.update("update users set email=? where email=?", email, other))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void failedIssuanceRollsBackRefreshRevocationAndReplacement() throws Exception {
        register();
        String raw = login().getResponse().getCookie("refresh_token").getValue();
        doThrow(new IllegalStateException("simulated signer failure")).when(accessTokens).issue(any());
        try {
            assertThatThrownBy(() -> auth.refresh(raw)).isInstanceOf(IllegalStateException.class);
            assertThat(jdbc.queryForObject("select revoked_at is null from refresh_tokens where token_hash=?",
                    Boolean.class, codec.hash(raw))).isTrue();
            assertThat(jdbc.queryForObject("select count(*) from refresh_tokens t join users u on u.id=t.user_id "
                    + "where u.email=?", Integer.class, email)).isEqualTo(1);
        } finally {
            reset(accessTokens);
        }
        assertThat(auth.refresh(raw)).isNotNull();
    }

    @Test
    void concurrentRegistrationHasOneAccountAndOneConflict() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var gate = new CountDownLatch(1);
            Callable<Integer> attempt = () -> {
                gate.await();
                try { auth.register(new RegisterRequest("Alice", email, PASSWORD)); return 201; }
                catch (ApiException ex) { return ex.getStatus(); }
            };
            var first = pool.submit(attempt);
            var second = pool.submit(attempt);
            gate.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("select count(*) from users where email=?", Integer.class, email)).isEqualTo(1);
    }

    @Test
    void controlledAdminRoleCanAccessAdminRoute() throws Exception {
        register();
        jdbc.update("insert into user_roles(user_id,role) select id,'ADMIN' from users where email=?", email);
        String access = mapper.readTree(login().getResponse().getContentAsString()).get("accessToken").asText();
        mvc.perform(get("/api/v1/admin/probe").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(content().string("admin"));
    }

    private String signed(String subject, String issuer, String audience, Instant issued, Instant expires) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().subject(subject).issuer(issuer).audience(List.of(audience))
                        .issuedAt(issued).expiresAt(expires).claim("roles", List.of("USER")).build())).getTokenValue();
    }
    private MockHttpServletRequestBuilder secured(String endpoint) {
        return post("/api/v1/auth" + endpoint).cookie(csrfCookie).header(csrfHeader, csrfValue)
                .contentType(MediaType.APPLICATION_JSON);
    }
    private String json(Object value) throws Exception { return mapper.writeValueAsString(value); }
    private String registration(String address) throws Exception {
        return json(Map.of("name", "Alice", "email", address, "password", PASSWORD));
    }
    private String credentials() throws Exception { return json(Map.of("email", email, "password", PASSWORD)); }
    private void register() throws Exception {
        mvc.perform(secured("/register").content(registration(email))).andExpect(status().isCreated());
    }
    private MvcResult login() throws Exception {
        return mvc.perform(secured("/login").content(credentials())).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store")).andReturn();
    }
}
