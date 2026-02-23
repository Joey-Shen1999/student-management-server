package com.studentmanagement.studentmanagementserver.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class TemporaryPasswordGenerator {

    private static final String UPPERCASE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGIT_CHARS = "23456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()-_=+[]{}:;,.?";
    private static final String ALL_CHARS = UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS + SPECIAL_CHARS;
    private static final int TEMP_PASSWORD_LENGTH = 8;

    private final PasswordPolicyValidator passwordPolicyValidator;
    private final SecureRandom random = new SecureRandom();

    public TemporaryPasswordGenerator(PasswordPolicyValidator passwordPolicyValidator) {
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    public String generate(String username) {
        for (int attempt = 0; attempt < 100; attempt++) {
            String candidate = buildRandomPassword();
            if (passwordPolicyValidator.validate(username, candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate a policy-compliant temporary password.");
    }

    private String buildRandomPassword() {
        List<Character> chars = new ArrayList<Character>();
        chars.add(randomChar(UPPERCASE_CHARS));
        chars.add(randomChar(LOWERCASE_CHARS));
        chars.add(randomChar(DIGIT_CHARS));
        chars.add(randomChar(SPECIAL_CHARS));

        while (chars.size() < TEMP_PASSWORD_LENGTH) {
            chars.add(randomChar(ALL_CHARS));
        }

        Collections.shuffle(chars, random);

        StringBuilder sb = new StringBuilder(chars.size());
        for (Character c : chars) {
            sb.append(c.charValue());
        }
        return sb.toString();
    }

    private char randomChar(String source) {
        return source.charAt(random.nextInt(source.length()));
    }
}
