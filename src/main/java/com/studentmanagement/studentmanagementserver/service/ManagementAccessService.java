package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

@Service
public class ManagementAccessService {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final UserRepository userRepository;

    public ManagementAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireTeacherManagementAccess(HttpServletRequest request) {
        User operator = authenticate(request);
        if (operator.isMustChangePassword()) {
            throw new MustChangePasswordRequiredException();
        }
        if (operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: admin role required.");
        }
        return operator;
    }

    private User authenticate(HttpServletRequest request) {
        String rawUserId = request.getHeader(USER_ID_HEADER);
        if (rawUserId == null || rawUserId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated.");
        }

        Long userId;
        try {
            userId = Long.parseLong(rawUserId.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated.");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated."));
    }
}
