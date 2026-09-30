package com.spotnight.establishment.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.spotnight.category.model.Category;
import com.spotnight.establishment.dto.EstablishmentResponse;
import com.spotnight.establishment.model.Establishment;
import com.spotnight.establishment.model.EstablishmentStatus;
import com.spotnight.user.model.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EstablishmentMapperTest {
    private static final Instant CREATED_AT = Instant.parse("2026-09-20T12:00:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private final EstablishmentMapper mapper = new EstablishmentMapper();

    @Test
    void mapsAllFieldsToResponse() {
        Establishment establishment = establishment(mock(User.class), category());

        EstablishmentResponse response = mapper.toResponse(establishment);

        assertThat(response.id()).isEqualTo(establishment.getId());
        assertThat(response.categoryId()).isEqualTo(CATEGORY_ID);
        assertThat(response.name()).isEqualTo("River Cafe");
        assertThat(response.description()).isEqualTo("Coffee by the river");
        assertThat(response.phone()).isEqualTo("+5592999991234");
        assertThat(response.websiteUrl()).isEqualTo("https://example.com");
        assertThat(response.instagramHandle()).isEqualTo("rivercafe");
        assertThat(response.street()).isEqualTo("River Street");
        assertThat(response.number()).isEqualTo("42");
        assertThat(response.complement()).isEqualTo("Second floor");
        assertThat(response.neighborhood()).isEqualTo("Centro");
        assertThat(response.city()).isEqualTo("Manaus");
        assertThat(response.state()).isEqualTo("AM");
        assertThat(response.postalCode()).isEqualTo("69000-000");
        assertThat(response.countryCode()).isEqualTo("BR");
        assertThat(response.latitude()).isEqualTo(-3.119);
        assertThat(response.longitude()).isEqualTo(-60.021);
        assertThat(response.timeZone()).isEqualTo("America/Manaus");
        assertThat(response.status()).isEqualTo(EstablishmentStatus.PENDING_REVIEW);
        assertThat(response.version()).isZero();
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
        assertThat(response.updatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void mapsCategoryAsCategoryId() {
        Category category = category();
        Establishment establishment = establishment(mock(User.class), category);

        EstablishmentResponse response = mapper.toResponse(establishment);

        assertThat(response.categoryId()).isEqualTo(CATEGORY_ID);
        verify(category).getId();
        verifyNoMoreInteractions(category);
        assertThat(EstablishmentResponse.class.getRecordComponents())
                .noneMatch(component -> component.getType().equals(Category.class));
    }

    @Test
    void doesNotExposeOwner() {
        User owner = mock(User.class);
        Establishment establishment = spy(establishment(owner, category()));

        mapper.toResponse(establishment);

        verify(establishment, never()).getOwner();
        verifyNoInteractions(owner);
        assertThat(EstablishmentResponse.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("id", "categoryId", "name", "description", "phone", "websiteUrl",
                        "instagramHandle", "street", "number", "complement", "neighborhood", "city",
                        "state", "postalCode", "countryCode", "latitude", "longitude", "timeZone",
                        "status", "version", "createdAt", "updatedAt");
        assertThat(EstablishmentResponse.class.getRecordComponents())
                .noneMatch(component -> component.getType().equals(User.class));
    }

    @Test
    void mapsNullableFields() {
        Establishment establishment = new Establishment(mock(User.class), category(), "River Cafe",
                null, null, null, null, "River Street", null, null, null, "Manaus", "AM", null,
                "BR", -3.119, -60.021, "America/Manaus", CREATED_AT);

        EstablishmentResponse response = mapper.toResponse(establishment);

        assertThat(response.description()).isNull();
        assertThat(response.phone()).isNull();
        assertThat(response.websiteUrl()).isNull();
        assertThat(response.instagramHandle()).isNull();
        assertThat(response.number()).isNull();
        assertThat(response.complement()).isNull();
        assertThat(response.neighborhood()).isNull();
        assertThat(response.postalCode()).isNull();
    }

    @ParameterizedTest
    @EnumSource(EstablishmentStatus.class)
    void mapsStatusVersionAndTimestamps(EstablishmentStatus status) {
        Establishment establishment = spy(establishment(mock(User.class), category()));
        Instant updatedAt = Instant.parse("2026-09-27T18:30:00Z");
        // Represent persisted metadata without introducing lifecycle mutation methods.
        doReturn(status).when(establishment).getStatus();
        doReturn(7L).when(establishment).getVersion();
        doReturn(updatedAt).when(establishment).getUpdatedAt();

        EstablishmentResponse response = mapper.toResponse(establishment);

        assertThat(response.status()).isEqualTo(status);
        assertThat(response.version()).isEqualTo(7L);
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }

    private Category category() {
        Category category = mock(Category.class);
        when(category.getId()).thenReturn(CATEGORY_ID);
        return category;
    }

    private Establishment establishment(User owner, Category category) {
        return new Establishment(owner, category, "River Cafe", "Coffee by the river",
                "+5592999991234", "https://example.com", "rivercafe", "River Street", "42",
                "Second floor", "Centro", "Manaus", "AM", "69000-000", "BR", -3.119, -60.021,
                "America/Manaus", CREATED_AT);
    }
}
