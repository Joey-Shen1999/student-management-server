package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.DllTaskRepository;
import com.studentmanagement.studentmanagementserver.repo.DllTemplateRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.service.ApiRequestException;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.TeacherBindingRequiredException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DllTaskCenterService {

    private static final int TEACHER_DEFAULT_PAGE = 1;
    private static final int TEACHER_DEFAULT_SIZE = 100;
    private static final int NAME_MAX_LENGTH = 200;
    private static final int DESCRIPTION_MAX_LENGTH = 2000;
    private static final int PAYLOAD_SCHEMA_MAX_LENGTH = 8000;

    private final AuthSessionService authSessionService;
    private final DllTemplateRepository dllTemplateRepository;
    private final DllTaskRepository dllTaskRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;

    public DllTaskCenterService(AuthSessionService authSessionService,
                                DllTemplateRepository dllTemplateRepository,
                                DllTaskRepository dllTaskRepository,
                                StudentRepository studentRepository,
                                TeacherRepository teacherRepository,
                                TeacherStudentRepository teacherStudentRepository) {
        this.authSessionService = authSessionService;
        this.dllTemplateRepository = dllTemplateRepository;
        this.dllTaskRepository = dllTaskRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
    }

    @Transactional(readOnly = true)
    public DllTaskListResponseDto listTeacherDllTasks(String pageRaw,
                                                      String sizeRaw,
                                                      HttpServletRequest request) {
        int page = parsePositiveIntOrDefault(pageRaw, TEACHER_DEFAULT_PAGE, "page");
        int size = parsePositiveIntOrDefault(sizeRaw, TEACHER_DEFAULT_SIZE, "size");

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        Long teacherScopeId = null;
        if (operator.getRole() == UserRole.TEACHER) {
            teacherScopeId = requireTeacherByUser(operator).getId();
        }

        Page<DllTask> dllTaskPage = dllTaskRepository.findTeacherDllTasks(teacherScopeId, PageRequest.of(page - 1, size));
        List<DllTaskDto> items = new ArrayList<DllTaskDto>(dllTaskPage.getContent().size());
        for (DllTask dllTask : dllTaskPage.getContent()) {
            items.add(toDllTaskDto(dllTask));
        }
        return new DllTaskListResponseDto(items, dllTaskPage.getTotalElements(), page, size);
    }

    @Transactional
    public DllTemplateDto createDllTemplate(CreateDllTemplateRequestDto requestBody, HttpServletRequest request) {
        if (requestBody == null) {
            throw badRequest("request body is required");
        }

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        String name = requireNonBlank(requestBody.getName(), "name", NAME_MAX_LENGTH);
        String description = normalizeTextWithMax(requestBody.getDescription(), DESCRIPTION_MAX_LENGTH);
        String payloadSchema = normalizeTextWithMax(requestBody.getPayloadSchema(), PAYLOAD_SCHEMA_MAX_LENGTH);

        if (description == null) {
            description = "";
        }
        if (payloadSchema == null) {
            payloadSchema = "{}";
        }

        Teacher creator = resolveTeacherForWrite(operator);
        DllTemplate saved = dllTemplateRepository.save(new DllTemplate(name, description, payloadSchema, creator));
        return toDllTemplateDto(saved);
    }

    @Transactional
    public DllTaskDto instantiateTemplate(Long templateId,
                                          InstantiateDllTemplateRequestDto requestBody,
                                          HttpServletRequest request) {
        Long normalizedTemplateId = requirePositiveId(templateId, "templateId");
        if (requestBody == null) {
            throw badRequest("request body is required");
        }
        Long assignedStudentId = requirePositiveId(requestBody.getAssignedStudentId(), "assignedStudentId");

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        Teacher operatorTeacher = resolveTeacherForWrite(operator);
        DllTemplate template = dllTemplateRepository.findByIdWithCreator(normalizedTemplateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DLL template not found."));

        if (operator.getRole() == UserRole.TEACHER) {
            if (!template.getCreatedByTeacher().getId().equals(operatorTeacher.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: cannot instantiate other teacher template.");
            }
            ensureTeacherCanAssignStudent(operatorTeacher, assignedStudentId);
        }

        Student assignedStudent = studentRepository.findById(assignedStudentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + assignedStudentId));
        String title = trimToNull(requestBody.getTitle());
        if (title == null) {
            title = template.getName();
        }
        if (title.length() > NAME_MAX_LENGTH) {
            throw badRequest("title too long");
        }
        DllTaskStatus status = parseDllTaskStatusOrDefault(requestBody.getStatus(), DllTaskStatus.NOT_STARTED);

        DllTask saved = dllTaskRepository.save(new DllTask(
                template,
                title,
                status,
                assignedStudent,
                operatorTeacher
        ));
        return toDllTaskDto(saved);
    }

    private DllTaskDto toDllTaskDto(DllTask task) {
        return new DllTaskDto(
                task.getId(),
                task.getTemplate().getId(),
                task.getTitle(),
                task.getStatus(),
                task.getAssignedStudent().getId(),
                task.getCreatedAt() == null ? null : task.getCreatedAt().toString()
        );
    }

    private DllTemplateDto toDllTemplateDto(DllTemplate template) {
        return new DllTemplateDto(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getPayloadSchema(),
                template.getCreatedAt() == null ? null : template.getCreatedAt().toString(),
                template.getUpdatedAt() == null ? null : template.getUpdatedAt().toString()
        );
    }

    private DllTaskStatus parseDllTaskStatusOrDefault(String rawStatus, DllTaskStatus defaultStatus) {
        String normalized = trimToNull(rawStatus);
        if (normalized == null) {
            return defaultStatus;
        }
        try {
            return DllTaskStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw badRequest("status invalid");
        }
    }

    private Teacher resolveTeacherForWrite(User operator) {
        Teacher teacher = teacherRepository.findByUser_Id(operator.getId()).orElse(null);
        if (teacher != null) {
            return teacher;
        }
        if (operator.getRole() == UserRole.TEACHER) {
            throw new TeacherBindingRequiredException();
        }
        String fallbackName = trimToNull(operator.getUsername());
        if (fallbackName == null) {
            fallbackName = "Admin #" + operator.getId();
        }
        return teacherRepository.save(new Teacher(operator, fallbackName));
    }

    private Teacher requireTeacherByUser(User operator) {
        return teacherRepository.findByUser_Id(operator.getId())
                .orElseThrow(TeacherBindingRequiredException::new);
    }

    private void ensureTeacherCanAssignStudent(Teacher teacher, Long studentId) {
        boolean hasActiveRelation = teacherStudentRepository.existsByTeacher_IdAndStudent_IdAndStatus(
                teacher.getId(),
                studentId,
                TeacherStudentStatus.ACTIVE
        );
        if (!hasActiveRelation) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Forbidden: student is not actively assigned to this teacher."
            );
        }
    }

    private String requireNonBlank(String value, String fieldName, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw badRequest(fieldName + " is required");
        }
        if (normalized.length() > maxLength) {
            throw badRequest(fieldName + " too long");
        }
        return normalized;
    }

    private String normalizeTextWithMax(String value, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw badRequest("text too long");
        }
        return normalized;
    }

    private int parsePositiveIntOrDefault(String raw, int defaultValue, String fieldName) {
        String normalized = trimToNull(raw);
        if (normalized == null) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(normalized);
            if (value <= 0) {
                throw badRequest(fieldName + " must be a positive integer");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw badRequest(fieldName + " must be a positive integer");
        }
    }

    private Long requirePositiveId(Long id, String fieldName) {
        if (id == null || id.longValue() <= 0L) {
            throw badRequest(fieldName + " must be positive");
        }
        return id;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ApiRequestException badRequest(String message) {
        return new ApiRequestException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
