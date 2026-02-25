package com.studentmanagement.studentmanagementserver.service;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
public class TeacherBindingBackfillService {

    private static final Logger log = LoggerFactory.getLogger(TeacherBindingBackfillService.class);

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;

    public TeacherBindingBackfillService(UserRepository userRepository, TeacherRepository teacherRepository) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runOnStartup() {
        BackfillResult result = backfillMissingTeacherBindings();
        log.info(
                "Teacher binding backfill completed. beforeMissing={}, inserted={}, afterMissing={}",
                result.getBeforeMissing(),
                result.getInserted(),
                result.getAfterMissing()
        );
    }

    @Transactional
    public BackfillResult backfillMissingTeacherBindings() {
        List<User> managementUsers = userRepository.findByRoleIn(Arrays.asList(UserRole.ADMIN, UserRole.TEACHER));
        int beforeMissing = 0;
        int inserted = 0;

        for (User user : managementUsers) {
            if (!teacherRepository.findByUser_Id(user.getId()).isPresent()) {
                beforeMissing++;
                teacherRepository.save(new Teacher(user, user.getUsername()));
                inserted++;
            }
        }

        int afterMissing = 0;
        for (User user : managementUsers) {
            if (!teacherRepository.findByUser_Id(user.getId()).isPresent()) {
                afterMissing++;
            }
        }

        return new BackfillResult(beforeMissing, inserted, afterMissing);
    }

    public static class BackfillResult {
        private final int beforeMissing;
        private final int inserted;
        private final int afterMissing;

        public BackfillResult(int beforeMissing, int inserted, int afterMissing) {
            this.beforeMissing = beforeMissing;
            this.inserted = inserted;
            this.afterMissing = afterMissing;
        }

        public int getBeforeMissing() {
            return beforeMissing;
        }

        public int getInserted() {
            return inserted;
        }

        public int getAfterMissing() {
            return afterMissing;
        }
    }
}
