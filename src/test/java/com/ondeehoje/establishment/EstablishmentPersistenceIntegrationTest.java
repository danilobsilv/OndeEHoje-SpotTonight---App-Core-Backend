package com.ondeehoje.establishment;

import static org.assertj.core.api.Assertions.*;

import com.ondeehoje.category.model.Category;
import com.ondeehoje.establishment.model.Establishment;
import com.ondeehoje.establishment.model.EstablishmentStatus;
import com.ondeehoje.user.model.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class EstablishmentPersistenceIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-09-19T12:34:56.123456Z");
    private static final UUID CATEGORY_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired EntityManager entityManager;
    @Autowired Flyway flyway;
    @Autowired Environment environment;

    @Test
    void persistsAndReloadsAllFieldsWithCreationInvariants() {
        User owner = persistOwner();
        Category category = entityManager.find(Category.class, CATEGORY_ID);
        String description = "A detailed description. ".repeat(30);
        Establishment establishment = new Establishment(owner, category, "River Bar", description,
                "+5592999999999", "https://example.com", "river_bar",
                "River Street", "42", "Second floor", "Center", "Manaus", "AM", "69000000",
                "BR", -3.119, -60.0217, "America/Manaus", NOW);

        assertThat(establishment.getId()).isNotNull();
        assertThat(establishment.getStatus()).isEqualTo(EstablishmentStatus.PENDING_REVIEW);
        assertThat(establishment.getVersion()).isZero();
        assertThat(establishment.getCreatedAt()).isEqualTo(NOW);
        assertThat(establishment.getUpdatedAt()).isEqualTo(NOW);
        entityManager.persist(establishment);
        entityManager.flush();
        entityManager.clear();

        Establishment loaded = entityManager.find(Establishment.class, establishment.getId());
        assertThat(loaded).isNotNull().isNotSameAs(establishment);
        assertThat(loaded.getId()).isEqualTo(establishment.getId());
        assertThat(Hibernate.isInitialized(loaded.getOwner())).isFalse();
        assertThat(Hibernate.isInitialized(loaded.getCategory())).isFalse();
        assertThat(loaded.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(loaded.getCategory().getId()).isEqualTo(CATEGORY_ID);
        assertThat(loaded.getName()).isEqualTo("River Bar");
        assertThat(loaded.getDescription()).isEqualTo(description);
        assertThat(loaded.getPhone()).isEqualTo("+5592999999999");
        assertThat(loaded.getWebsiteUrl()).isEqualTo("https://example.com");
        assertThat(loaded.getInstagramHandle()).isEqualTo("river_bar");
        assertThat(loaded.getStreet()).isEqualTo("River Street");
        assertThat(loaded.getNumber()).isEqualTo("42");
        assertThat(loaded.getComplement()).isEqualTo("Second floor");
        assertThat(loaded.getNeighborhood()).isEqualTo("Center");
        assertThat(loaded.getCity()).isEqualTo("Manaus");
        assertThat(loaded.getState()).isEqualTo("AM");
        assertThat(loaded.getPostalCode()).isEqualTo("69000000");
        assertThat(loaded.getCountryCode()).isEqualTo("BR");
        assertThat(loaded.getLatitude()).isEqualTo(-3.119);
        assertThat(loaded.getLongitude()).isEqualTo(-60.0217);
        assertThat(loaded.getTimeZone()).isEqualTo("America/Manaus");
        assertThat(loaded.getStatus()).isEqualTo(EstablishmentStatus.PENDING_REVIEW);
        assertThat(loaded.getVersion()).isZero();
        assertThat(loaded.getCreatedAt()).isEqualTo(NOW);
        assertThat(loaded.getUpdatedAt()).isEqualTo(NOW);
        assertThat(entityManager.createNativeQuery("select status from establishments where id = :id")
                .setParameter("id", loaded.getId()).getSingleResult()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void persistsNullableFieldsAndMaximumLengthName() {
        Establishment establishment = new Establishment(persistOwner(),
                entityManager.find(Category.class, CATEGORY_ID), "N".repeat(150), null,
                null, null, null, "River Street", null, null, null, "Manaus", "AM", null,
                "BR", 0, 0, "America/Manaus", NOW);
        entityManager.persist(establishment);
        entityManager.flush();
        entityManager.clear();

        Establishment loaded = entityManager.find(Establishment.class, establishment.getId());
        assertThat(loaded.getName()).hasSize(150);
        assertThat(loaded.getDescription()).isNull();
        assertThat(loaded.getPhone()).isNull();
        assertThat(loaded.getWebsiteUrl()).isNull();
        assertThat(loaded.getInstagramHandle()).isNull();
        assertThat(loaded.getNumber()).isNull();
        assertThat(loaded.getComplement()).isNull();
        assertThat(loaded.getNeighborhood()).isNull();
        assertThat(loaded.getPostalCode()).isNull();
    }

    @Test
    void validatesEntitySchemaAgainstUnchangedFlywayVersion4() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("4");
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    }

    private User persistOwner() {
        User owner = new User("Establishment Owner", "owner-" + UUID.randomUUID() + "@example.com",
                "$2a$10$" + "A".repeat(53), NOW);
        entityManager.persist(owner);
        return owner;
    }
}
