package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Profile("!test")
public class StudentTestDataInitializer {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentTestDataInitializer(UserRepository userRepository,
                                      StudentRepository studentRepository,
                                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initStudents() {
        ensureStudent("demo_student_active_01", "Student!234", "Alice", "Zhang", "Ali", UserAccountStatus.ACTIVE);
        ensureStudent("demo_student_active_02", "Student!234", "Bob", "Li", "Bobby", UserAccountStatus.ACTIVE);
        ensureStudent("demo_student_archived_01", "Student!234", "Cindy", "Wang", "Cici", UserAccountStatus.ARCHIVED);
    }

    private void ensureStudent(String username,
                               String rawPassword,
                               String firstName,
                               String lastName,
                               String nickName,
                               UserAccountStatus status) {
        Optional<User> existing = userRepository.findByUsername(username);
        User user;
        if (!existing.isPresent()) {
            user = new User(username, passwordEncoder.encode(rawPassword), UserRole.STUDENT);
            user.updateStatus(status, null);
            user = userRepository.save(user);
        } else {
            user = existing.get();
            if (user.getRole() != UserRole.STUDENT) {
                return;
            }
            if (user.getStatus() != status) {
                user.updateStatus(status, null);
                user = userRepository.save(user);
            }
        }

        if (!studentRepository.findByUser_Id(user.getId()).isPresent()) {
            studentRepository.save(new Student(user, firstName, lastName, nickName));
        }
    }
}
