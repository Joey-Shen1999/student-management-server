package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.service.ApiRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping
public class TaskCenterController {

    private final TaskCenterService goalTaskCenterService;
    private final InfoTaskCenterService infoTaskCenterService;
    private final DllTaskCenterService dllTaskCenterService;

    public TaskCenterController(TaskCenterService goalTaskCenterService,
                                InfoTaskCenterService infoTaskCenterService,
                                DllTaskCenterService dllTaskCenterService) {
        this.goalTaskCenterService = goalTaskCenterService;
        this.infoTaskCenterService = infoTaskCenterService;
        this.dllTaskCenterService = dllTaskCenterService;
    }

    @GetMapping("/api/student/tasks")
    public ResponseEntity<?> listMyTasks(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "unreadOnly", required = false) String unreadOnly,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "size", required = false) String size,
            HttpServletRequest request) {
        if ("GOAL".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(goalTaskCenterService.listMyGoals(type, status, keyword, page, size, request));
        }
        if ("INFO".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(
                    infoTaskCenterService.listMyInfos(category, tag, keyword, unreadOnly, page, size, request)
            );
        }
        throw badRequest("type must be GOAL or INFO");
    }

    @PatchMapping("/api/student/tasks/{taskId}/status")
    public ResponseEntity<GoalTaskDto> updateMyGoalStatus(
            @PathVariable Long taskId,
            @RequestBody(required = false) UpdateGoalStatusRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(goalTaskCenterService.updateMyGoalStatus(taskId, requestBody, request));
    }

    @PatchMapping("/api/student/tasks/{infoId}/read")
    public ResponseEntity<InfoTaskDto> markMyInfoAsRead(@PathVariable Long infoId,
                                                        @RequestBody(required = false) Object ignoredBody,
                                                        HttpServletRequest request) {
        return ResponseEntity.ok(infoTaskCenterService.markMyInfoAsRead(infoId, request));
    }

    @GetMapping("/api/teacher/tasks")
    public ResponseEntity<?> listTeacherTasks(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "studentId", required = false) String studentId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "size", required = false) String size,
            HttpServletRequest request) {
        if ("GOAL".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(
                    goalTaskCenterService.listTeacherGoals(type, studentId, status, keyword, page, size, request)
            );
        }
        if ("INFO".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(
                    infoTaskCenterService.listTeacherInfos(category, tag, keyword, page, size, request)
            );
        }
        if ("DLL".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(dllTaskCenterService.listTeacherDllTasks(page, size, request));
        }
        throw badRequest("type must be GOAL, INFO or DLL");
    }

    @PostMapping("/api/teacher/tasks/goals")
    public ResponseEntity<GoalTaskDto> createGoal(
            @RequestBody(required = false) CreateGoalRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(goalTaskCenterService.createGoal(requestBody, request));
    }

    @PostMapping("/api/teacher/tasks/infos")
    public ResponseEntity<InfoTaskDto> createInfo(
            @RequestBody(required = false) CreateInfoRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(infoTaskCenterService.createInfo(requestBody, request));
    }

    @PostMapping("/api/teacher/tasks/dll-templates")
    public ResponseEntity<DllTemplateDto> createDllTemplate(
            @RequestBody(required = false) CreateDllTemplateRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(dllTaskCenterService.createDllTemplate(requestBody, request));
    }

    @PostMapping("/api/teacher/tasks/dll-templates/{templateId}/instantiate")
    public ResponseEntity<DllTaskDto> instantiateDllTemplate(
            @PathVariable Long templateId,
            @RequestBody(required = false) InstantiateDllTemplateRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(dllTaskCenterService.instantiateTemplate(templateId, requestBody, request));
    }

    @PatchMapping("/api/teacher/tasks/{taskId}/status")
    public ResponseEntity<GoalTaskDto> updateTeacherGoalStatus(
            @PathVariable Long taskId,
            @RequestBody(required = false) UpdateGoalStatusRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(goalTaskCenterService.updateTeacherGoalStatus(taskId, requestBody, request));
    }

    @GetMapping("/api/teacher/tasks/assignable-students")
    public ResponseEntity<List<AssignableStudentDto>> listAssignableStudents(HttpServletRequest request) {
        return ResponseEntity.ok(goalTaskCenterService.listAssignableStudents(request));
    }

    private ApiRequestException badRequest(String message) {
        return new ApiRequestException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
