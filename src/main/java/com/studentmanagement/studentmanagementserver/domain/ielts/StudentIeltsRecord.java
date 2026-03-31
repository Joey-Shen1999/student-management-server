package com.studentmanagement.studentmanagementserver.domain.ielts;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
        name = "student_ielts_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_ielts_record_module_record_id",
                        columnNames = {"ielts_module_id", "record_id"}
                )
        },
        indexes = {
                @Index(name = "idx_student_ielts_record_module_id", columnList = "ielts_module_id"),
                @Index(name = "idx_student_ielts_record_module_test_date", columnList = "ielts_module_id,test_date")
        }
)
public class StudentIeltsRecord extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ielts_module_id", nullable = false)
    private StudentIeltsModule ieltsModule;

    @Column(name = "record_id", nullable = false, length = 64)
    private String recordId;

    @Column(name = "test_date", nullable = false)
    private LocalDate testDate;

    @Column(nullable = false)
    private double listening;

    @Column(nullable = false)
    private double reading;

    @Column(nullable = false)
    private double writing;

    @Column(nullable = false)
    private double speaking;

    protected StudentIeltsRecord() {
    }

    public StudentIeltsRecord(StudentIeltsModule ieltsModule,
                              String recordId,
                              LocalDate testDate,
                              double listening,
                              double reading,
                              double writing,
                              double speaking) {
        this.ieltsModule = ieltsModule;
        this.recordId = recordId;
        this.testDate = testDate;
        this.listening = listening;
        this.reading = reading;
        this.writing = writing;
        this.speaking = speaking;
    }

    public StudentIeltsModule getIeltsModule() {
        return ieltsModule;
    }

    public String getRecordId() {
        return recordId;
    }

    public LocalDate getTestDate() {
        return testDate;
    }

    public double getListening() {
        return listening;
    }

    public double getReading() {
        return reading;
    }

    public double getWriting() {
        return writing;
    }

    public double getSpeaking() {
        return speaking;
    }

    public void overwrite(LocalDate testDate,
                          double listening,
                          double reading,
                          double writing,
                          double speaking) {
        this.testDate = testDate;
        this.listening = listening;
        this.reading = reading;
        this.writing = writing;
        this.speaking = speaking;
    }
}
