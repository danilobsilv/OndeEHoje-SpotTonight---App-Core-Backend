package com.spotnight.user.service;

import com.spotnight.shared.error.ApiException;
import com.spotnight.user.dto.UserResponse;
import com.spotnight.user.model.UserStatus;
import com.spotnight.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    public UserService(UserRepository users) { this.users = users; }

    @Transactional(readOnly = true)
    public UserResponse current(UUID id) {
        var user = users.findById(id).filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(401, "UNAUTHORIZED", "Authentication is required."));
        return UserResponse.from(user);
    }
}
