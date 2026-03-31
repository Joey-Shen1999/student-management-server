package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.domain.user.UserSession;
import com.studentmanagement.studentmanagementserver.repo.UserSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AuthSessionService {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final ThreadLocal<MessageDigest> SHA_256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    });

    private final UserSessionRepository userSessionRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long sessionHours;

    public AuthSessionService(UserSessionRepository userSessionRepository,
                              @Value("${app.auth.session-hours:12}") long sessionHours) {
        this.userSessionRepository = userSessionRepository;
        this.sessionHours = sessionHours;
    }

    @Transactional
    public IssuedSession issueSession(User user) {
        String accessToken = generateToken();
        String tokenHash = sha256Hex(accessToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(sessionHours);

        UserSession session = new UserSession(user, tokenHash, expiresAt);
        userSessionRepository.save(session);

        return new IssuedSession(accessToken, "Bearer", expiresAt.toString());
    }

    @Transactional(readOnly = true)
    public User requireAuthenticatedUser(HttpServletRequest request) {
        return requireActiveSession(request).getUser();
    }

    @Transactional
    public void revokeCurrentSession(HttpServletRequest request) {
        UserSession session = requireActiveSession(request);
        session.revokeNow();
        userSessionRepository.save(session);
    }

    private UserSession requireActiveSession(HttpServletRequest request) {
        String accessToken = resolveBearerToken(request);
        String tokenHash = sha256Hex(accessToken);

        UserSession session = userSessionRepository.findActiveByTokenHash(tokenHash, LocalDateTime.now())
                .orElseThrow(this::unauthenticated);
        return session;
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || authHeader.trim().isEmpty()) {
            throw unauthenticated();
        }
        if (!authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw unauthenticated();
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw unauthenticated();
        }
        return token;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        MessageDigest md = SHA_256.get();
        md.reset();
        byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
        char[] hex = new char[digest.length * 2];
        for (int i = 0; i < digest.length; i++) {
            int unsigned = digest[i] & 0xFF;
            hex[i * 2] = HEX_DIGITS[unsigned >>> 4];
            hex[i * 2 + 1] = HEX_DIGITS[unsigned & 0x0F];
        }
        return new String(hex);
    }

    private ResponseStatusException unauthenticated() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated.");
    }

    public static class IssuedSession {
        private final String accessToken;
        private final String tokenType;
        private final String expiresAt;

        public IssuedSession(String accessToken, String tokenType, String expiresAt) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.expiresAt = expiresAt;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public String getExpiresAt() {
            return expiresAt;
        }
    }
}
