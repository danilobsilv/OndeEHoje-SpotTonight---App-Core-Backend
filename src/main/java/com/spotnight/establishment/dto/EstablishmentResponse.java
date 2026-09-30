package com.spotnight.establishment.dto;

import com.spotnight.establishment.model.EstablishmentStatus;
import java.time.Instant;
import java.util.UUID;

public record EstablishmentResponse(
        UUID id,
        UUID categoryId,
        String name,
        String description,
        String phone,
        String websiteUrl,
        String instagramHandle,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String postalCode,
        String countryCode,
        double latitude,
        double longitude,
        String timeZone,
        EstablishmentStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt) {}
