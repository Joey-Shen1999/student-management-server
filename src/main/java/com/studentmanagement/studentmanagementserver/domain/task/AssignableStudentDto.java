package com.studentmanagement.studentmanagementserver.domain.task;

public class AssignableStudentDto {
    private Long studentId;
    private String studentName;
    private String username;
    private String email;
    private String phone;
    private String graduation;
    private String schoolName;
    private String canadaIdentity;
    private String gender;
    private String nationality;
    private String firstLanguage;
    private String schoolBoard;
    private String country;
    private String province;
    private String city;
    private String teacherNote;
    private AssignableStudentStatus status;
    private boolean selectable;

    public AssignableStudentDto(Long studentId,
                                String studentName,
                                String username,
                                String email,
                                String phone,
                                String graduation,
                                String schoolName,
                                String canadaIdentity,
                                String gender,
                                String nationality,
                                String firstLanguage,
                                String schoolBoard,
                                String country,
                                String province,
                                String city,
                                String teacherNote,
                                AssignableStudentStatus status,
                                boolean selectable) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.graduation = graduation;
        this.schoolName = schoolName;
        this.canadaIdentity = canadaIdentity;
        this.gender = gender;
        this.nationality = nationality;
        this.firstLanguage = firstLanguage;
        this.schoolBoard = schoolBoard;
        this.country = country;
        this.province = province;
        this.city = city;
        this.teacherNote = teacherNote;
        this.status = status;
        this.selectable = selectable;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getGraduation() {
        return graduation;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public String getCanadaIdentity() {
        return canadaIdentity;
    }

    public String getGender() {
        return gender;
    }

    public String getNationality() {
        return nationality;
    }

    public String getFirstLanguage() {
        return firstLanguage;
    }

    public String getSchoolBoard() {
        return schoolBoard;
    }

    public String getCountry() {
        return country;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public String getTeacherNote() {
        return teacherNote;
    }

    public AssignableStudentStatus getStatus() {
        return status;
    }

    public boolean isSelectable() {
        return selectable;
    }
}
