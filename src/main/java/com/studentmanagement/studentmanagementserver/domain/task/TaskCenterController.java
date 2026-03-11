package com.studentmanagement.studentmanagementserver.domain.task;

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

    private final TaskCenterService taskCenterService;

    public TaskCenterController(TaskCenterService taskCenterService) {
        this.taskCenterService = taskCenterService;
    }

    @GetMapping("/api/student/tasks")
    public ResponseEntity<GoalListResponseDto> listMyGoals(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "size", required = false) String size,
            HttpServletRequest request) {
        return ResponseEntity.ok(taskCenterService.listMyGoals(type, status, keyword, page, size, request));
    }

    @PatchMapping("/api/student/tasks/{taskId}/status")
    public ResponseEntity<GoalTaskDto> updateMyGoalStatus(
            @PathVariable Long taskId,
            @RequestBody(required = false) UpdateGoalStatusRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(taskCenterService.updateMyGoalStatus(taskId, requestBody, request));
    }

    @GetMapping("/api/teacher/tasks")
    public ResponseEntity<GoalListResponseDto> listTeacherGoals(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "studentId", required = false) String studentId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "size", required = false) String size,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                taskCenterService.listTeacherGoals(type, studentId, status, keyword, page, size, request)
        );
    }

    @PostMapping("/api/teacher/tasks/goals")
    public ResponseEntity<GoalTaskDto> createGoal(
            @RequestBody(required = false) CreateGoalRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(taskCenterService.createGoal(requestBody, request));
    }

    @PatchMapping("/api/teacher/tasks/{taskId}/status")
    public ResponseEntity<GoalTaskDto> updateTeacherGoalStatus(
            @PathVariable Long taskId,
            @RequestBody(required = false) UpdateGoalStatusRequestDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(taskCenterService.updateTeacherGoalStatus(taskId, requestBody, request));
    }

    @GetMapping("/api/teacher/tasks/assignable-students")
    public ResponseEntity<List<AssignableStudentDto>> listAssignableStudents(HttpServletRequest request) {
        return ResponseEntity.ok(taskCenterService.listAssignableStudents(request));
    }
}
