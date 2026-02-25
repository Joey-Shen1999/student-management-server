package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
        name = "student_profile",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_profile_student_id", columnNames = "student_id"),
        indexes = @Index(name = "idx_student_profile_student_id", columnList = "student_id")
)
public class StudentProfile extends BaseEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @Column(length = 40)
    private String gender;

    private LocalDate birthday;

    @Column(name = "status_in_canada", length = 80)
    private String statusInCanada;

    @Column(length = 40)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 80)
    private String citizenship;

    @Column(name = "first_language", length = 80)
    private String firstLanguage;

    @Column(name = "first_boarding_date")
    private LocalDate firstBoardingDate;

    @Column(name = "oen_number", length = 80)
    private String oenNumber;

    @Column(length = 80)
    private String ib;

    @Column(nullable = false)
    private boolean ap;

    @Column(name = "identity_file_note", length = 500)
    private String identityFileNote;

    @Column(name = "street_address", length = 200)
    private String streetAddress;

    @Column(name = "street_address_line2", length = 200)
    private String streetAddressLine2;

    @Column(length = 120)
    private String city;

    @Column(length = 120)
    private String state;

    @Column(length = 120)
    private String country;

    @Column(length = 40)
    private String postal;

    @Column(name = "updated_by")
    private Long updatedBy;

    protected StudentProfile() {
    }

    public StudentProfile(Student student) {
        this.student = student;
        this.ap = false;
    }

    public Student getStudent() {
        return student;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getStatusInCanada() {
        return statusInCanada;
    }

    public void setStatusInCanada(String statusInCanada) {
        this.statusInCanada = statusInCanada;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCitizenship() {
        return citizenship;
    }

    public void setCitizenship(String citizenship) {
        this.citizenship = citizenship;
    }

    public String getFirstLanguage() {
        return firstLanguage;
    }

    public void setFirstLanguage(String firstLanguage) {
        this.firstLanguage = firstLanguage;
    }

    public LocalDate getFirstBoardingDate() {
        return firstBoardingDate;
    }

    public void setFirstBoardingDate(LocalDate firstBoardingDate) {
        this.firstBoardingDate = firstBoardingDate;
    }

    public String getOenNumber() {
        return oenNumber;
    }

    public void setOenNumber(String oenNumber) {
        this.oenNumber = oenNumber;
    }

    public String getIb() {
        return ib;
    }

    public void setIb(String ib) {
        this.ib = ib;
    }

    public boolean isAp() {
        return ap;
    }

    public void setAp(boolean ap) {
        this.ap = ap;
    }

    public String getIdentityFileNote() {
        return identityFileNote;
    }

    public void setIdentityFileNote(String identityFileNote) {
        this.identityFileNote = identityFileNote;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getStreetAddressLine2() {
        return streetAddressLine2;
    }

    public void setStreetAddressLine2(String streetAddressLine2) {
        this.streetAddressLine2 = streetAddressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostal() {
        return postal;
    }

    public void setPostal(String postal) {
        this.postal = postal;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
