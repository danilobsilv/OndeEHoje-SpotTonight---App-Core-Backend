package com.ondeehoje.establishment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ondeehoje.auth.dto.LoginRequest;
import com.ondeehoje.auth.dto.RegisterRequest;
import com.ondeehoje.auth.service.AccessTokenService;
import com.ondeehoje.auth.service.AuthService;
import com.ondeehoje.establishment.dto.EstablishmentCreateRequest;
import com.ondeehoje.user.model.User;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(EstablishmentCreateIntegrationTest.FixedClockConfiguration.class)
class EstablishmentCreateIntegrationTest {
    private static final String PATH = "/api/v1/users/me/establishments";
    private static final UUID CATEGORY_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-30T12:34:56Z");
    private static final String PASSWORD = "correct horse battery staple";

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock creationTestClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthService auth;
    @Autowired AccessTokenService accessTokens;
    @Autowired JwtDecoder decoder;
    private UUID ownerId;
    private String email;
    private String access;
    private Cookie refresh;

    @BeforeEach
    void authenticateActiveUser() {
        email = UUID.randomUUID() + "@example.com";
        ownerId = auth.register(new RegisterRequest("Alice", email, PASSWORD)).id();
        var session = auth.login(new LoginRequest(email, PASSWORD));
        access = session.accessToken().accessToken();
        refresh = new Cookie("refresh_token", session.refreshToken());
        assertThat(decoder.decode(access).getSubject()).isEqualTo(ownerId.toString());
    }

    @Test
    void authenticatedUserCanCreateEstablishmentWithJwtOwnerAndInitialState() throws Exception {
        ObjectNode submitted = validRequest();
        assertThat(submitted.has("ownerId")).isFalse();
        var result = mvc.perform(create(submitted)).andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.createdAt").value(NOW.toString()))
                .andExpect(jsonPath("$.updatedAt").value(NOW.toString()))
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.ownerId").doesNotExist())
                .andExpect(jsonPath("$.category").doesNotExist()).andReturn();
        var response = mapper.readTree(result.getResponse().getContentAsString());
        submitted.fields().forEachRemaining(field ->
                assertThat(response.get(field.getKey())).as(field.getKey()).isEqualTo(field.getValue()));
        assertThat(response.size()).isEqualTo(22);
        UUID id = UUID.fromString(response.get("id").asText());

        // No test transaction: this reads the row committed by the service transaction.
        var row = jdbc.queryForMap("select * from establishments where id=?", id);
        assertThat(row.get("owner_id")).isEqualTo(UUID.fromString(decoder.decode(access).getSubject()));
        assertThat(row.get("category_id")).isEqualTo(CATEGORY_ID);
        assertThat(row.get("name")).isEqualTo("River Cafe");
        assertThat(row.get("description")).isEqualTo("Coffee by the river");
        assertThat(row.get("phone")).isEqualTo("+5592999991234");
        assertThat(row.get("website_url")).isEqualTo("https://example.com");
        assertThat(row.get("instagram_handle")).isEqualTo("rivercafe");
        assertThat(row.get("street")).isEqualTo("River Street");
        assertThat(row.get("number")).isEqualTo("42");
        assertThat(row.get("complement")).isEqualTo("Second floor");
        assertThat(row.get("neighborhood")).isEqualTo("Centro");
        assertThat(row.get("city")).isEqualTo("Manaus");
        assertThat(row.get("state")).isEqualTo("AM");
        assertThat(row.get("postal_code")).isEqualTo("69000-000");
        assertThat(row.get("country_code")).isEqualTo("BR");
        assertThat(row.get("latitude")).isEqualTo(-3.119);
        assertThat(row.get("longitude")).isEqualTo(-60.021);
        assertThat(row.get("time_zone")).isEqualTo("America/Manaus");
        assertThat(row.get("status")).isEqualTo("PENDING_REVIEW");
        assertThat(row.get("version")).isEqualTo(0L);
        assertThat(((Timestamp) row.get("created_at")).toInstant()).isEqualTo(NOW);
        assertThat(((Timestamp) row.get("updated_at")).toInstant()).isEqualTo(NOW);
    }

    @Test
    void clientCannotSetServerManagedFields() throws Exception {
        for (String field : List.of("id", "ownerId", "status", "version", "createdAt", "updatedAt")) {
            var body = validRequest().put(field, "client-controlled");
            assertRejected(create(body), 400, "BAD_REQUEST", "The request could not be processed.");
        }
        for (String status : List.of("APPROVED", "REJECTED", "SUSPENDED")) {
            assertRejected(create(validRequest().put("status", status)), 400,
                    "BAD_REQUEST", "The request could not be processed.");
        }
    }

    @Test
    void inactiveCategoryIsRejected() throws Exception {
        jdbc.update("update categories set active=false where id=?", CATEGORY_ID);
        try {
            assertRejected(create(validRequest()), 400, "INVALID_CATEGORY", "Invalid category.");
        } finally {
            jdbc.update("update categories set active=true where id=?", CATEGORY_ID);
        }
    }

    @Test
    void nonexistentCategoryIsRejected() throws Exception {
        assertRejected(create(validRequest().put("categoryId", UUID.randomUUID().toString())),
                400, "INVALID_CATEGORY", "Invalid category.");
    }

    @Test
    void invalidTimeZoneIsRejectedWithoutLeakingDetails() throws Exception {
        for (String zone : List.of("Not/A_Real_Time_Zone", "invalid time zone")) {
            assertRejected(create(validRequest().put("timeZone", zone)),
                    400, "INVALID_TIME_ZONE", "Invalid time zone.");
        }
    }

    @Test
    void disabledUserCannotCreateWithStillValidJwt() throws Exception {
        jdbc.update("update users set status='DISABLED' where id=?", ownerId);
        assertThat(decoder.decode(access).getSubject()).isEqualTo(ownerId.toString());
        assertRejected(create(validRequest()), 401, "UNAUTHORIZED", "Authentication is required.");
    }

    @Test
    void missingCurrentUserCannotCreate() throws Exception {
        var missing = new User("Missing", UUID.randomUUID() + "@example.com", "unused", NOW);
        access = accessTokens.issue(missing).accessToken();
        assertRejected(create(validRequest()), 401, "UNAUTHORIZED", "Authentication is required.");
    }

    @Test
    void authenticationStillApplies() throws Exception {
        String body = mapper.writeValueAsString(validRequest());
        assertRejected(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body),
                401, "UNAUTHORIZED", "Authentication is required.");
        assertRejected(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body).cookie(refresh),
                401, "UNAUTHORIZED", "Authentication is required.");
        assertRejected(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer invalid-token"),
                401, "UNAUTHORIZED", "Authentication is required.");
        assertThat(auth.refresh(refresh.getValue())).isNotNull();
    }

    @Test
    void validationStillApplies() throws Exception {
        assertRejected(create(validRequest().put("name", " ")), 400,
                "VALIDATION_ERROR", "Request validation failed.");
        assertRejected(create(validRequest().put("latitude", 91)), 400,
                "VALIDATION_ERROR", "Request validation failed.");
        var missing = validRequest();
        missing.remove("categoryId");
        assertRejected(create(missing), 400, "VALIDATION_ERROR", "Request validation failed.");
    }

    @Test
    void adminOnlyJwtCreatesForItsOwnSubject() throws Exception {
        jdbc.update("update user_roles set role='ADMIN' where user_id=?", ownerId);
        access = auth.login(new LoginRequest(email, PASSWORD)).accessToken().accessToken();
        assertThat(decoder.decode(access).getClaimAsStringList("roles")).containsExactly("ADMIN");
        var result = mvc.perform(create(validRequest())).andExpect(status().isCreated()).andReturn();
        UUID id = UUID.fromString(mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
        assertThat(jdbc.queryForObject("select owner_id from establishments where id=?", UUID.class, id))
                .isEqualTo(ownerId);
    }

    private void assertRejected(MockHttpServletRequestBuilder request, int status, String code, String message)
            throws Exception {
        Long before = jdbc.queryForObject("select count(*) from establishments", Long.class);
        mvc.perform(request).andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code)).andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value(PATH)).andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.exception").doesNotExist()).andExpect(jsonPath("$.trace").doesNotExist());
        assertThat(jdbc.queryForObject("select count(*) from establishments", Long.class)).isEqualTo(before);
    }

    private MockHttpServletRequestBuilder create(ObjectNode body) throws Exception {
        // Deliberately no cookies or CSRF header: creation authenticates through Bearer JWT only.
        return post(PATH).header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body));
    }

    private ObjectNode validRequest() {
        return mapper.valueToTree(new EstablishmentCreateRequest(CATEGORY_ID, "River Cafe", "Coffee by the river",
                "+5592999991234", "https://example.com", "rivercafe", "River Street", "42", "Second floor",
                "Centro", "Manaus", "AM", "69000-000", "BR", -3.119, -60.021, "America/Manaus"));
    }
}
