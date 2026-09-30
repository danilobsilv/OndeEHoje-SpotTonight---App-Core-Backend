package com.spotnight.auth.dto;

import jakarta.validation.constraints.*;
import java.util.Locale;

public record RegisterRequest(@NotBlank @Size(max = 120) String name,
                              @NotBlank @Email @Size(max = 254) String email,
                              @NotBlank @Size(min = 12, max = 72) String password) {
    public RegisterRequest {
        if (name != null) name = name.strip();
        if (email != null) email = email.strip().toLowerCase(Locale.ROOT);
    }
    @Override public String toString() { return "RegisterRequest[REDACTED]"; }
}
