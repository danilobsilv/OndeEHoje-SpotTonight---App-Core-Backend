package com.ondeehoje.user.dto;

import com.ondeehoje.user.model.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, UserStatus status,
                           Set<Role> roles, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getStatus(),
                user.getRoles(), user.getCreatedAt());
    }
}
