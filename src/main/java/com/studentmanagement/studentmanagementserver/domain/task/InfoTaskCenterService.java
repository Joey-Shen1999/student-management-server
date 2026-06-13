package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.notification.EmailService;
import com.studentmanagement.studentmanagementserver.domain.notification.EmailService.EmailAttachment;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentProfile;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.domain.task.InfoTaskAttachmentStorageService.StoredAttachment;
import com.studentmanagement.studentmanagementserver.repo.InfoTaskRecipientRepository;
import com.studentmanagement.studentmanagementserver.repo.InfoTaskRepository;
import com.studentmanagement.studentmanagementserver.repo.InfoTaskAttachmentRepository;
import com.studentmanagement.studentmanagementserver.repo.InfoVolunteerTaskItemRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.service.ApiRequestException;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.TeacherBindingRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class InfoTaskCenterService {

    private static final Logger log = LoggerFactory.getLogger(InfoTaskCenterService.class);

    private static final int STUDENT_DEFAULT_PAGE = 1;
    private static final int STUDENT_DEFAULT_SIZE = 20;
    private static final int TEACHER_DEFAULT_PAGE = 1;
    private static final int TEACHER_DEFAULT_SIZE = 100;
    private static final int TITLE_MAX_LENGTH = 200;
    private static final int CONTENT_MAX_LENGTH = 4000;
    private static final int TAG_MAX_LENGTH = 50;
    private static final int TASK_GROUP_ID_MAX_LENGTH = 64;
    private static final int VOLUNTEER_TASK_NAME_MAX_LENGTH = 200;
    private static final int VOLUNTEER_TASK_DESCRIPTION_MAX_LENGTH = 2000;
    private static final int VOLUNTEER_VERIFIER_CONTACT_MAX_LENGTH = 255;
    private static final long ATTACHMENT_MAX_UPLOAD_SIZE_BYTES = 30L * 1024L * 1024L;
    private static final long ATTACHMENTS_MAX_TOTAL_SIZE_BYTES = 100L * 1024L * 1024L;
    private static final int EMAIL_CONTENT_PREVIEW_MAX_LENGTH = 800;
    private static final int EMAIL_SUBJECT_MAX_LENGTH = 200;
    private static final String INFO_TASK_EMAIL_SUBJECT_PREFIX = "学生平台通知：";
    private static final String STUDENT_NOT_ASSIGNABLE_CODE = "STUDENT_NOT_ASSIGNABLE";
    private static final String STUDENT_ARCHIVED_CODE = "STUDENT_ARCHIVED";

    private final AuthSessionService authSessionService;
    private final InfoTaskRepository infoTaskRepository;
    private final InfoTaskRecipientRepository infoTaskRecipientRepository;
    private final InfoTaskAttachmentRepository infoTaskAttachmentRepository;
    private final InfoVolunteerTaskItemRepository infoVolunteerTaskItemRepository;
    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final EmailService emailService;
    private final InfoTaskAttachmentStorageService infoTaskAttachmentStorageService;
    private final TaskExecutor taskExecutor;
    private final boolean infoTaskEmailRemindersEnabled;

    public InfoTaskCenterService(AuthSessionService authSessionService,
                                 InfoTaskRepository infoTaskRepository,
                                 InfoTaskRecipientRepository infoTaskRecipientRepository,
                                 InfoTaskAttachmentRepository infoTaskAttachmentRepository,
                                 InfoVolunteerTaskItemRepository infoVolunteerTaskItemRepository,
                                 StudentRepository studentRepository,
                                 StudentProfileRepository studentProfileRepository,
                                 TeacherRepository teacherRepository,
                                 TeacherStudentRepository teacherStudentRepository,
                                 EmailService emailService,
                                 InfoTaskAttachmentStorageService infoTaskAttachmentStorageService,
                                 TaskExecutor taskExecutor,
                                 @Value("${app.info-task.email-reminders.enabled:false}")
                                 boolean infoTaskEmailRemindersEnabled) {
        this.authSessionService = authSessionService;
        this.infoTaskRepository = infoTaskRepository;
        this.infoTaskRecipientRepository = infoTaskRecipientRepository;
        this.infoTaskAttachmentRepository = infoTaskAttachmentRepository;
        this.infoVolunteerTaskItemRepository = infoVolunteerTaskItemRepository;
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherRepository = teacherRepository;
        this.teacherStudentRepository = teacherStudentRepository;
        this.emailService = emailService;
        this.infoTaskAttachmentStorageService = infoTaskAttachmentStorageService;
        this.taskExecutor = taskExecutor;
        this.infoTaskEmailRemindersEnabled = infoTaskEmailRemindersEnabled;
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

        List<InfoTaskRecipient> studentRecipients = infoPage.getContent();
        List<Long> infoTaskIds = collectInfoTaskIdsFromRecipients(studentRecipients);
        Map<Long, List<InfoTaskAttachmentDto>> attachmentsByInfoTaskId =
                loadAttachmentDtosByInfoTaskIds(infoTaskIds);
        List<InfoTaskDto> items = new ArrayList<InfoTaskDto>(studentRecipients.size());
        for (InfoTaskRecipient recipient : studentRecipients) {
            items.add(toStudentInfoTaskDto(
                    recipient.getInfoTask(),
                    recipient.isRead(),
                    recipient.getReadAt(),
                    attachmentsByInfoTaskId.get(recipient.getInfoTask().getId())
            ));
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

        List<InfoTask> infoTasks = infoPage.getContent();
        List<Long> infoTaskIds = collectInfoTaskIdsFromInfos(infoTasks);
        Map<Long, List<Long>> recipientStudentIdsByInfoTaskId =
                loadRecipientStudentIdsByInfoTaskIds(infoTaskIds);
        Map<Long, InfoTaskVolunteerDto> volunteerByInfoTaskId =
                loadVolunteerByInfoTaskIds(infoTaskIds);
        Map<Long, List<InfoTaskAttachmentDto>> attachmentsByInfoTaskId =
                loadAttachmentDtosByInfoTaskIds(infoTaskIds);

        List<InfoTaskDto> items = new ArrayList<InfoTaskDto>(infoTasks.size());
        for (InfoTask infoTask : infoTasks) {
            items.add(toInfoTaskDto(
                    infoTask,
                    false,
                    null,
                    recipientStudentIdsByInfoTaskId.get(infoTask.getId()),
                    volunteerByInfoTaskId.get(infoTask.getId()),
                    attachmentsByInfoTaskId.get(infoTask.getId())
            ));
        }
        return new InfoListResponseDto(items, infoPage.getTotalElements(), page, size);
    }

    @Transactional
    public InfoTaskDto createInfo(CreateInfoRequestDto requestBody, HttpServletRequest request) {
        return createInfo(requestBody, Collections.<MultipartFile>emptyList(), request);
    }

    @Transactional
    public InfoTaskDto createInfo(CreateInfoRequestDto requestBody,
                                  List<MultipartFile> attachments,
                                  HttpServletRequest request) {
        if (requestBody == null) {
            throw badRequest("request body is required");
        }
        assertAttachmentsWithinLimits(attachments);

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        String title = requireNonBlank(requestBody.getTitle(), "title", TITLE_MAX_LENGTH);
        String content = requireNonBlank(requestBody.getContent(), "content", CONTENT_MAX_LENGTH);
        InfoTaskCategory category = parseCategoryRequired(requestBody.getCategory());
        List<String> tags = normalizeTags(requestBody.getTags());
        List<Long> studentIds = normalizeStudentIds(requestBody.getStudentIds());
        String taskGroupId = normalizeOptionalTaskGroupId(requestBody.getTaskGroupId());
        Long goalId = normalizeOptionalGoalId(requestBody.getGoalId());
        NormalizedVolunteerPayload volunteerPayload = normalizeVolunteerPayload(requestBody.getVolunteer(), category);

        Teacher publisher = resolveTeacherForWrite(operator);
        List<Student> targetStudents = resolveTargetStudentsForInfo(operator, publisher, studentIds);
        int targetCount = targetStudents.size();

        String tagsText = joinTags(tags);
        InfoTask infoTask = null;
        if (taskGroupId != null) {
            infoTask = infoTaskRepository.findTopByPublishedByTeacher_IdAndTaskGroupIdOrderByIdDesc(
                    publisher.getId(),
                    taskGroupId
            ).orElse(null);
        }
        if (infoTask == null && goalId != null) {
            infoTask = infoTaskRepository.findTopByPublishedByTeacher_IdAndGoalIdOrderByIdDesc(publisher.getId(), goalId)
                    .orElse(null);
        }

        if (infoTask != null) {
            infoTask.overwrite(title, content, category, tagsText, targetCount, goalId, taskGroupId);
            infoTask = infoTaskRepository.save(infoTask);
            List<Student> newlyAddedStudents = overwriteRecipients(infoTask, targetStudents);
            overwriteVolunteerItems(infoTask, volunteerPayload);
            List<InfoTaskAttachment> savedAttachments =
                    overwriteAttachments(infoTask, attachments);
            sendInfoTaskEmailReminder(infoTask, newlyAddedStudents, savedAttachments);
            return toInfoTaskDto(
                    infoTask,
                    false,
                    null,
                    findRecipientStudentIdsByInfoTaskId(infoTask.getId()),
                    findVolunteerByInfoTaskId(infoTask.getId()),
                    findAttachmentsByInfoTaskId(infoTask.getId())
            );
        }

        infoTask = new InfoTask(
                title,
                content,
                category,
                tagsText,
                targetCount,
                publisher,
                goalId,
                taskGroupId
        );
        infoTask = infoTaskRepository.save(infoTask);
        saveRecipients(infoTask, targetStudents);
        overwriteVolunteerItems(infoTask, volunteerPayload);
        List<InfoTaskAttachment> savedAttachments = saveAttachments(infoTask, attachments);
        sendInfoTaskEmailReminder(infoTask, targetStudents, savedAttachments);
        return toInfoTaskDto(
                infoTask,
                false,
                null,
                findRecipientStudentIdsByInfoTaskId(infoTask.getId()),
                findVolunteerByInfoTaskId(infoTask.getId()),
                findAttachmentsByInfoTaskId(infoTask.getId())
        );
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
        return toStudentInfoTaskDto(
                saved.getInfoTask(),
                saved.isRead(),
                saved.getReadAt(),
                findAttachmentsByInfoTaskId(saved.getInfoTask().getId())
        );
    }

    @Transactional
    public void deleteInfoGroup(String taskGroupIdRaw, HttpServletRequest request) {
        String taskGroupId = requireNonBlank(taskGroupIdRaw, "taskGroupId", TASK_GROUP_ID_MAX_LENGTH);

        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }

        InfoTask infoTask;
        if (operator.getRole() == UserRole.TEACHER) {
            Teacher teacher = requireTeacherByUser(operator);
            infoTask = infoTaskRepository
                    .findTopByPublishedByTeacher_IdAndTaskGroupIdOrderByIdDesc(teacher.getId(), taskGroupId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Info task group not found."));
        } else {
            infoTask = infoTaskRepository
                    .findTopByTaskGroupIdOrderByIdDesc(taskGroupId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Info task group not found."));
        }

        Long infoTaskId = infoTask.getId();
        infoTaskAttachmentRepository.deleteByInfoTask_Id(infoTaskId);
        infoVolunteerTaskItemRepository.deleteByInfoTask_Id(infoTaskId);
        infoTaskRecipientRepository.deleteByInfoTask_Id(infoTaskId);
        infoTaskRepository.delete(infoTask);
    }

    @Transactional(readOnly = true)
    public InfoAttachmentDownload downloadInfoAttachment(Long infoId,
                                                         Long attachmentId,
                                                         HttpServletRequest request) {
        Long normalizedInfoId = requirePositiveId(infoId, "infoId");
        Long normalizedAttachmentId = requirePositiveId(attachmentId, "attachmentId");

        User operator = authSessionService.requireAuthenticatedUser(request);
        InfoTask infoTask = requireAccessibleInfoTask(normalizedInfoId, operator);
        InfoTaskAttachment attachment = infoTaskAttachmentRepository
                .findByIdAndInfoTask_Id(normalizedAttachmentId, infoTask.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Info attachment not found."));

        byte[] content = infoTaskAttachmentStorageService.readAllBytes(attachment.getStorageKey());
        String fileName = trimToNull(attachment.getOriginalFilename());
        if (fileName == null) {
            fileName = "attachment.bin";
        }
        String contentType = trimToNull(attachment.getMimeType());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return new InfoAttachmentDownload(fileName, contentType, content);
    }

    private List<Long> normalizeStudentIds(List<Long> rawStudentIds) {
        if (rawStudentIds == null || rawStudentIds.isEmpty()) {
            throw badRequest("studentIds is required");
        }

        LinkedHashSet<Long> deduplicated = new LinkedHashSet<Long>();
        for (Long rawId : rawStudentIds) {
            if (rawId == null || rawId.longValue() <= 0L) {
                throw badRequest("studentIds must contain positive integers");
            }
            deduplicated.add(rawId);
        }
        if (deduplicated.isEmpty()) {
            throw badRequest("studentIds is required");
        }
        return new ArrayList<Long>(deduplicated);
    }

    private Long normalizeOptionalGoalId(Long rawGoalId) {
        if (rawGoalId == null) {
            return null;
        }
        return requirePositiveId(rawGoalId, "goalId");
    }

    private String normalizeOptionalTaskGroupId(String rawTaskGroupId) {
        String normalized = trimToNull(rawTaskGroupId);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > TASK_GROUP_ID_MAX_LENGTH) {
            throw badRequest("taskGroupId too long");
        }
        return normalized;
    }

    private NormalizedVolunteerPayload normalizeVolunteerPayload(CreateInfoVolunteerDto volunteer,
                                                                 InfoTaskCategory category) {
        if (category != InfoTaskCategory.VOLUNTEER || volunteer == null) {
            return null;
        }

        List<CreateInfoVolunteerTaskItemDto> rawTasks = volunteer.getTasks();
        if (rawTasks == null || rawTasks.isEmpty()) {
            throw badRequest("volunteer.tasks must contain at least one item");
        }

        List<NormalizedVolunteerTaskItem> normalizedTasks =
                new ArrayList<NormalizedVolunteerTaskItem>(rawTasks.size());
        BigDecimal totalHours = BigDecimal.ZERO;
        for (int i = 0; i < rawTasks.size(); i++) {
            CreateInfoVolunteerTaskItemDto rawTask = rawTasks.get(i);
            String pathPrefix = "volunteer.tasks[" + i + "]";
            if (rawTask == null) {
                throw badRequest(pathPrefix + " is required");
            }

            String taskName = requireNonBlank(
                    rawTask.getTaskName(),
                    pathPrefix + ".taskName",
                    VOLUNTEER_TASK_NAME_MAX_LENGTH
            );
            String description = requireNonBlank(
                    rawTask.getDescription(),
                    pathPrefix + ".description",
                    VOLUNTEER_TASK_DESCRIPTION_MAX_LENGTH
            );

            BigDecimal durationHours = rawTask.getDurationHours();
            if (durationHours != null && durationHours.compareTo(BigDecimal.ZERO) <= 0) {
                throw badRequest(pathPrefix + ".durationHours must be greater than 0");
            }

            LocalDate startDate = rawTask.getStartDate();
            if (startDate == null) {
                throw badRequest(pathPrefix + ".startDate is required");
            }
            LocalDate endDate = rawTask.getEndDate();
            if (endDate == null) {
                throw badRequest(pathPrefix + ".endDate is required");
            }
            if (endDate.isBefore(startDate)) {
                throw badRequest(pathPrefix + ".endDate must be on or after startDate");
            }

            normalizedTasks.add(new NormalizedVolunteerTaskItem(
                    taskName,
                    description,
                    durationHours,
                    startDate,
                    endDate,
                    trimToNull(rawTask.getVerifierContact())
            ));
            if (durationHours != null) {
                totalHours = totalHours.add(durationHours);
            }
        }
        return new NormalizedVolunteerPayload(totalHours, normalizedTasks);
    }

    private void overwriteVolunteerItems(InfoTask infoTask, NormalizedVolunteerPayload volunteerPayload) {
        if (infoTask == null || infoTask.getId() == null) {
            return;
        }

        infoVolunteerTaskItemRepository.deleteByInfoTask_Id(infoTask.getId());
        if (volunteerPayload == null || volunteerPayload.getTasks().isEmpty()) {
            return;
        }

        List<InfoVolunteerTaskItem> entities =
                new ArrayList<InfoVolunteerTaskItem>(volunteerPayload.getTasks().size());
        for (NormalizedVolunteerTaskItem taskItem : volunteerPayload.getTasks()) {
            entities.add(new InfoVolunteerTaskItem(
                    infoTask,
                    taskItem.getTaskName(),
                    taskItem.getDescription(),
                    taskItem.getDurationHours(),
                    taskItem.getStartDate(),
                    taskItem.getEndDate(),
                    taskItem.getVerifierContact()
            ));
        }
        infoVolunteerTaskItemRepository.saveAll(entities);
    }

    private void saveRecipients(InfoTask infoTask, List<Student> targetStudents) {
        if (targetStudents == null || targetStudents.isEmpty()) {
            return;
        }
        List<InfoTaskRecipient> recipients = new ArrayList<InfoTaskRecipient>(targetStudents.size());
        for (Student student : targetStudents) {
            recipients.add(new InfoTaskRecipient(infoTask, student));
        }
        infoTaskRecipientRepository.saveAll(recipients);
    }

    private List<Student> overwriteRecipients(InfoTask infoTask, List<Student> targetStudents) {
        List<InfoTaskRecipient> existingRecipients = infoTaskRecipientRepository.findByInfoTask_Id(infoTask.getId());
        Map<Long, InfoTaskRecipient> existingByStudentId = new HashMap<Long, InfoTaskRecipient>(existingRecipients.size());
        for (InfoTaskRecipient recipient : existingRecipients) {
            if (recipient == null || recipient.getStudent() == null || recipient.getStudent().getId() == null) {
                continue;
            }
            existingByStudentId.put(recipient.getStudent().getId(), recipient);
        }

        Set<Long> targetStudentIds = new LinkedHashSet<Long>();
        List<InfoTaskRecipient> recipientsToCreate = new ArrayList<InfoTaskRecipient>();
        List<Student> newlyAddedStudents = new ArrayList<Student>();
        for (Student student : targetStudents) {
            if (student == null || student.getId() == null) {
                continue;
            }
            Long studentId = student.getId();
            targetStudentIds.add(studentId);
            InfoTaskRecipient existing = existingByStudentId.remove(studentId);
            if (existing == null) {
                recipientsToCreate.add(new InfoTaskRecipient(infoTask, student));
                newlyAddedStudents.add(student);
            } else {
                existing.markUnread();
            }
        }

        List<InfoTaskRecipient> recipientsToDelete = new ArrayList<InfoTaskRecipient>();
        for (Map.Entry<Long, InfoTaskRecipient> entry : existingByStudentId.entrySet()) {
            if (!targetStudentIds.contains(entry.getKey())) {
                recipientsToDelete.add(entry.getValue());
            }
        }

        if (!recipientsToDelete.isEmpty()) {
            infoTaskRecipientRepository.deleteAll(recipientsToDelete);
        }
        if (!recipientsToCreate.isEmpty()) {
            infoTaskRecipientRepository.saveAll(recipientsToCreate);
        }
        return newlyAddedStudents;
    }

    private void sendInfoTaskEmailReminder(InfoTask infoTask,
                                           List<Student> recipients,
                                           List<InfoTaskAttachment> attachments) {
        if (!infoTaskEmailRemindersEnabled || recipients == null || recipients.isEmpty()) {
            return;
        }

        List<String> emails = collectInfoTaskReminderEmails(recipients);
        if (emails.isEmpty()) {
            return;
        }

        final Long infoTaskId = infoTask == null ? null : infoTask.getId();
        final String subject = buildInfoTaskEmailSubject(infoTask);
        final String body = buildInfoTaskEmailBody(infoTask);
        List<EmailAttachment> emailAttachmentsTmp;
        try {
            emailAttachmentsTmp = buildEmailAttachments(attachments);
        } catch (RuntimeException ex) {
            log.warn("Failed to load info task email attachments for infoTaskId={}", infoTaskId, ex);
            emailAttachmentsTmp = Collections.emptyList();
        }
        final List<EmailAttachment> emailAttachments = emailAttachmentsTmp;
        taskExecutor.execute(() -> {
            try {
                emailService.sendTextEmail(emails, subject, body, emailAttachments);
            } catch (RuntimeException ex) {
                log.warn("Failed to send info task reminder email for infoTaskId={}", infoTaskId, ex);
            }
        });
    }

    private List<String> collectInfoTaskReminderEmails(List<Student> students) {
        if (students == null || students.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> studentIds = new ArrayList<Long>(students.size());
        for (Student student : students) {
            if (student != null && student.getId() != null) {
                studentIds.add(student.getId());
            }
        }
        if (studentIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, StudentProfile> profileByStudentId = findProfilesByStudentIds(studentIds);
        LinkedHashSet<String> emails = new LinkedHashSet<String>();
        for (Student student : students) {
            if (student == null || student.getId() == null) {
                continue;
            }
            StudentProfile profile = profileByStudentId.get(student.getId());
            String email = profile == null ? null : trimToNull(profile.getEmail());
            if (email == null && student.getUser() != null) {
                email = trimToNull(student.getUser().getUsername());
            }
            if (email != null && email.contains("@")) {
                emails.add(email);
            }
        }
        return new ArrayList<String>(emails);
    }

    private Map<Long, StudentProfile> findProfilesByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StudentProfile> profiles = studentProfileRepository.findByStudentIdsWithStudent(studentIds);
        Map<Long, StudentProfile> profileByStudentId = new HashMap<Long, StudentProfile>();
        for (StudentProfile profile : profiles) {
            if (profile != null && profile.getStudent() != null && profile.getStudent().getId() != null) {
                profileByStudentId.put(profile.getStudent().getId(), profile);
            }
        }
        return profileByStudentId;
    }

    private String buildInfoTaskEmailSubject(InfoTask infoTask) {
        String title = infoTask == null ? null : trimToNull(infoTask.getTitle());
        String subject = title == null ? "Task reminder" : INFO_TASK_EMAIL_SUBJECT_PREFIX + title;
        return abbreviateForEmail(subject, EMAIL_SUBJECT_MAX_LENGTH);
    }

    private String buildInfoTaskEmailBody(InfoTask infoTask) {
        String title = infoTask == null ? null : trimToNull(infoTask.getTitle());
        String content = infoTask == null ? null : trimToNull(infoTask.getContent());
        String category = infoTask == null || infoTask.getCategory() == null
                ? null
                : infoTaskCategoryEmailLabel(infoTask.getCategory());
        String tags = infoTask == null ? null : trimToNull(infoTask.getTagsText());
        String publisherName = infoTask == null || infoTask.getPublishedByTeacher() == null
                ? null
                : buildTeacherDisplayName(infoTask.getPublishedByTeacher());

        StringBuilder body = new StringBuilder();
        body.append("同学你好，\n\n");
        body.append("你收到了一条新的学生平台通知，请及时登录学生端查看并完成相关事项。\n\n");
        if (title != null) {
            body.append("通知标题：").append(title).append("\n");
        }
        if (category != null) {
            body.append("通知类型：").append(category).append("\n");
        }
        if (tags != null) {
            body.append("标签：").append(tags).append("\n");
        }
        if (publisherName != null) {
            body.append("发布老师：").append(publisherName).append("\n");
        }
        if (content != null) {
            body.append("\n通知内容：\n");
            body.append(abbreviateForEmail(content, EMAIL_CONTENT_PREVIEW_MAX_LENGTH)).append("\n");
        }
        body.append("\n请登录学生平台查看完整通知详情。");
        return body.toString();
    }

    private String infoTaskCategoryEmailLabel(InfoTaskCategory category) {
        if (category == null) {
            return null;
        }
        switch (category) {
            case VOLUNTEER:
                return "义工";
            case ACTIVITY:
            default:
                return "活动";
        }
    }

    private List<EmailAttachment> buildEmailAttachments(List<InfoTaskAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }

        List<EmailAttachment> emailAttachments = new ArrayList<EmailAttachment>(attachments.size());
        for (InfoTaskAttachment attachment : attachments) {
            if (attachment == null || attachment.getStorageKey() == null) {
                continue;
            }
            byte[] content = infoTaskAttachmentStorageService.readAllBytes(attachment.getStorageKey());
            emailAttachments.add(new EmailAttachment(
                    attachment.getOriginalFilename(),
                    attachment.getMimeType(),
                    content
            ));
        }
        return emailAttachments;
    }

    private String abbreviateForEmail(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        if (maxLength <= 3) {
            return value.substring(0, maxLength);
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private Map<Long, List<Long>> loadRecipientStudentIdsByInfoTaskIds(List<Long> infoTaskIds) {
        if (infoTaskIds == null || infoTaskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LinkedHashSet<Long> deduplicatedInfoTaskIds = new LinkedHashSet<Long>();
        for (Long infoTaskId : infoTaskIds) {
            if (infoTaskId != null) {
                deduplicatedInfoTaskIds.add(infoTaskId);
            }
        }
        if (deduplicatedInfoTaskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<Long>> recipientStudentIdsByInfoTaskId =
                new LinkedHashMap<Long, List<Long>>(deduplicatedInfoTaskIds.size());
        for (Long infoTaskId : deduplicatedInfoTaskIds) {
            recipientStudentIdsByInfoTaskId.put(infoTaskId, new ArrayList<Long>());
        }

        List<InfoTaskRecipientRepository.InfoTaskRecipientStudentIdView> recipientRows =
                infoTaskRecipientRepository.findRecipientStudentIdsByInfoTaskIds(
                        new ArrayList<Long>(deduplicatedInfoTaskIds)
                );
        for (InfoTaskRecipientRepository.InfoTaskRecipientStudentIdView recipientRow : recipientRows) {
            if (recipientRow == null || recipientRow.getInfoTaskId() == null || recipientRow.getStudentId() == null) {
                continue;
            }
            List<Long> recipientStudentIds = recipientStudentIdsByInfoTaskId.get(recipientRow.getInfoTaskId());
            if (recipientStudentIds != null) {
                recipientStudentIds.add(recipientRow.getStudentId());
            }
        }
        return recipientStudentIdsByInfoTaskId;
    }

    private Map<Long, InfoTaskVolunteerDto> loadVolunteerByInfoTaskIds(List<Long> infoTaskIds) {
        if (infoTaskIds == null || infoTaskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LinkedHashSet<Long> deduplicatedInfoTaskIds = new LinkedHashSet<Long>();
        for (Long infoTaskId : infoTaskIds) {
            if (infoTaskId != null) {
                deduplicatedInfoTaskIds.add(infoTaskId);
            }
        }
        if (deduplicatedInfoTaskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<InfoVolunteerTaskItem> volunteerTaskItems =
                infoVolunteerTaskItemRepository.findByInfoTask_IdInOrderByInfoTask_IdAscIdAsc(
                        new ArrayList<Long>(deduplicatedInfoTaskIds)
                );
        if (volunteerTaskItems == null || volunteerTaskItems.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<InfoVolunteerTaskItem>> volunteerItemsByInfoTaskId =
                new LinkedHashMap<Long, List<InfoVolunteerTaskItem>>();
        for (InfoVolunteerTaskItem volunteerTaskItem : volunteerTaskItems) {
            if (volunteerTaskItem == null
                    || volunteerTaskItem.getInfoTask() == null
                    || volunteerTaskItem.getInfoTask().getId() == null) {
                continue;
            }
            Long infoTaskId = volunteerTaskItem.getInfoTask().getId();
            List<InfoVolunteerTaskItem> taskItems = volunteerItemsByInfoTaskId.get(infoTaskId);
            if (taskItems == null) {
                taskItems = new ArrayList<InfoVolunteerTaskItem>();
                volunteerItemsByInfoTaskId.put(infoTaskId, taskItems);
            }
            taskItems.add(volunteerTaskItem);
        }

        Map<Long, InfoTaskVolunteerDto> volunteerByInfoTaskId =
                new LinkedHashMap<Long, InfoTaskVolunteerDto>(volunteerItemsByInfoTaskId.size());
        for (Map.Entry<Long, List<InfoVolunteerTaskItem>> entry : volunteerItemsByInfoTaskId.entrySet()) {
            InfoTaskVolunteerDto volunteer = toVolunteerDto(entry.getValue());
            if (volunteer != null) {
                volunteerByInfoTaskId.put(entry.getKey(), volunteer);
            }
        }
        return volunteerByInfoTaskId;
    }

    private List<Long> collectInfoTaskIdsFromRecipients(List<InfoTaskRecipient> infoTaskRecipients) {
        if (infoTaskRecipients == null || infoTaskRecipients.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> infoTaskIds = new ArrayList<Long>(infoTaskRecipients.size());
        for (InfoTaskRecipient infoTaskRecipient : infoTaskRecipients) {
            if (infoTaskRecipient == null || infoTaskRecipient.getInfoTask() == null || infoTaskRecipient.getInfoTask().getId() == null) {
                continue;
            }
            infoTaskIds.add(infoTaskRecipient.getInfoTask().getId());
        }
        return infoTaskIds;
    }

    private List<Long> collectInfoTaskIdsFromInfos(List<InfoTask> infoTasks) {
        if (infoTasks == null || infoTasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> infoTaskIds = new ArrayList<Long>(infoTasks.size());
        for (InfoTask infoTask : infoTasks) {
            if (infoTask == null || infoTask.getId() == null) {
                continue;
            }
            infoTaskIds.add(infoTask.getId());
        }
        return infoTaskIds;
    }

    private List<Long> findRecipientStudentIdsByInfoTaskId(Long infoTaskId) {
        if (infoTaskId == null) {
            return Collections.emptyList();
        }
        List<Long> recipientStudentIds = infoTaskRecipientRepository.findStudentIdsByInfoTaskId(infoTaskId);
        if (recipientStudentIds == null) {
            return Collections.emptyList();
        }
        return recipientStudentIds;
    }

    private InfoTaskVolunteerDto findVolunteerByInfoTaskId(Long infoTaskId) {
        if (infoTaskId == null) {
            return null;
        }
        return toVolunteerDto(infoVolunteerTaskItemRepository.findByInfoTask_IdOrderByIdAsc(infoTaskId));
    }

    private List<Student> resolveTargetStudentsForInfo(User operator,
                                                       Teacher teacher,
                                                       List<Long> targetStudentIds) {
        List<Student> sourceStudents = studentRepository.findByIdInWithUser(targetStudentIds);
        Map<Long, Student> studentById = new LinkedHashMap<Long, Student>();
        for (Student sourceStudent : sourceStudents) {
            if (sourceStudent == null || sourceStudent.getId() == null) {
                continue;
            }
            studentById.put(sourceStudent.getId(), sourceStudent);
        }

        List<Student> orderedStudents = new ArrayList<Student>(targetStudentIds.size());
        for (Long studentId : targetStudentIds) {
            Student student = studentById.get(studentId);
            if (student == null) {
                throw badRequest("studentId is invalid: " + studentId);
            }
            if (isStudentArchived(student)) {
                throw studentArchivedException(studentId);
            }
            if (operator.getRole() == UserRole.TEACHER) {
                ensureStudentAssignableForTeacher(teacher, studentId);
            }
            orderedStudents.add(student);
        }

        return orderedStudents;
    }

    private void ensureStudentAssignableForTeacher(Teacher teacher, Long studentId) {
        Long teacherId = teacher == null ? null : teacher.getId();
        TeacherStudent relation = (teacherId == null || studentId == null)
                ? null
                : teacherStudentRepository.findTopByTeacher_IdAndStudent_IdOrderByIdDesc(teacherId, studentId).orElse(null);
        if (relation == null || relation.getStatus() != TeacherStudentStatus.ACTIVE) {
            throw new ApiRequestException(
                    HttpStatus.BAD_REQUEST,
                    STUDENT_NOT_ASSIGNABLE_CODE,
                    "studentId is not assignable to current teacher: " + studentId
            );
        }
    }

    private ApiRequestException studentArchivedException(Long studentId) {
        return new ApiRequestException(
                HttpStatus.BAD_REQUEST,
                STUDENT_ARCHIVED_CODE,
                "student is archived and cannot be assigned: " + studentId
        );
    }

    private boolean isStudentArchived(Student student) {
        if (student == null || student.getUser() == null) {
            return true;
        }
        return student.getUser().getStatus() == UserAccountStatus.ARCHIVED;
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

    private InfoTask requireAccessibleInfoTask(Long infoId, User operator) {
        if (operator.getRole() == UserRole.ADMIN) {
            return infoTaskRepository.findById(infoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Info task not found."));
        }
        if (operator.getRole() == UserRole.TEACHER) {
            Teacher teacher = requireTeacherByUser(operator);
            InfoTask infoTask = infoTaskRepository.findById(infoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Info task not found."));
            if (infoTask.getPublishedByTeacher() == null
                    || infoTask.getPublishedByTeacher().getId() == null
                    || !infoTask.getPublishedByTeacher().getId().equals(teacher.getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Info task not found.");
            }
            return infoTask;
        }

        if (operator.getRole() == UserRole.STUDENT) {
            Student student = studentRepository.findByUser_Id(operator.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found."));
            InfoTaskRecipient recipient = infoTaskRecipientRepository
                    .findByInfoTask_IdAndStudent_Id(infoId, student.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Info task not found."));
            return recipient.getInfoTask();
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden.");
    }

    private InfoTaskDto toInfoTaskDto(InfoTask infoTask,
                                      boolean read,
                                      LocalDateTime readAt,
                                      List<Long> recipientStudentIds,
                                      InfoTaskVolunteerDto volunteer,
                                      List<InfoTaskAttachmentDto> attachments) {
        Teacher publisher = infoTask.getPublishedByTeacher();
        return new InfoTaskDto(
                infoTask.getId(),
                "INFO",
                infoTask.getTitle(),
                infoTask.getContent(),
                infoTask.getCategory(),
                parseTags(infoTask.getTagsText()),
                infoTask.getGoalId(),
                infoTask.getTaskGroupId(),
                volunteer,
                attachments == null ? Collections.<InfoTaskAttachmentDto>emptyList() : attachments,
                recipientStudentIds == null ? Collections.<Long>emptyList() : recipientStudentIds,
                infoTask.getTargetStudentCount(),
                publisher.getId(),
                buildTeacherDisplayName(publisher),
                infoTask.getCreatedAt() == null ? null : infoTask.getCreatedAt().toString(),
                infoTask.getUpdatedAt() == null ? null : infoTask.getUpdatedAt().toString(),
                read,
                readAt == null ? null : readAt.toString()
        );
    }

    private InfoTaskDto toStudentInfoTaskDto(InfoTask infoTask,
                                             boolean read,
                                             LocalDateTime readAt,
                                             List<InfoTaskAttachmentDto> attachments) {
        Teacher publisher = infoTask.getPublishedByTeacher();
        return new InfoTaskDto(
                infoTask.getId(),
                "INFO",
                infoTask.getTitle(),
                infoTask.getContent(),
                infoTask.getCategory(),
                Collections.<String>emptyList(),
                null,
                null,
                null,
                attachments == null ? Collections.<InfoTaskAttachmentDto>emptyList() : attachments,
                Collections.<Long>emptyList(),
                0,
                publisher.getId(),
                buildTeacherDisplayName(publisher),
                infoTask.getCreatedAt() == null ? null : infoTask.getCreatedAt().toString(),
                infoTask.getUpdatedAt() == null ? null : infoTask.getUpdatedAt().toString(),
                read,
                readAt == null ? null : readAt.toString()
        );
    }

    private Map<Long, List<InfoTaskAttachmentDto>> loadAttachmentDtosByInfoTaskIds(List<Long> infoTaskIds) {
        if (infoTaskIds == null || infoTaskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LinkedHashSet<Long> deduplicatedInfoTaskIds = new LinkedHashSet<Long>();
        for (Long infoTaskId : infoTaskIds) {
            if (infoTaskId != null) {
                deduplicatedInfoTaskIds.add(infoTaskId);
            }
        }
        if (deduplicatedInfoTaskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<InfoTaskAttachmentDto>> attachmentsByInfoTaskId =
                new LinkedHashMap<Long, List<InfoTaskAttachmentDto>>(deduplicatedInfoTaskIds.size());
        for (Long infoTaskId : deduplicatedInfoTaskIds) {
            attachmentsByInfoTaskId.put(infoTaskId, new ArrayList<InfoTaskAttachmentDto>());
        }

        List<InfoTaskAttachment> attachments =
                infoTaskAttachmentRepository.findByInfoTask_IdInOrderByInfoTask_IdAscIdAsc(
                        new ArrayList<Long>(deduplicatedInfoTaskIds)
                );
        for (InfoTaskAttachment attachment : attachments) {
            if (attachment == null || attachment.getInfoTask() == null || attachment.getInfoTask().getId() == null) {
                continue;
            }
            List<InfoTaskAttachmentDto> items = attachmentsByInfoTaskId.get(attachment.getInfoTask().getId());
            if (items != null) {
                items.add(toAttachmentDto(attachment));
            }
        }
        return attachmentsByInfoTaskId;
    }

    private List<InfoTaskAttachmentDto> findAttachmentsByInfoTaskId(Long infoTaskId) {
        if (infoTaskId == null || infoTaskId.longValue() <= 0L) {
            return Collections.emptyList();
        }
        List<InfoTaskAttachment> attachments =
                infoTaskAttachmentRepository.findByInfoTask_IdOrderByCreatedAtAscIdAsc(infoTaskId);
        List<InfoTaskAttachmentDto> items = new ArrayList<InfoTaskAttachmentDto>(attachments.size());
        for (InfoTaskAttachment attachment : attachments) {
            items.add(toAttachmentDto(attachment));
        }
        return items;
    }

    private List<InfoTaskAttachment> saveAttachments(InfoTask infoTask, List<MultipartFile> attachments) {
        if (infoTask == null || attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }

        List<InfoTaskAttachment> savedAttachments = new ArrayList<InfoTaskAttachment>();
        for (MultipartFile file : attachments) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            assertAttachmentSizeWithinLimit(file);
            StoredAttachment stored = infoTaskAttachmentStorageService.store(infoTask.getId(), file);
            InfoTaskAttachment attachment = new InfoTaskAttachment(
                    infoTask,
                    stored.getStorageKey(),
                    stored.getOriginalFilename(),
                    stored.getContentType(),
                    Long.valueOf(stored.getSizeBytes())
            );
            savedAttachments.add(infoTaskAttachmentRepository.save(attachment));
        }
        return savedAttachments;
    }

    private List<InfoTaskAttachment> overwriteAttachments(InfoTask infoTask, List<MultipartFile> attachments) {
        if (infoTask == null || attachments == null || attachments.isEmpty()) {
            return findAttachmentsByEntity(infoTask);
        }
        List<InfoTaskAttachment> existingAttachments = findAttachmentsByEntity(infoTask);
        List<InfoTaskAttachment> newAttachments = saveAttachments(infoTask, attachments);
        if (!existingAttachments.isEmpty()) {
            infoTaskAttachmentRepository.deleteAll(existingAttachments);
            for (InfoTaskAttachment attachment : existingAttachments) {
                infoTaskAttachmentStorageService.deleteIfExists(attachment.getStorageKey());
            }
        }
        return newAttachments;
    }

    private List<InfoTaskAttachment> findAttachmentsByEntity(InfoTask infoTask) {
        if (infoTask == null || infoTask.getId() == null) {
            return Collections.emptyList();
        }
        return infoTaskAttachmentRepository.findByInfoTask_IdOrderByCreatedAtAscIdAsc(infoTask.getId());
    }

    private void assertAttachmentSizeWithinLimit(MultipartFile file) {
        if (file.getSize() > ATTACHMENT_MAX_UPLOAD_SIZE_BYTES) {
            throw badRequest("attachment too large; max is 30MB");
        }
    }

    void assertAttachmentsWithinLimits(List<MultipartFile> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        long totalSize = 0L;
        for (MultipartFile file : attachments) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            assertAttachmentSizeWithinLimit(file);
            if (file.getSize() > ATTACHMENTS_MAX_TOTAL_SIZE_BYTES - totalSize) {
                throw badRequest("attachments too large; combined max is 100MB");
            }
            totalSize += file.getSize();
        }
    }

    private InfoTaskAttachmentDto toAttachmentDto(InfoTaskAttachment attachment) {
        if (attachment == null) {
            return null;
        }
        return new InfoTaskAttachmentDto(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getMimeType(),
                attachment.getSizeBytes(),
                attachment.getCreatedAt() == null ? null : attachment.getCreatedAt().toString()
        );
    }

    private InfoTaskVolunteerDto toVolunteerDto(List<InfoVolunteerTaskItem> volunteerTaskItems) {
        if (volunteerTaskItems == null || volunteerTaskItems.isEmpty()) {
            return null;
        }

        List<InfoTaskVolunteerTaskItemDto> tasks =
                new ArrayList<InfoTaskVolunteerTaskItemDto>(volunteerTaskItems.size());
        BigDecimal totalHours = BigDecimal.ZERO;
        for (InfoVolunteerTaskItem volunteerTaskItem : volunteerTaskItems) {
            if (volunteerTaskItem == null) {
                continue;
            }
            BigDecimal durationHours = volunteerTaskItem.getDurationHours();
            if (durationHours != null) {
                totalHours = totalHours.add(durationHours);
            }
            tasks.add(new InfoTaskVolunteerTaskItemDto(
                    volunteerTaskItem.getTaskName(),
                    volunteerTaskItem.getDescription(),
                    durationHours,
                    volunteerTaskItem.getStartDate(),
                    volunteerTaskItem.getEndDate(),
                    volunteerTaskItem.getVerifierContact()
            ));
        }
        if (tasks.isEmpty()) {
            return null;
        }
        return new InfoTaskVolunteerDto(totalHours, tasks);
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

    public static class InfoAttachmentDownload {
        private final String fileName;
        private final String contentType;
        private final byte[] content;

        public InfoAttachmentDownload(String fileName, String contentType, byte[] content) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.content = content == null ? new byte[0] : content.clone();
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getContent() {
            return content.clone();
        }
    }

    private static class NormalizedVolunteerPayload {
        private final BigDecimal totalHours;
        private final List<NormalizedVolunteerTaskItem> tasks;

        private NormalizedVolunteerPayload(BigDecimal totalHours, List<NormalizedVolunteerTaskItem> tasks) {
            this.totalHours = totalHours;
            this.tasks = tasks;
        }

        public BigDecimal getTotalHours() {
            return totalHours;
        }

        public List<NormalizedVolunteerTaskItem> getTasks() {
            return tasks;
        }
    }

    private static class NormalizedVolunteerTaskItem {
        private final String taskName;
        private final String description;
        private final BigDecimal durationHours;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String verifierContact;

        private NormalizedVolunteerTaskItem(String taskName,
                                            String description,
                                            BigDecimal durationHours,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            String verifierContact) {
            this.taskName = taskName;
            this.description = description;
            this.durationHours = durationHours;
            this.startDate = startDate;
            this.endDate = endDate;
            this.verifierContact = verifierContact;
        }

        public String getTaskName() {
            return taskName;
        }

        public String getDescription() {
            return description;
        }

        public BigDecimal getDurationHours() {
            return durationHours;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public String getVerifierContact() {
            return verifierContact;
        }
    }

    private ApiRequestException badRequest(String message) {
        return new ApiRequestException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
