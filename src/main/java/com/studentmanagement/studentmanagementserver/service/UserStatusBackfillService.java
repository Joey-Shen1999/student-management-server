package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
public class UserStatusBackfillService {

    private final UserRepository userRepository;

    public UserStatusBackfillService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillTeacherAndAdminStatus() {
        List<User> users = userRepository.findByStatusIsNullAndRoleIn(
                Arrays.asList(UserRole.TEACHER, UserRole.ADMIN)
        );
        for (User user : users) {
            user.updateStatus(UserAccountStatus.ACTIVE, null);
        }
        if (!users.isEmpty()) {
            userRepository.saveAll(users);
        }
    }
}
