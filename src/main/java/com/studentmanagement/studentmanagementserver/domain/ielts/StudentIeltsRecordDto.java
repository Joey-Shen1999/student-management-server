package com.studentmanagement.studentmanagementserver.domain.ielts;

public class StudentIeltsRecordDto {
    private String recordId;
    private String testDate;
    private Double listening;
    private Double reading;
    private Double writing;
    private Double speaking;

    public StudentIeltsRecordDto() {
    }

    public StudentIeltsRecordDto(String recordId,
                                 String testDate,
                                 Double listening,
                                 Double reading,
                                 Double writing,
                                 Double speaking) {
        this.recordId = recordId;
        this.testDate = testDate;
        this.listening = listening;
        this.reading = reading;
        this.writing = writing;
        this.speaking = speaking;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getTestDate() {
        return testDate;
    }

    public void setTestDate(String testDate) {
        this.testDate = testDate;
    }

    public Double getListening() {
        return listening;
    }

    public void setListening(Double listening) {
        this.listening = listening;
    }

    public Double getReading() {
        return reading;
    }

    public void setReading(Double reading) {
        this.reading = reading;
    }

    public Double getWriting() {
        return writing;
    }

    public void setWriting(Double writing) {
        this.writing = writing;
    }

    public Double getSpeaking() {
        return speaking;
    }

    public void setSpeaking(Double speaking) {
        this.speaking = speaking;
    }
}
