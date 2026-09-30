package com.spotnight.establishment;

import static org.assertj.core.api.Assertions.*;

import com.spotnight.category.model.Category;
import com.spotnight.establishment.model.Establishment;
import com.spotnight.establishment.repository.EstablishmentRepository;
import com.spotnight.user.model.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class EstablishmentRepositoryIntegrationTest {
    private static final Instant BASE_TIME = Instant.parse("2026-09-19T12:00:00Z");
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
    @Autowired EstablishmentRepository establishments;

    private User ownerA;
    private User ownerB;
    private Category category;

    @BeforeEach
    void setUp() {
        ownerA = persistUser("Owner A");
        ownerB = persistUser("Owner B");
        category = entityManager.find(Category.class, CATEGORY_ID);
    }

    @Test
    void listReturnsOnlyRequestedOwnersEstablishments() {
        Establishment a1 = persistEstablishment(ownerA, "River Bar", BASE_TIME);
        Establishment a2 = persistEstablishment(ownerA, "Central Bar", BASE_TIME.plusSeconds(60));
        Establishment b1 = persistEstablishment(ownerB, "Garden Bar", BASE_TIME.plusSeconds(120));
        flushAndClear();

        assertThat(establishments.findAllByOwner_IdOrderByCreatedAtDescIdAsc(ownerA.getId()))
                .extracting(Establishment::getId).containsExactly(a2.getId(), a1.getId());
        assertThat(establishments.findAllByOwner_IdOrderByCreatedAtDescIdAsc(ownerB.getId()))
                .extracting(Establishment::getId).containsExactly(b1.getId());
    }

    @Test
    void listUsesDeterministicOrdering() {
        Establishment tiedA = persistEstablishment(ownerA, "First Tied Bar", BASE_TIME.plusSeconds(60));
        Establishment oldest = persistEstablishment(ownerA, "Oldest Bar", BASE_TIME);
        Establishment newest = persistEstablishment(ownerA, "Newest Bar", BASE_TIME.plusSeconds(120));
        Establishment tiedB = persistEstablishment(ownerA, "Second Tied Bar", BASE_TIME.plusSeconds(60));
        flushAndClear();

        // Canonical UUID strings follow PostgreSQL's unsigned byte ordering.
        // UUID.compareTo uses signed longs and can disagree with PostgreSQL.
        List<UUID> tiedIds = List.of(tiedA.getId(), tiedB.getId()).stream()
                .sorted(Comparator.comparing(UUID::toString)).toList();
        var result = establishments.findAllByOwner_IdOrderByCreatedAtDescIdAsc(ownerA.getId());

        assertThat(result).extracting(Establishment::getCreatedAt).containsExactly(
                BASE_TIME.plusSeconds(120), BASE_TIME.plusSeconds(60),
                BASE_TIME.plusSeconds(60), BASE_TIME);
        assertThat(result).extracting(Establishment::getId).containsExactly(
                newest.getId(), tiedIds.get(0), tiedIds.get(1), oldest.getId());
    }

    @Test
    void listReturnsEmptyForOwnerWithoutEstablishments() {
        persistEstablishment(ownerB, "Garden Bar", BASE_TIME);
        flushAndClear();

        assertThat(establishments.findAllByOwner_IdOrderByCreatedAtDescIdAsc(ownerA.getId())).isEmpty();
    }

    @Test
    void detailReturnsOwnedEstablishment() {
        Establishment owned = persistEstablishment(ownerA, "River Bar", BASE_TIME);
        persistEstablishment(ownerB, "Garden Bar", BASE_TIME);
        flushAndClear();

        assertThat(establishments.findByIdAndOwner_Id(owned.getId(), ownerA.getId()))
                .hasValueSatisfying(loaded -> {
                    assertThat(loaded.getId()).isEqualTo(owned.getId());
                    assertThat(loaded.getName()).isEqualTo("River Bar");
                    assertThat(loaded.getOwner().getId()).isEqualTo(ownerA.getId());
                });
    }

    @Test
    void detailDoesNotExposeAnotherOwnersEstablishment() {
        Establishment owned = persistEstablishment(ownerA, "River Bar", BASE_TIME);
        persistEstablishment(ownerB, "Garden Bar", BASE_TIME);
        flushAndClear();

        assertThat(establishments.findByIdAndOwner_Id(owned.getId(), ownerB.getId())).isEmpty();
    }

    @Test
    void detailReturnsEmptyForUnknownId() {
        persistEstablishment(ownerA, "River Bar", BASE_TIME);
        persistEstablishment(ownerB, "Garden Bar", BASE_TIME);
        flushAndClear();

        assertThat(establishments.findByIdAndOwner_Id(UUID.randomUUID(), ownerA.getId())).isEmpty();
    }

    @Test
    void relationshipsRemainLazy() {
        Establishment owned = persistEstablishment(ownerA, "River Bar", BASE_TIME);
        persistEstablishment(ownerB, "Garden Bar", BASE_TIME);
        flushAndClear();

        var listed = establishments.findAllByOwner_IdOrderByCreatedAtDescIdAsc(ownerA.getId());
        assertThat(listed).hasSize(1).allSatisfy(this::assertLazyRelationships);
        entityManager.clear();

        var detail = establishments.findByIdAndOwner_Id(owned.getId(), ownerA.getId());
        assertThat(detail).hasValueSatisfying(this::assertLazyRelationships);
    }

    private void assertLazyRelationships(Establishment establishment) {
        assertThat(Hibernate.isInitialized(establishment.getOwner())).isFalse();
        assertThat(Hibernate.isInitialized(establishment.getCategory())).isFalse();
    }

    private User persistUser(String name) {
        User user = new User(name, "owner-" + UUID.randomUUID() + "@example.com",
                "$2a$10$" + "A".repeat(53), BASE_TIME.minusSeconds(60));
        entityManager.persist(user);
        return user;
    }

    private Establishment persistEstablishment(User owner, String name, Instant createdAt) {
        Establishment establishment = new Establishment(owner, category, name, null,
                null, null, null, "River Street", null, null, null, "Manaus", "AM", null,
                "BR", -3.119, -60.0217, "America/Manaus", createdAt);
        entityManager.persist(establishment);
        return establishment;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
