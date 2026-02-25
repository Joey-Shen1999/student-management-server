package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class ManagementTestDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(ManagementTestDataInitializer.class);

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final PasswordEncoder passwordEncoder;

    public ManagementTestDataInitializer(UserRepository userRepository,
                                         StudentRepository studentRepository,
                                         TeacherRepository teacherRepository,
                                         TeacherStudentRepository teacherStudentRepository,
                                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initManagementAccounts() {
        Teacher teacher = ensureTeacher("demo_teacher_active_01", "Teacher!234", "Demo Teacher");
        Teacher adminTeacher = ensureAdmin("demo_admin_active_01", "Admin!234", "Demo Admin");
        Student student = ensureStudent("demo_student_active_01", "Student!234", "Alice", "Zhang", "Ali");
        ensureActiveAssignment(teacher, student);

        log.info(
                "Management demo accounts ready. admin={}, teacher={}, student={}",
                adminTeacher.getUser().getUsername(),
                teacher.getUser().getUsername(),
                student.getUser().getUsername()
        );
    }

    private Teacher ensureTeacher(String username, String rawPassword, String displayName) {
        User user = ensureUser(username, rawPassword, UserRole.TEACHER, UserAccountStatus.ACTIVE);
        return ensureTeacherRecord(user, displayName);
    }

    private Teacher ensureAdmin(String username, String rawPassword, String displayName) {
        User user = ensureUser(username, rawPassword, UserRole.ADMIN, UserAccountStatus.ACTIVE);
        return ensureTeacherRecord(user, displayName);
    }

    private Student ensureStudent(String username,
                                  String rawPassword,
                                  String firstName,
                                  String lastName,
                                  String nickName) {
        User user = ensureUser(username, rawPassword, UserRole.STUDENT, UserAccountStatus.ACTIVE);
        return studentRepository.findByUser_Id(user.getId())
                .orElseGet(() -> studentRepository.save(new Student(user, firstName, lastName, nickName)));
    }

    private User ensureUser(String username,
                            String rawPassword,
                            UserRole role,
                            UserAccountStatus status) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = new User(username, passwordEncoder.encode(rawPassword), role);
            user.updateStatus(status, null);
            return userRepository.save(user);
        }

        if (user.getRole() != role) {
            log.warn("Skipping demo user {} because existing role is {} (expected {}).", username, user.getRole(), role);
            return user;
        }

        if (user.getStatus() != status) {
            user.updateStatus(status, null);
            user = userRepository.save(user);
        }
        return user;
    }

    private Teacher ensureTeacherRecord(User user, String displayName) {
        return teacherRepository.findByUser_Id(user.getId())
                .orElseGet(() -> teacherRepository.save(new Teacher(user, displayName)));
    }

    private void ensureActiveAssignment(Teacher teacher, Student student) {
        boolean exists = teacherStudentRepository.existsByTeacher_IdAndStudent_IdAndStatus(
                teacher.getId(),
                student.getId(),
                TeacherStudentStatus.ACTIVE
        );
        if (!exists) {
            teacherStudentRepository.save(new TeacherStudent(
                    teacher,
                    student,
                    TeacherStudentStatus.ACTIVE,
                    "Demo assignment"
            ));
        }
    }
}
