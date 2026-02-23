package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.api.dto.RegisterRequest;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AuthServicePasswordPolicyTest {

    @Autowired
    AuthService authService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @ParameterizedTest
    @MethodSource("invalidPasswordCases")
    void register_rejectsEachPasswordRule(String username, String password, String expectedDetail) {
        RegisterRequest req = studentRegister(username, password);

        PasswordPolicyViolationException ex = assertThrows(
                PasswordPolicyViolationException.class,
                () -> authService.register(req)
        );

        assertTrue(ex.getDetails().contains(expectedDetail));
    }

    @Test
    void register_acceptsValidPassword() {
        RegisterRequest req = studentRegister("alice_valid", "Strong!9A");
        assertNotNull(authService.register(req).getUserId());
    }

    @Test
    void setPassword_rejectsUsernameContainment() {
        User user = userRepository.save(new User("teacherUser", passwordEncoder.encode("Valid!9A"), UserRole.TEACHER));

        PasswordPolicyViolationException ex = assertThrows(
                PasswordPolicyViolationException.class,
                () -> authService.setPassword(user.getId(), "xTeacherUser!1")
        );

        assertTrue(ex.getDetails().contains("Password must not contain the username."));
    }

    @Test
    void setPassword_acceptsValidPasswordAndClearsMustChangeFlag() {
        User user = userRepository.save(new User("teacher_ok", passwordEncoder.encode("Valid!9A"), UserRole.TEACHER));
        user.setMustChangePassword(true);
        user = userRepository.save(user);

        authService.setPassword(user.getId(), "Another!9A");

        User updated = userRepository.findById(user.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        assertFalse(updated.isMustChangePassword());
        assertTrue(passwordEncoder.matches("Another!9A", updated.getPasswordHash()));
    }

    private static Stream<Arguments> invalidPasswordCases() {
        return Stream.of(
                Arguments.of("rule_len", "Aa1!a", "Password must be at least 8 characters."),
                Arguments.of("rule_nolower", "ABC123!!", "Password must include at least one lowercase letter."),
                Arguments.of("rule_noupper", "abc123!!", "Password must include at least one uppercase letter."),
                Arguments.of("rule_nodigit", "Abcdef!!", "Password must include at least one digit."),
                Arguments.of("rule_nospecial", "Abcdef12", "Password must include at least one special character."),
                Arguments.of("rule_space", "Abc12! d", "Password must not contain whitespace."),
                Arguments.of("alice", "AAalice!1", "Password must not contain the username.")
        );
    }

    private RegisterRequest studentRegister(String username, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword(password);
        req.setRole(UserRole.STUDENT);
        req.setFirstName("Joey");
        req.setLastName("Shen");
        req.setPreferredName("JS");
        return req;
    }
}
