package com.ondeehoje.establishment.mapper;

import com.ondeehoje.establishment.dto.EstablishmentResponse;
import com.ondeehoje.establishment.model.Establishment;
import org.springframework.stereotype.Component;

@Component
public class EstablishmentMapper {
    public EstablishmentResponse toResponse(Establishment establishment) {
        return new EstablishmentResponse(
                establishment.getId(),
                establishment.getCategory().getId(),
                establishment.getName(),
                establishment.getDescription(),
                establishment.getPhone(),
                establishment.getWebsiteUrl(),
                establishment.getInstagramHandle(),
                establishment.getStreet(),
                establishment.getNumber(),
                establishment.getComplement(),
                establishment.getNeighborhood(),
                establishment.getCity(),
                establishment.getState(),
                establishment.getPostalCode(),
                establishment.getCountryCode(),
                establishment.getLatitude(),
                establishment.getLongitude(),
                establishment.getTimeZone(),
                establishment.getStatus(),
                establishment.getVersion(),
                establishment.getCreatedAt(),
                establishment.getUpdatedAt());
    }
}
