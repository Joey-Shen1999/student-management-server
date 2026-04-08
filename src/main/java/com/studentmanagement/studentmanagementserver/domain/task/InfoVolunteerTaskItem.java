package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "info_volunteer_task_items",
        indexes = {
                @Index(name = "idx_info_volunteer_task_items_info_id", columnList = "info_id")
        }
)
public class InfoVolunteerTaskItem extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "info_id", nullable = false)
    private InfoTask infoTask;

    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "duration_hours", nullable = false, precision = 12, scale = 2)
    private BigDecimal durationHours;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "verifier_contact", nullable = false, length = 255)
    private String verifierContact;

    protected InfoVolunteerTaskItem() {
    }

    public InfoVolunteerTaskItem(InfoTask infoTask,
                                 String taskName,
                                 String description,
                                 BigDecimal durationHours,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 String verifierContact) {
        this.infoTask = infoTask;
        this.taskName = taskName;
        this.description = description;
        this.durationHours = durationHours;
        this.startDate = startDate;
        this.endDate = endDate;
        this.verifierContact = verifierContact;
    }

    public InfoTask getInfoTask() {
        return infoTask;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getDurationHours() {
        return durationHours;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getVerifierContact() {
        return verifierContact;
    }
}
