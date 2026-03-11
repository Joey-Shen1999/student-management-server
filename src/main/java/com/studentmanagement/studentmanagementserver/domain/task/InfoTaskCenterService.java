package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.InfoTaskRecipientRepository;
import com.studentmanagement.studentmanagementserver.repo.InfoTaskRepository;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class InfoTaskCenterService {

    private static final int STUDENT_DEFAULT_PAGE = 1;
    private static final int STUDENT_DEFAULT_SIZE = 20;
    private static final int TEACHER_DEFAULT_PAGE = 1;
    private static final int TEACHER_DEFAULT_SIZE = 100;
    private static final int TITLE_MAX_LENGTH = 200;
    private static final int CONTENT_MAX_LENGTH = 4000;
    private static final int TAG_MAX_LENGTH = 50;

    private final AuthSessionService authSessionService;
    private final InfoTaskRepository infoTaskRepository;
    private final InfoTaskRecipientRepository infoTaskRecipientRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;

    public InfoTaskCenterService(AuthSessionService authSessionService,
                                 InfoTaskRepository infoTaskRepository,
                                 InfoTaskRecipientRepository infoTaskRecipientRepository,
                                 StudentRepository studentRepository,
                                 TeacherRepository teacherRepository,
                                 TeacherStudentRepository teacherStudentRepository) {
        this.authSessionService = authSessionService;
        this.infoTaskRepository = infoTaskRepository;
        this.infoTaskRecipientRepository = infoTaskRecipientRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
    }

    @Transactional(readOnly = true)
    public InfoListResponseDto listMyInfos(String categoryRaw,
                                           String tagRaw,
                                           String keywordRaw,
                                           String unreadOnlyRaw,
                                           String pageRaw,
                                           String sizeRaw,
                                           HttpServletRequest request) {
        InfoTaskCategory category = parseCategoryFilter(categoryRaw);
        String tag = normalizeFilterText(tagRaw);
        String keyword = normalizeFilterText(keywordRaw);
        boolean unreadOnly = parseBooleanOrDefault(unreadOnlyRaw, false, "unreadOnly");
        int page = parsePositiveIntOrDefault(pageRaw, STUDENT_DEFAULT_PAGE, "page");
        int size = parsePositiveIntOrDefault(sizeRaw, STUDENT_DEFAULT_SIZE, "size");

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student role required.");
        }
        Student student = studentRepository.findByUser_Id(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found."));

        Page<InfoTaskRecipient> infoPage = infoTaskRecipientRepository.findStudentInfos(
                student.getId(),
                category,
                tag,
                keyword,
                unreadOnly,
                PageRequest.of(page - 1, size)
        );

        List<InfoTaskDto> items = new ArrayList<InfoTaskDto>(infoPage.getContent().size());
        for (InfoTaskRecipient recipient : infoPage.getContent()) {
            items.add(toInfoTaskDto(recipient.getInfoTask(), recipient.isRead(), recipient.getReadAt()));
        }
        return new InfoListResponseDto(items, infoPage.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public InfoListResponseDto listTeacherInfos(String categoryRaw,
                                                String tagRaw,
                                                String keywordRaw,
                                                String pageRaw,
                                                String sizeRaw,
                                                HttpServletRequest request) {
        InfoTaskCategory category = parseCategoryFilter(categoryRaw);
        String tag = normalizeFilterText(tagRaw);
        String keyword = normalizeFilterText(keywordRaw);
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

        Page<InfoTask> infoPage = infoTaskRepository.findTeacherInfos(
                teacherScopeId,
                category,
                tag,
                keyword,
                PageRequest.of(page - 1, size)
        );

        List<InfoTaskDto> items = new ArrayList<InfoTaskDto>(infoPage.getContent().size());
        for (InfoTask infoTask : infoPage.getContent()) {
            items.add(toInfoTaskDto(infoTask, false, null));
        }
        return new InfoListResponseDto(items, infoPage.getTotalElements(), page, size);
    }

    @Transactional
    public InfoTaskDto createInfo(CreateInfoRequestDto requestBody, HttpServletRequest request) {
        if (requestBody == null) {
            throw badRequest("request body is required");
        }

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        String title = requireNonBlank(requestBody.getTitle(), "title", TITLE_MAX_LENGTH);
        String content = requireNonBlank(requestBody.getContent(), "content", CONTENT_MAX_LENGTH);
        InfoTaskCategory category = parseCategoryRequired(requestBody.getCategory());
        List<String> tags = normalizeTags(requestBody.getTags());

        Teacher publisher = resolveTeacherForWrite(operator);
        List<Student> targetStudents = resolveTargetStudentsForInfo(operator, publisher);
        int targetCount = targetStudents.size();

        InfoTask infoTask = new InfoTask(
                title,
                content,
                category,
                joinTags(tags),
                targetCount,
                publisher
        );
        infoTask = infoTaskRepository.save(infoTask);

        if (!targetStudents.isEmpty()) {
            List<InfoTaskRecipient> recipients = new ArrayList<InfoTaskRecipient>(targetStudents.size());
            for (Student student : targetStudents) {
                recipients.add(new InfoTaskRecipient(infoTask, student));
            }
            infoTaskRecipientRepository.saveAll(recipients);
        }

        return toInfoTaskDto(infoTask, false, null);
    }

    @Transactional
    public InfoTaskDto markMyInfoAsRead(Long infoId, HttpServletRequest request) {
        Long normalizedInfoId = requirePositiveId(infoId, "infoId");

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student role required.");
        }
        Student student = studentRepository.findByUser_Id(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found."));

        InfoTaskRecipient recipient = infoTaskRecipientRepository.findByInfoTask_IdAndStudent_Id(normalizedInfoId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Info task not found."));
        recipient.markRead();
        InfoTaskRecipient saved = infoTaskRecipientRepository.save(recipient);
        return toInfoTaskDto(saved.getInfoTask(), saved.isRead(), saved.getReadAt());
    }

    private List<Student> resolveTargetStudentsForInfo(User operator, Teacher publisher) {
        List<Student> sourceStudents;
        if (operator.getRole() == UserRole.ADMIN) {
            sourceStudents = studentRepository.findAllWithUser();
        } else {
            sourceStudents = teacherStudentRepository.findDistinctStudentsByTeacherIdAndStatusWithUser(
                    publisher.getId(),
                    TeacherStudentStatus.ACTIVE
            );
        }
        if (sourceStudents.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Student> deduplicated = new LinkedHashMap<Long, Student>();
        for (Student student : sourceStudents) {
            if (student.getId() != null) {
                deduplicated.put(student.getId(), student);
            }
        }
        return new ArrayList<Student>(deduplicated.values());
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

    private InfoTaskDto toInfoTaskDto(InfoTask infoTask, boolean read, java.time.LocalDateTime readAt) {
        Teacher publisher = infoTask.getPublishedByTeacher();
        return new InfoTaskDto(
                infoTask.getId(),
                "INFO",
                infoTask.getTitle(),
                infoTask.getContent(),
                infoTask.getCategory(),
                parseTags(infoTask.getTagsText()),
                infoTask.getTargetStudentCount(),
                publisher.getId(),
                buildTeacherDisplayName(publisher),
                infoTask.getCreatedAt() == null ? null : infoTask.getCreatedAt().toString(),
                infoTask.getUpdatedAt() == null ? null : infoTask.getUpdatedAt().toString(),
                read,
                readAt == null ? null : readAt.toString()
        );
    }

    private String buildTeacherDisplayName(Teacher teacher) {
        String teacherName = trimToNull(teacher.getName());
        if (teacherName != null) {
            return teacherName;
        }
        if (teacher.getUser() != null) {
            String username = trimToNull(teacher.getUser().getUsername());
            if (username != null) {
                return username;
            }
        }
        return "Teacher #" + teacher.getId();
    }

    private InfoTaskCategory parseCategoryFilter(String raw) {
        String normalized = trimToNull(raw);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        try {
            return InfoTaskCategory.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw badRequest("category invalid");
        }
    }

    private InfoTaskCategory parseCategoryRequired(String raw) {
        String normalized = trimToNull(raw);
        if (normalized == null) {
            throw badRequest("category is required");
        }
        try {
            return InfoTaskCategory.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw badRequest("category invalid");
        }
    }

    private List<String> normalizeTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> deduplicated = new LinkedHashSet<String>();
        for (String rawTag : rawTags) {
            String tag = trimToNull(rawTag);
            if (tag == null) {
                continue;
            }
            if (tag.length() > TAG_MAX_LENGTH) {
                throw badRequest("tag too long");
            }
            deduplicated.add(tag);
        }
        if (deduplicated.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(deduplicated);
    }

    private List<String> parseTags(String tagsText) {
        String normalized = trimToNull(tagsText);
        if (normalized == null) {
            return Collections.emptyList();
        }
        String[] splits = normalized.split(",");
        List<String> tags = new ArrayList<String>(splits.length);
        for (String split : splits) {
            String tag = trimToNull(split);
            if (tag != null) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(tags.get(i));
        }
        return sb.toString();
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

    private boolean parseBooleanOrDefault(String raw, boolean defaultValue, String fieldName) {
        String normalized = trimToNull(raw);
        if (normalized == null) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return false;
        }
        throw badRequest(fieldName + " must be true or false");
    }

    private Long requirePositiveId(Long id, String fieldName) {
        if (id == null || id.longValue() <= 0L) {
            throw badRequest(fieldName + " must be positive");
        }
        return id;
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

    private String normalizeFilterText(String raw) {
        String normalized = trimToNull(raw);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
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
