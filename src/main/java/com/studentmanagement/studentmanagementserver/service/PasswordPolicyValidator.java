package com.studentmanagement.studentmanagementserver.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PasswordPolicyValidator {

    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile(".*[^A-Za-z0-9].*");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile(".*\\s+.*");

    public void validateOrThrow(String username, String password) {
        List<String> failures = validate(username, password);
        if (!failures.isEmpty()) {
            throw new PasswordPolicyViolationException(failures);
        }
    }

    public List<String> validate(String username, String password) {
        List<String> failures = new ArrayList<String>();
        String normalizedUsername = safeTrim(username);
        String rawPassword = password == null ? "" : password;

        if (rawPassword.length() < 8) {
            failures.add("Password must be at least 8 characters.");
        }
        if (!LOWERCASE_PATTERN.matcher(rawPassword).matches()) {
            failures.add("Password must include at least one lowercase letter.");
        }
        if (!UPPERCASE_PATTERN.matcher(rawPassword).matches()) {
            failures.add("Password must include at least one uppercase letter.");
        }
        if (!DIGIT_PATTERN.matcher(rawPassword).matches()) {
            failures.add("Password must include at least one digit.");
        }
        if (!SPECIAL_PATTERN.matcher(rawPassword).matches()) {
            failures.add("Password must include at least one special character.");
        }
        if (WHITESPACE_PATTERN.matcher(rawPassword).matches()) {
            failures.add("Password must not contain whitespace.");
        }

        if (normalizedUsername.length() >= 3
                && !rawPassword.isEmpty()
                && rawPassword.toLowerCase().contains(normalizedUsername.toLowerCase())) {
            failures.add("Password must not contain the username.");
        }

        return failures;
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
