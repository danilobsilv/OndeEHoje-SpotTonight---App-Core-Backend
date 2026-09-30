package com.spotnight.auth.service;

import org.hibernate.exception.ConstraintViolationException;

/** Identifies only the named PostgreSQL email uniqueness violation, without inspecting messages. */
final class EmailUniqueConstraint {
    private EmailUniqueConstraint() {}

    static boolean isViolation(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "uk_users_email".equals(violation.getConstraintName())
                    && "23505".equals(violation.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
