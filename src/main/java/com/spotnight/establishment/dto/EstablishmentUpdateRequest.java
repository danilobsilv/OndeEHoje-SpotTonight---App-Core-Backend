package com.spotnight.establishment.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record EstablishmentUpdateRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 150) String name,
        String description,
        String phone,
        String websiteUrl,
        String instagramHandle,
        @NotBlank String street,
        String number,
        String complement,
        String neighborhood,
        @NotBlank String city,
        @NotBlank String state,
        String postalCode,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$") String countryCode,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @NotBlank String timeZone) {}
