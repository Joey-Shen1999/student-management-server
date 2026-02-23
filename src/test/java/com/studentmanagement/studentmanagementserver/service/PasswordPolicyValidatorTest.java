package com.studentmanagement.studentmanagementserver.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyValidatorTest {

    private final PasswordPolicyValidator validator = new PasswordPolicyValidator();

    @Test
    void validate_failsWhenLengthLessThan8() {
        List<String> failures = validator.validate("alice", "Aa1!");
        assertTrue(failures.contains("Password must be at least 8 characters."));
    }

    @Test
    void validate_failsWhenNoLowercase() {
        List<String> failures = validator.validate("alice", "ABC123!!");
        assertTrue(failures.contains("Password must include at least one lowercase letter."));
    }

    @Test
    void validate_failsWhenNoUppercase() {
        List<String> failures = validator.validate("alice", "abc123!!");
        assertTrue(failures.contains("Password must include at least one uppercase letter."));
    }

    @Test
    void validate_failsWhenNoDigit() {
        List<String> failures = validator.validate("alice", "Abcdef!!");
        assertTrue(failures.contains("Password must include at least one digit."));
    }

    @Test
    void validate_failsWhenNoSpecialCharacter() {
        List<String> failures = validator.validate("alice", "Abcdef12");
        assertTrue(failures.contains("Password must include at least one special character."));
    }

    @Test
    void validate_failsWhenContainsWhitespace() {
        List<String> failures = validator.validate("alice", "Abc12! d");
        assertTrue(failures.contains("Password must not contain whitespace."));
    }

    @Test
    void validate_failsWhenContainsUsernameCaseInsensitive() {
        List<String> failures = validator.validate("Alice", "xxaLIce!1");
        assertTrue(failures.contains("Password must not contain the username."));
    }

    @Test
    void validate_passesForValidPassword() {
        List<String> failures = validator.validate("alice", "Strong!9A");
        assertTrue(failures.isEmpty());
    }
}
