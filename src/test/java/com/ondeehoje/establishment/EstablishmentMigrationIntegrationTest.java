package com.ondeehoje.establishment;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class EstablishmentMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;

    UUID ownerId;
    UUID categoryId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();

        jdbc.update("""
            INSERT INTO users (
                id,
                name,
                email,
                password_hash,
                status,
                created_at,
                updated_at
            )
            VALUES (
                ?,
                'Establishment Owner',
                ?,
                '$2a$10$' || repeat('A', 53),
                'ACTIVE',
                now(),
                now()
            )
            """,
            ownerId,
            "owner-" + ownerId + "@example.com"
        );

        categoryId = jdbc.queryForObject(
            "SELECT id FROM categories WHERE slug = 'bar'",
            UUID.class
        );
    }

    private void insertEstablishment(
        double latitude,
        double longitude,
        String status,
        String countryCode
) {
      jdbc.update("""
            INSERT INTO establishments (
                  id,
                  owner_id,
                  category_id,
                  name,
                  street,
                  city,
                  state,
                  country_code,
                  latitude,
                  longitude,
                  time_zone,
                  status,
                  created_at,
                  updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
            """,
            UUID.randomUUID(),
            ownerId,
            categoryId,
            "Test Establishment",
            "Test Street",
            "Manaus",
            "AM",
            countryCode,
            latitude,
            longitude,
            "America/Manaus",
            status
            );
      }


      @Test
      void flywayCreatesEstablishmentsTableAtVersion4() {
            assertThat(flyway.info().current().getVersion().getVersion())
                  .isEqualTo("4");

            assertThat(flyway.validateWithResult().validationSuccessful)
                  .isTrue();

            assertThat(flyway.migrate().migrationsExecuted)
                  .isZero();
      }

      @Test
      void insertsValidEstablishmentAndDefaultsVersionToZero() {
            UUID id = UUID.randomUUID();

            jdbc.update("""
                  INSERT INTO establishments (
                        id,
                        owner_id,
                        category_id,
                        name,
                        street,
                        city,
                        state,
                        country_code,
                        latitude,
                        longitude,
                        time_zone,
                        status,
                        created_at,
                        updated_at
                  )
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                  """,
                  id,
                  ownerId,
                  categoryId,
                  "Bar do Centro",
                  "Avenida Eduardo Ribeiro",
                  "Manaus",
                  "AM",
                  "BR",
                  -3.1190,
                  -60.0217,
                  "America/Manaus",
                  "PENDING_REVIEW"
            );

            Long version = jdbc.queryForObject(
                  "SELECT version FROM establishments WHERE id = ?",
                  Long.class,
                  id
            );

            assertThat(version).isZero();
      }

      @Test
      void rejectsInvalidLatitude() {
            assertThatThrownBy(() ->
                  insertEstablishment(91.0, -60.0, "PENDING_REVIEW", "BR")).isInstanceOf(DataIntegrityViolationException.class);

            assertThatThrownBy(() -> insertEstablishment(-91.0, -60.0, "PENDING_REVIEW", "BR")).isInstanceOf(DataIntegrityViolationException.class);
      }

      @Test
      void rejectsInvalidLongitude() {
            assertThatThrownBy(() -> insertEstablishment(-3.0, 181.0, "PENDING_REVIEW", "BR")).isInstanceOf(DataIntegrityViolationException.class);

            assertThatThrownBy(() -> insertEstablishment(-3.0, -181.0, "PENDING_REVIEW", "BR")).isInstanceOf(DataIntegrityViolationException.class);
      }

      @Test
      void rejectsUnknownStatus() {
            assertThatThrownBy(() ->
            insertEstablishment(
                  -3.1190,
                  -60.0217,
                  "DRAFT",
                  "BR"
            )
            ).isInstanceOf(DataIntegrityViolationException.class);
      }

      @Test
      void rejectsNoncanonicalCountryCode() {
            assertThatThrownBy(() ->
                  insertEstablishment(-3.0, -60.0, "PENDING_REVIEW", "br")
            ).isInstanceOf(DataIntegrityViolationException.class);

            assertThatThrownBy(() ->
                  insertEstablishment(-3.0, -60.0, "PENDING_REVIEW", "BRA")
            ).isInstanceOf(DataIntegrityViolationException.class);

            assertThatThrownBy(() ->
                  insertEstablishment(-3.0, -60.0, "PENDING_REVIEW", "B1")
            ).isInstanceOf(DataIntegrityViolationException.class);
      }

      @Test
      void rejectsUnknownOwner() {
            assertThatThrownBy(() ->
                  jdbc.update("""
                        INSERT INTO establishments (
                        id, owner_id, category_id,
                        name, street, city, state, country_code,
                        latitude, longitude, time_zone,
                        status, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                        """,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        categoryId,
                        "Test",
                        "Test Street",
                        "Manaus",
                        "AM",
                        "BR",
                        -3.1,
                        -60.0,
                        "America/Manaus",
                        "PENDING_REVIEW"
                  )
            ).isInstanceOf(DataIntegrityViolationException.class);
      }

      @Test
      void createsRequiredIndexes() {
            var indexes = jdbc.queryForList("""
                  SELECT indexname
                  FROM pg_indexes
                  WHERE schemaname = 'public'
                  AND tablename = 'establishments'
                  """, String.class);

            assertThat(indexes)
                  .contains(
                        "idx_establishments_owner_id",
                        "idx_establishments_status",
                        "idx_establishments_category_id",
                        "idx_establishments_status_category_id"
                  );
            }
}