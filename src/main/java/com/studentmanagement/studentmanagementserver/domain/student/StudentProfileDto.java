package com.studentmanagement.studentmanagementserver.domain.student;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentProfileDto {

    private String legalFirstName;
    private String legalLastName;
    private String preferredName;
    private String firstName;
    private String lastName;
    private String nickName;
    private String gender;
    private String birthday;
    private String phone;
    private String email;
    private String statusInCanada;
    private String citizenship;
    private String firstLanguage;
    private String firstBoardingDate;
    private AddressDto address = new AddressDto();
    private String oenNumber;
    private String ib;
    private Boolean ap = Boolean.FALSE;
    private String identityFileNote;

    // Preferred field for school history.
    private List<SchoolDto> schools;
    // Backward-compatible alias for school history.
    private List<SchoolDto> schoolRecords;

    // Preferred field for external courses.
    private List<CourseDto> otherCourses;
    // Backward-compatible alias for external courses.
    private List<CourseDto> externalCourses;

    public String getLegalFirstName() {
        return legalFirstName;
    }

    public void setLegalFirstName(String legalFirstName) {
        this.legalFirstName = legalFirstName;
    }

    public String getLegalLastName() {
        return legalLastName;
    }

    public void setLegalLastName(String legalLastName) {
        this.legalLastName = legalLastName;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(String preferredName) {
        this.preferredName = preferredName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
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

    public String getStatusInCanada() {
        return statusInCanada;
    }

    public void setStatusInCanada(String statusInCanada) {
        this.statusInCanada = statusInCanada;
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

    public String getFirstBoardingDate() {
        return firstBoardingDate;
    }

    public void setFirstBoardingDate(String firstBoardingDate) {
        this.firstBoardingDate = firstBoardingDate;
    }

    public AddressDto getAddress() {
        if (address == null) {
            address = new AddressDto();
        }
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address == null ? new AddressDto() : address;
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

    public Boolean getAp() {
        return ap;
    }

    public void setAp(Boolean ap) {
        this.ap = ap;
    }

    public String getIdentityFileNote() {
        return identityFileNote;
    }

    public void setIdentityFileNote(String identityFileNote) {
        this.identityFileNote = identityFileNote;
    }

    public List<SchoolDto> getSchools() {
        return schools;
    }

    public void setSchools(List<SchoolDto> schools) {
        this.schools = schools;
    }

    public List<SchoolDto> getSchoolRecords() {
        return schoolRecords;
    }

    public void setSchoolRecords(List<SchoolDto> schoolRecords) {
        this.schoolRecords = schoolRecords;
    }

    public List<CourseDto> getOtherCourses() {
        return otherCourses;
    }

    public void setOtherCourses(List<CourseDto> otherCourses) {
        this.otherCourses = otherCourses;
    }

    public List<CourseDto> getExternalCourses() {
        return externalCourses;
    }

    public void setExternalCourses(List<CourseDto> externalCourses) {
        this.externalCourses = externalCourses;
    }

    public List<SchoolDto> getSchoolsOrEmpty() {
        return schools == null ? new ArrayList<SchoolDto>() : schools;
    }

    public List<SchoolDto> getSchoolRecordsOrEmpty() {
        return schoolRecords == null ? new ArrayList<SchoolDto>() : schoolRecords;
    }

    public List<CourseDto> getOtherCoursesOrEmpty() {
        return otherCourses == null ? new ArrayList<CourseDto>() : otherCourses;
    }

    public List<CourseDto> getExternalCoursesOrEmpty() {
        return externalCourses == null ? new ArrayList<CourseDto>() : externalCourses;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddressDto {
        private String streetAddress;
        private String streetAddressLine2;
        private String city;
        private String state;
        private String country;
        private String postal;

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
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SchoolDto {
        private String schoolType;
        private String schoolName;
        private AddressDto address = new AddressDto();
        private String streetAddress;
        private String city;
        private String state;
        private String country;
        private String postal;
        private String startTime;
        private String endTime;

        public String getSchoolType() {
            return schoolType;
        }

        public void setSchoolType(String schoolType) {
            this.schoolType = schoolType;
        }

        public String getSchoolName() {
            return schoolName;
        }

        public void setSchoolName(String schoolName) {
            this.schoolName = schoolName;
        }

        public AddressDto getAddress() {
            if (address == null) {
                address = new AddressDto();
            }
            return address;
        }

        public void setAddress(AddressDto address) {
            this.address = address == null ? new AddressDto() : address;
        }

        public String getStreetAddress() {
            return streetAddress;
        }

        public void setStreetAddress(String streetAddress) {
            this.streetAddress = streetAddress;
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

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CourseDto {
        private String schoolName;
        private AddressDto address = new AddressDto();
        private String streetAddress;
        private String city;
        private String state;
        private String country;
        private String postal;
        private String courseCode;
        private Integer mark;
        private Integer gradeLevel;
        private String startTime;
        private String endTime;

        public String getSchoolName() {
            return schoolName;
        }

        public void setSchoolName(String schoolName) {
            this.schoolName = schoolName;
        }

        public AddressDto getAddress() {
            if (address == null) {
                address = new AddressDto();
            }
            return address;
        }

        public void setAddress(AddressDto address) {
            this.address = address == null ? new AddressDto() : address;
        }

        public String getStreetAddress() {
            return streetAddress;
        }

        public void setStreetAddress(String streetAddress) {
            this.streetAddress = streetAddress;
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

        public String getCourseCode() {
            return courseCode;
        }

        public void setCourseCode(String courseCode) {
            this.courseCode = courseCode;
        }

        public Integer getMark() {
            return mark;
        }

        public void setMark(Integer mark) {
            this.mark = mark;
        }

        public Integer getGradeLevel() {
            return gradeLevel;
        }

        public void setGradeLevel(Integer gradeLevel) {
            this.gradeLevel = gradeLevel;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }
}
