package com.studentmanagement.studentmanagementserver.domain.osslt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentOssltModuleUpdateRequestDto {
    private String latestOssltResult;
    private Boolean hasOsslc;
    private String ossltTrackingManualStatus;
    private boolean latestOssltResultPresent;
    private boolean hasOsslcPresent;
    private boolean ossltTrackingManualStatusPresent;

    public String getLatestOssltResult() {
        return latestOssltResult;
    }

    @JsonSetter("latestOssltResult")
    public void setLatestOssltResult(String latestOssltResult) {
        this.latestOssltResult = latestOssltResult;
        this.latestOssltResultPresent = true;
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

    public boolean isLatestOssltResultPresent() {
        return latestOssltResultPresent;
    }

    @JsonIgnore
    public boolean isHasOsslcPresent() {
        return hasOsslcPresent;
    }

    @JsonIgnore
    public boolean isOssltTrackingManualStatusPresent() {
        return ossltTrackingManualStatusPresent;
    }
}
