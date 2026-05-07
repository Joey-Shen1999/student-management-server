package com.studentmanagement.studentmanagementserver.domain.serviceprogress;

public class StudentRemarkUpdateRequestDto {
    private String studentRemark;
    private String teacherNote;

    public String getStudentRemark() {
        return studentRemark;
    }

    public void setStudentRemark(String studentRemark) {
        this.studentRemark = studentRemark;
    }

    public String getTeacherNote() {
        return teacherNote;
    }

    public void setTeacherNote(String teacherNote) {
        this.teacherNote = teacherNote;
    }

    public String resolveRemark() {
        return studentRemark != null ? studentRemark : teacherNote;
    }
}
