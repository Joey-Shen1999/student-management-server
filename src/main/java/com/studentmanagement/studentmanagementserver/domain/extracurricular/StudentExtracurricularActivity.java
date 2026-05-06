package com.studentmanagement.studentmanagementserver.domain.extracurricular;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(
        name = "student_extracurricular_activity",
        indexes = {
                @Index(name = "idx_student_extracurricular_activity_tracking_id", columnList = "tracking_id"),
                @Index(name = "idx_student_extracurricular_activity_type", columnList = "activity_type"),
                @Index(name = "idx_student_extracurricular_activity_date", columnList = "activity_date,start_date,end_date")
        }
)
public class StudentExtracurricularActivity extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tracking_id", nullable = false)
    private StudentExtracurricularTracking tracking;

    @Column(name = "activity_type", nullable = false, length = 40)
    private String activityType;

    @Column(name = "activity_name", nullable = false, length = 200)
    private String activityName;

    @Column(name = "organization", length = 200)
    private String organization;

    @Column(name = "activity_role", length = 120)
    private String role;

    @Column(name = "activity_level", length = 40)
    private String activityLevel;

    @Column(name = "award_or_result", length = 255)
    private String awardOrResult;

    @Column(name = "competition_category", length = 120)
    private String competitionCategory;

    @Column(name = "activity_date")
    private LocalDate activityDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "admission_relevance", length = 2000)
    private String admissionRelevance;

    @Column(name = "proof_contact", length = 255)
    private String proofContact;

    @Column(name = "proof_url", length = 500)
    private String proofUrl;

    protected StudentExtracurricularActivity() {
    }

    public StudentExtracurricularActivity(StudentExtracurricularTracking tracking,
                                          String activityType,
                                          String activityName,
                                          String organization,
                                          String role,
                                          String activityLevel,
                                          String awardOrResult,
                                          String competitionCategory,
                                          LocalDate activityDate,
                                          LocalDate startDate,
                                          LocalDate endDate,
                                          String description,
                                          String admissionRelevance,
                                          String proofContact,
                                          String proofUrl) {
        this.tracking = tracking;
        this.activityType = activityType;
        this.activityName = activityName;
        this.organization = organization;
        this.role = role;
        this.activityLevel = activityLevel;
        this.awardOrResult = awardOrResult;
        this.competitionCategory = competitionCategory;
        this.activityDate = activityDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.admissionRelevance = admissionRelevance;
        this.proofContact = proofContact;
        this.proofUrl = proofUrl;
    }

    public StudentExtracurricularTracking getTracking() {
        return tracking;
    }

    public String getActivityType() {
        return activityType;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getOrganization() {
        return organization;
    }

    public String getRole() {
        return role;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public String getAwardOrResult() {
        return awardOrResult;
    }

    public String getCompetitionCategory() {
        return competitionCategory;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDescription() {
        return description;
    }

    public String getAdmissionRelevance() {
        return admissionRelevance;
    }

    public String getProofContact() {
        return proofContact;
    }

    public String getProofUrl() {
        return proofUrl;
    }
}
