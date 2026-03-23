package com.studentmanagement.studentmanagementserver.domain.student;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TeacherStudentProfileDto extends StudentProfileDto {

    private String teacherNote;
    private boolean teacherNoteProvided;

    public String getTeacherNote() {
        return teacherNote;
    }

    @JsonSetter("teacherNote")
    public void setTeacherNote(String teacherNote) {
        this.teacherNoteProvided = true;
        this.teacherNote = teacherNote;
    }

    @JsonIgnore
    public boolean isTeacherNoteProvided() {
        return teacherNoteProvided;
    }
}
