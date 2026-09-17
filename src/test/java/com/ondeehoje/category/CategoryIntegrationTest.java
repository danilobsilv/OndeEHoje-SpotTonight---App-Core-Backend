package com.ondeehoje.category;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondeehoje.auth.security.AuthProperties;
import com.ondeehoje.category.model.Category;
import com.ondeehoje.category.repository.CategoryRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CategoryIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CategoryRepository categories;
    @Autowired ObjectMapper mapper;
    @Autowired Flyway flyway;
    @Autowired JwtEncoder encoder;
    @Autowired AuthProperties auth;
    @Autowired Clock clock;

    @Test
    void flywayCreatesAndSeedsStableCategories() {
        assertThat(Arrays.stream(flyway.info().applied()).map(m -> m.getVersion().getVersion()))
                .containsExactly("0", "1", "2", "3");
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        var seeded = categories.findAll();
        assertThat(seeded).hasSize(5);
        assertThat(seeded).extracting(Category::getSlug).doesNotHaveDuplicates();
        Map<String, String> names = Map.of("bar", "Bar", "restaurant", "Restaurant",
                "cafe", "Café", "nightclub", "Nightclub", "pub", "Pub");
        List<String> identities = List.of("bar", "restaurant", "cafe", "nightclub", "pub");
        for (Category category : seeded) {
            assertThat(category.getName()).isEqualTo(names.get(category.getSlug()));
            assertThat(category.getId()).isEqualTo(UUID.fromString(
                    "00000000-0000-4000-8000-%012d".formatted(identities.indexOf(category.getSlug()) + 1)));
            assertThat(category.isActive()).isTrue();
            assertThat(category.getCreatedAt()).isEqualTo(Instant.parse("2026-09-17T00:00:00Z"));
        }
    }

    @Test
    void duplicateSlugIsRejectedByNamedDatabaseConstraint() {
        assertThat(jdbc.queryForObject("select count(*) from pg_constraint where conrelid='categories'::regclass "
                + "and conname='uk_categories_slug' and contype='u'", Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> insert(UUID.randomUUID(), "Another bar", "bar", true,
                Timestamp.from(clock.instant()))).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(categories.count()).isEqualTo(5);
    }

    @Test
    void databaseRejectsMissingFieldsBlankNamesAndNoncanonicalSlugs() {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(clock.instant());
        Object[][] invalid = {
                {null, "Name", "valid", true, now},
                {id, null, "valid", true, now},
                {id, "", "valid", true, now},
                {id, " \t\n", "valid", true, now},
                {id, "Name", null, true, now},
                {id, "Name", "", true, now},
                {id, "Name", "UPPER", true, now},
                {id, "Name", "has space", true, now},
                {id, "Name", "valid", null, now},
                {id, "Name", "valid", true, null}
        };
        for (Object[] values : invalid) {
            assertThatThrownBy(() -> jdbc.update(
                    "insert into categories(id,name,slug,active,created_at) values (?,?,?,?,?)", values))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
        assertThat(categories.count()).isEqualTo(5);
    }

    @Test
    void publicEndpointReturnsOnlyDtoFieldsWithoutAuthenticationOrCsrf() throws Exception {
        var response = mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$[*].name", org.hamcrest.Matchers.contains(
                        "Bar", "Café", "Nightclub", "Pub", "Restaurant")))
                .andExpect(header().exists("X-Request-Id")).andReturn().getResponse();
        var json = mapper.readTree(response.getContentAsString());
        for (var category : json) {
            var fields = new HashSet<String>();
            category.fieldNames().forEachRemaining(fields::add);
            assertThat(fields).containsExactlyInAnyOrder("id", "name", "slug");
            assertThat(UUID.fromString(category.get("id").asText())).isNotNull();
        }
    }

    @Test
    @Transactional
    void queryAndApiExcludeInactiveCategoriesAndOrderNameTiesBySlug() throws Exception {
        jdbc.update("update categories set active=false where slug='bar'");
        insert(UUID.randomUUID(), "AAA hidden", "hidden", false, Timestamp.from(clock.instant()));
        insert(UUID.randomUUID(), "Same name", "same-b", true, Timestamp.from(clock.instant()));
        insert(UUID.randomUUID(), "Same name", "same-a", true, Timestamp.from(clock.instant()));
        var expected = List.of("cafe", "nightclub", "pub", "restaurant", "same-a", "same-b");
        assertThat(categories.findByActiveTrueOrderByNameAscSlugAsc())
                .allMatch(Category::isActive).extracting(Category::getSlug).containsExactlyElementsOf(expected);
        mvc.perform(get("/api/v1/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug", org.hamcrest.Matchers.contains(expected.toArray(String[]::new))));
    }

    @Test
    @Transactional
    void noActiveCategoriesReturnsAnEmptyArray() throws Exception {
        jdbc.update("update categories set active=false");
        mvc.perform(get("/api/v1/categories")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void publicCategoryPermissionDoesNotExposeOtherReadsOrCategoryWrites() throws Exception {
        for (String path : List.of("/api/v1/users/me", "/api/v1/admin/probe",
                "/api/v1/categories/" + UUID.randomUUID(), "/actuator/env", "/v3/api-docs")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
        for (HttpMethod method : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
            for (String path : List.of("/api/v1/categories", "/api/v1/categories/" + UUID.randomUUID())) {
                mvc.perform(request(method, path))
                        .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
                // Valid Bearer authentication bypasses CSRF, but deny-by-default still rejects writes.
                for (String role : List.of("USER", "ADMIN")) {
                    mvc.perform(request(method, path).header("Authorization", "Bearer " + accessToken(role)))
                            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
                }
            }
        }
    }

    private void insert(UUID id, String name, String slug, boolean active, Timestamp createdAt) {
        jdbc.update("insert into categories(id,name,slug,active,created_at) values (?,?,?,?,?)",
                id, name, slug, active, createdAt);
    }

    private String accessToken(String role) {
        var now = clock.instant();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer(auth.issuer()).audience(List.of(auth.audience()))
                        .subject(UUID.randomUUID().toString()).issuedAt(now).expiresAt(now.plusSeconds(60))
                        .claim("roles", List.of(role)).build())).getTokenValue();
    }
}
