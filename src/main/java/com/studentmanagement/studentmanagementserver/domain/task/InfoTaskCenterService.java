package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.InfoTaskRecipientRepository;
import com.studentmanagement.studentmanagementserver.repo.InfoTaskRepository;
import com.studentmanagement.studentmanagementserver.repo.InfoVolunteerTaskItemRepository;
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
    private static final String STUDENT_NOT_ASSIGNABLE_CODE = "STUDENT_NOT_ASSIGNABLE";
    private static final String STUDENT_ARCHIVED_CODE = "STUDENT_ARCHIVED";

    private final AuthSessionService authSessionService;
    private final InfoTaskRepository infoTaskRepository;
    private final InfoTaskRecipientRepository infoTaskRecipientRepository;
    private final InfoVolunteerTaskItemRepository infoVolunteerTaskItemRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherStudentRepository teacherStudentRepository;

    public InfoTaskCenterService(AuthSessionService authSessionService,
                                 InfoTaskRepository infoTaskRepository,
                                 InfoTaskRecipientRepository infoTaskRecipientRepository,
                                 InfoVolunteerTaskItemRepository infoVolunteerTaskItemRepository,
                                 StudentRepository studentRepository,
                                 TeacherRepository teacherRepository,
                                 TeacherStudentRepository teacherStudentRepository) {
        this.authSessionService = authSessionService;
        this.infoTaskRepository = infoTaskRepository;
        this.infoTaskRecipientRepository = infoTaskRecipientRepository;
        this.infoVolunteerTaskItemRepository = infoVolunteerTaskItemRepository;
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

        List<InfoTaskRecipient> studentRecipients = infoPage.getContent();
        List<Long> infoTaskIds = collectInfoTaskIdsFromRecipients(studentRecipients);
        Map<Long, List<Long>> recipientStudentIdsByInfoTaskId =
                loadRecipientStudentIdsByInfoTaskIds(infoTaskIds);
        Map<Long, InfoTaskVolunteerDto> volunteerByInfoTaskId =
                loadVolunteerByInfoTaskIds(infoTaskIds);

        List<InfoTaskDto> items = new ArrayList<InfoTaskDto>(studentRecipients.size());
        for (InfoTaskRecipient recipient : studentRecipients) {
            InfoTask infoTask = recipient.getInfoTask();
            items.add(toInfoTaskDto(
                    infoTask,
                    recipient.isRead(),
                    recipient.getReadAt(),
                    recipientStudentIdsByInfoTaskId.get(infoTask.getId()),
                    volunteerByInfoTaskId.get(infoTask.getId())
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

        List<InfoTaskDto> items = new ArrayList<InfoTaskDto>(infoTasks.size());
        for (InfoTask infoTask : infoTasks) {
            items.add(toInfoTaskDto(
                    infoTask,
                    false,
                    null,
                    recipientStudentIdsByInfoTaskId.get(infoTask.getId()),
                    volunteerByInfoTaskId.get(infoTask.getId())
            ));
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
            overwriteRecipients(infoTask, targetStudents);
            overwriteVolunteerItems(infoTask, volunteerPayload);
            return toInfoTaskDto(
                    infoTask,
                    false,
                    null,
                    findRecipientStudentIdsByInfoTaskId(infoTask.getId()),
                    findVolunteerByInfoTaskId(infoTask.getId())
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
        return toInfoTaskDto(
                infoTask,
                false,
                null,
                findRecipientStudentIdsByInfoTaskId(infoTask.getId()),
                findVolunteerByInfoTaskId(infoTask.getId())
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
        return toInfoTaskDto(
                saved.getInfoTask(),
                saved.isRead(),
                saved.getReadAt(),
                findRecipientStudentIdsByInfoTaskId(saved.getInfoTask().getId()),
                findVolunteerByInfoTaskId(saved.getInfoTask().getId())
        );
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
            String verifierContact = requireNonBlank(
                    rawTask.getVerifierContact(),
                    pathPrefix + ".verifierContact",
                    VOLUNTEER_VERIFIER_CONTACT_MAX_LENGTH
            );

            BigDecimal durationHours = rawTask.getDurationHours();
            if (durationHours == null) {
                throw badRequest(pathPrefix + ".durationHours is required");
            }
            if (durationHours.compareTo(BigDecimal.ZERO) <= 0) {
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
                    verifierContact
            ));
            totalHours = totalHours.add(durationHours);
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

    private void overwriteRecipients(InfoTask infoTask, List<Student> targetStudents) {
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
        for (Student student : targetStudents) {
            if (student == null || student.getId() == null) {
                continue;
            }
            Long studentId = student.getId();
            targetStudentIds.add(studentId);
            InfoTaskRecipient existing = existingByStudentId.remove(studentId);
            if (existing == null) {
                recipientsToCreate.add(new InfoTaskRecipient(infoTask, student));
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

    private InfoTaskDto toInfoTaskDto(InfoTask infoTask,
                                      boolean read,
                                      LocalDateTime readAt,
                                      List<Long> recipientStudentIds,
                                      InfoTaskVolunteerDto volunteer) {
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
