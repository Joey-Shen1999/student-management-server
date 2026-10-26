package com.studentmanagement.studentmanagementserver.domain.graduation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterviewCourseAcademicYearTest {
    @ParameterizedTest
    @CsvSource({
            "2026-08-31, 2026",
            "2026-09-01, 2027",
            "2026-10-26, 2027",
            "2027-01-01, 2027",
            "2027-06-30, 2027",
            "2027-08-31, 2027",
            "2027-09-01, 2028"
    })
    void selectsGraduationCohortAcrossAcademicYearBoundaries(String date, int cohort) {
        assertEquals(cohort, InterviewCourseService.currentGraduationYear(LocalDate.parse(date)));
    }
}
