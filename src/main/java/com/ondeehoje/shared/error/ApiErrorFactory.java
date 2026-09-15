package com.ondeehoje.shared.error;

import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorFactory {
    private final Clock clock;

    public ApiErrorFactory(Clock clock) {
        this.clock = clock;
    }

    public ApiError create(int status, String code, String message, String path) {
        return new ApiError(clock.instant(), status, code, message, path, List.of());
    }
}
