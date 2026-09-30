package com.ondeehoje.establishment.service;

import com.ondeehoje.category.model.Category;
import com.ondeehoje.category.repository.CategoryRepository;
import com.ondeehoje.establishment.dto.EstablishmentCreateRequest;
import com.ondeehoje.establishment.dto.EstablishmentResponse;
import com.ondeehoje.establishment.mapper.EstablishmentMapper;
import com.ondeehoje.establishment.model.Establishment;
import com.ondeehoje.establishment.repository.EstablishmentRepository;
import com.ondeehoje.shared.error.ApiException;
import com.ondeehoje.user.model.UserStatus;
import com.ondeehoje.user.repository.UserRepository;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstablishmentService {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final EstablishmentRepository establishments;
    private final EstablishmentMapper mapper;
    private final Clock clock;

    public EstablishmentService(UserRepository users, CategoryRepository categories,
            EstablishmentRepository establishments, EstablishmentMapper mapper, Clock clock) {
        this.users = users;
        this.categories = categories;
        this.establishments = establishments;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public EstablishmentResponse create(UUID ownerId, EstablishmentCreateRequest request) {
        var owner = users.findById(ownerId).filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(401, "UNAUTHORIZED", "Authentication is required."));
        var category = categories.findById(request.categoryId()).filter(Category::isActive)
                .orElseThrow(() -> new ApiException(400, "INVALID_CATEGORY", "Invalid category."));
        try {
            ZoneId.of(request.timeZone());
        } catch (DateTimeException ex) {
            throw new ApiException(400, "INVALID_TIME_ZONE", "Invalid time zone.");
        }

        Instant now = clock.instant();
        var establishment = new Establishment(owner, category, request.name(), request.description(),
                request.phone(), request.websiteUrl(), request.instagramHandle(), request.street(),
                request.number(), request.complement(), request.neighborhood(), request.city(), request.state(),
                request.postalCode(), request.countryCode(), request.latitude(), request.longitude(),
                request.timeZone(), now);
        return mapper.toResponse(establishments.saveAndFlush(establishment));
    }
}
