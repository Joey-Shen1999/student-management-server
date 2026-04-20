package com.studentmanagement.studentmanagementserver.domain.osslt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TeacherOssltModuleUpdateRequestDto {
    private String latestOssltResult;
    private String latestOssltDate;
    private Boolean hasOsslc;
    private String ossltTrackingManualStatus;
    private String osslcCourseStatus;
    private String osslcCourseLocation;
    private boolean latestOssltResultPresent;
    private boolean latestOssltDatePresent;
    private boolean hasOsslcPresent;
    private boolean ossltTrackingManualStatusPresent;
    private boolean osslcCourseStatusPresent;
    private boolean osslcCourseLocationPresent;

    public String getLatestOssltResult() {
        return latestOssltResult;
    }

    @JsonSetter("latestOssltResult")
    public void setLatestOssltResult(String latestOssltResult) {
        this.latestOssltResult = latestOssltResult;
        this.latestOssltResultPresent = true;
    }

    public String getLatestOssltDate() {
        return latestOssltDate;
    }

    @JsonSetter("latestOssltDate")
    public void setLatestOssltDate(String latestOssltDate) {
        this.latestOssltDate = latestOssltDate;
        this.latestOssltDatePresent = true;
    }

    public Boolean getHasOsslc() {
        return hasOsslc;
    }

    @JsonSetter("hasOsslc")
    public void setHasOsslc(Boolean hasOsslc) {
        this.hasOsslc = hasOsslc;
        this.hasOsslcPresent = true;
    }

    public String getOssltTrackingManualStatus() {
        return ossltTrackingManualStatus;
    }

    @JsonSetter("ossltTrackingManualStatus")
    public void setOssltTrackingManualStatus(String ossltTrackingManualStatus) {
        this.ossltTrackingManualStatus = ossltTrackingManualStatus;
        this.ossltTrackingManualStatusPresent = true;
    }

    public String getOsslcCourseStatus() {
        return osslcCourseStatus;
    }

    @JsonSetter("osslcCourseStatus")
    public void setOsslcCourseStatus(String osslcCourseStatus) {
        this.osslcCourseStatus = osslcCourseStatus;
        this.osslcCourseStatusPresent = true;
    }

    public String getOsslcCourseLocation() {
        return osslcCourseLocation;
    }

    @JsonSetter("osslcCourseLocation")
    public void setOsslcCourseLocation(String osslcCourseLocation) {
        this.osslcCourseLocation = osslcCourseLocation;
        this.osslcCourseLocationPresent = true;
    }

    public boolean isLatestOssltResultPresent() {
        return latestOssltResultPresent;
    }

    @JsonIgnore
    public boolean isLatestOssltDatePresent() {
        return latestOssltDatePresent;
    }

    @JsonIgnore
    public boolean isHasOsslcPresent() {
        return hasOsslcPresent;
    }

    @JsonIgnore
    public boolean isOssltTrackingManualStatusPresent() {
        return ossltTrackingManualStatusPresent;
    }

    @JsonIgnore
    public boolean isOsslcCourseStatusPresent() {
        return osslcCourseStatusPresent;
    }

    @JsonIgnore
    public boolean isOsslcCourseLocationPresent() {
        return osslcCourseLocationPresent;
    }
}
