package com.studentmanagement.studentmanagementserver.domain.serviceprogress;

import java.util.ArrayList;
import java.util.List;

public class ServiceProgressStateDto {
    private Long studentId;
    private String studentRemark;
    private List<ServiceProgressRecordDto> records = new ArrayList<ServiceProgressRecordDto>();

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentRemark() {
        return studentRemark;
    }

    public void setStudentRemark(String studentRemark) {
        this.studentRemark = studentRemark;
    }

    public List<ServiceProgressRecordDto> getRecords() {
        return records;
    }

    public void setRecords(List<ServiceProgressRecordDto> records) {
        this.records = records == null ? new ArrayList<ServiceProgressRecordDto>() : records;
    }
}
