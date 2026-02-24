package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

@Service
public class ManagementAccessService {

    private final AuthSessionService authSessionService;

    public ManagementAccessService(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    public User requireTeacherManagementAccess(HttpServletRequest request) {
        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.isMustChangePassword()) {
            throw new MustChangePasswordRequiredException();
        }
        if (operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: admin role required.");
        }
        return operator;
    }
}
