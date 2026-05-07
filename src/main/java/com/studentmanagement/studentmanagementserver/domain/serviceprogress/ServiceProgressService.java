package com.studentmanagement.studentmanagementserver.domain.serviceprogress;

import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentProfile;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.repo.ServiceProgressRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceProgressService {

    private static final int MAX_TEXT_LENGTH = 5000;

    private final ServiceProgressRecordRepository serviceProgressRecordRepository;
    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherRepository teacherRepository;

    public ServiceProgressService(ServiceProgressRecordRepository serviceProgressRecordRepository,
                                  StudentRepository studentRepository,
                                  StudentProfileRepository studentProfileRepository,
                                  TeacherRepository teacherRepository) {
        this.serviceProgressRecordRepository = serviceProgressRecordRepository;
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherRepository = teacherRepository;
    }

    @Transactional(readOnly = true)
    public ServiceProgressStateDto getStudentServiceProgress(Long studentId) {
        Student student = requireStudent(studentId);
        return buildState(student);
    }

    @Transactional
    public ServiceProgressRecordDto createRecord(Long studentId, ServiceProgressRecordRequestDto request) {
        Student student = requireStudent(studentId);
        Teacher advisor = requireAdvisor(request == null ? null : request.getAdvisorId());
        ServiceProgressRecord record = new ServiceProgressRecord(
                student,
                requireAppointmentTime(request == null ? null : request.getAppointmentTime()),
                advisor
        );
        applyRecordRequest(record, request, advisor);
        return toDto(serviceProgressRecordRepository.save(record));
    }

    @Transactional
    public ServiceProgressRecordDto updateRecord(Long recordId, ServiceProgressRecordRequestDto request) {
        ServiceProgressRecord record = requireRecord(recordId);
        Teacher advisor = requireAdvisor(request == null ? null : request.getAdvisorId());
        record.setAppointmentTime(requireAppointmentTime(request == null ? null : request.getAppointmentTime()));
        applyRecordRequest(record, request, advisor);
        return toDto(serviceProgressRecordRepository.save(record));
    }

    @Transactional
    public void deleteRecord(Long recordId) {
        ServiceProgressRecord record = requireRecord(recordId);
        serviceProgressRecordRepository.delete(record);
    }

    @Transactional
    public ServiceProgressStateDto updateStudentRemark(Long studentId, StudentRemarkUpdateRequestDto request) {
        Student student = requireStudent(studentId);
        StudentProfile profile = studentProfileRepository.findByStudent_Id(student.getId())
                .orElseGet(() -> new StudentProfile(student));
        profile.setTeacherNote(normalizeText(request == null ? null : request.resolveRemark()));
        studentProfileRepository.save(profile);
        return buildState(student);
    }

    private ServiceProgressStateDto buildState(Student student) {
        StudentProfile profile = studentProfileRepository.findByStudent_Id(student.getId()).orElse(null);
        List<ServiceProgressRecord> records =
                serviceProgressRecordRepository.findByStudent_IdOrderByAppointmentTimeDescCreatedAtDescIdDesc(student.getId());
        List<ServiceProgressRecordDto> items = new ArrayList<ServiceProgressRecordDto>(records.size());
        for (ServiceProgressRecord record : records) {
            items.add(toDto(record));
        }

        ServiceProgressStateDto state = new ServiceProgressStateDto();
        state.setStudentId(student.getId());
        state.setStudentRemark(profile == null ? null : profile.getTeacherNote());
        state.setRecords(items);
        return state;
    }

    private void applyRecordRequest(ServiceProgressRecord record,
                                    ServiceProgressRecordRequestDto request,
                                    Teacher advisor) {
        record.setAdvisor(advisor);
        record.setFollowUpContent(normalizeText(request == null ? null : request.getFollowUpContent()));
        record.setNextPlan(normalizeText(request == null ? null : request.getNextPlan()));
    }

    private Student requireStudent(Long studentId) {
        if (studentId == null || studentId.longValue() <= 0L) {
            throw new IllegalArgumentException("studentId must be positive");
        }
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));
    }

    private ServiceProgressRecord requireRecord(Long recordId) {
        if (recordId == null || recordId.longValue() <= 0L) {
            throw new IllegalArgumentException("recordId must be positive");
        }
        return serviceProgressRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service progress record not found: " + recordId));
    }

    private Teacher requireAdvisor(Long advisorId) {
        if (advisorId == null || advisorId.longValue() <= 0L) {
            throw new IllegalArgumentException("advisorId is required");
        }
        Teacher advisor = teacherRepository.findById(advisorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Advisor not found: " + advisorId));
        if (!advisor.isAdvisorEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected teacher is not enabled as an advisor.");
        }
        return advisor;
    }

    private LocalDateTime requireAppointmentTime(LocalDateTime appointmentTime) {
        if (appointmentTime == null) {
            throw new IllegalArgumentException("appointmentTime is required");
        }
        return appointmentTime;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Text fields must be at most " + MAX_TEXT_LENGTH + " characters");
        }
        return normalized;
    }

    private ServiceProgressRecordDto toDto(ServiceProgressRecord record) {
        ServiceProgressRecordDto dto = new ServiceProgressRecordDto();
        dto.setId(record.getId());
        dto.setStudentId(record.getStudent() == null ? null : record.getStudent().getId());
        dto.setAppointmentTime(record.getAppointmentTime());
        dto.setAdvisorId(record.getAdvisor() == null ? null : record.getAdvisor().getId());
        dto.setAdvisorName(record.getAdvisor() == null ? null : record.getAdvisor().getName());
        dto.setFollowUpContent(record.getFollowUpContent());
        dto.setNextPlan(record.getNextPlan());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        return dto;
    }
}
