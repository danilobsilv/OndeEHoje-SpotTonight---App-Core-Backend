package com.ondeehoje.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class EmailUniqueConstraintTest {
    @Test
    void recognizesNamedUniqueViolationThroughWrappedCauses() {
        assertThat(EmailUniqueConstraint.isViolation(new DataIntegrityViolationException("ignored",
                new RuntimeException(violation("uk_users_email", "23505"))))).isTrue();
    }

    @Test
    void rejectsOtherConstraintsMissingMetadataAndWrongSqlState() {
        assertThat(EmailUniqueConstraint.isViolation(violation("users_pkey", "23505"))).isFalse();
        assertThat(EmailUniqueConstraint.isViolation(violation("users_email_normalized", "23514"))).isFalse();
        assertThat(EmailUniqueConstraint.isViolation(violation("uk_users_email", "23514"))).isFalse();
        assertThat(EmailUniqueConstraint.isViolation(violation(null, "23505"))).isFalse();
        assertThat(EmailUniqueConstraint.isViolation(violation("uk_users_email", null))).isFalse();
    }

    @Test
    void neverInfersTheConstraintFromAnExceptionMessage() {
        assertThat(EmailUniqueConstraint.isViolation(new DataIntegrityViolationException(
                "duplicate key violates uk_users_email SQLSTATE 23505"))).isFalse();
        assertThat(EmailUniqueConstraint.isViolation(new SQLException("uk_users_email", "23505"))).isFalse();
    }

    private ConstraintViolationException violation(String constraint, String state) {
        return new ConstraintViolationException("ignored", new SQLException("ignored", state), constraint);
    }
}
