package com.studentmanagement.studentmanagementserver.domain.graduation;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewLessonConfirmationMigrationTest {
    @Test
    void migrationPreservesExistingSchedulesAndDefaultsToUnconfirmed() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:lesson_confirmation_migration;MODE=PostgreSQL");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE interview_course_lessons (id BIGINT PRIMARY KEY, lesson_date DATE, lesson_time TIME)");
            statement.execute("INSERT INTO interview_course_lessons VALUES (1, '2026-10-26', '09:17:00')");
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20261026_01__interview_lesson_confirmation.sql"));
            try (ResultSet row = statement.executeQuery("SELECT * FROM interview_course_lessons WHERE id = 1")) {
                assertTrue(row.next());
                assertEquals("2026-10-26", row.getDate("lesson_date").toString());
                assertEquals("09:17:00", row.getTime("lesson_time").toString());
                assertFalse(row.getBoolean("confirmed"));
                assertFalse(row.wasNull());
            }
            statement.execute("INSERT INTO interview_course_lessons (id) VALUES (2)");
            try (ResultSet row = statement.executeQuery("SELECT confirmed FROM interview_course_lessons WHERE id = 2")) {
                assertTrue(row.next());
                assertFalse(row.getBoolean("confirmed"));
                assertFalse(row.wasNull());
            }
            assertThrows(SQLException.class,
                    () -> statement.execute("UPDATE interview_course_lessons SET confirmed = NULL WHERE id = 1"));
        }
    }
}
