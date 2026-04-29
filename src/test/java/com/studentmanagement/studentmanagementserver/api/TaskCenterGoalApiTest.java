package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.task.GoalTask;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.GoalTaskRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskCenterGoalApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherStudentRepository teacherStudentRepository;

    @Autowired
    private GoalTaskRepository goalTaskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teacherCreateListAndUpdateGoal_success() throws Exception {
        Teacher teacher = createTeacherAccount("task_teacher_create", "Task Teacher");
        Student student = createStudentAccount("task_student_create", "Amy", "Chen", "Amy");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        MvcResult createResult = mockMvc.perform(post("/api/teacher/tasks/goals")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + "," +
                                "\"title\":\"完成 OUAC 账户注册\"," +
                                "\"description\":\"本周内完成并截图上传\"," +
                                "\"dueAt\":\"2026-03-15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("GOAL"))
                .andExpect(jsonPath("$.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.assignedStudentId").value(student.getId()))
                .andExpect(jsonPath("$.assignedByTeacherId").value(teacher.getId()))
                .andExpect(jsonPath("$.taskGroupId").isNotEmpty())
                .andExpect(jsonPath("$.dueAt").value("2026-03-15"))
                .andReturn();
        long goalId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .param("type", "GOAL")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(goalId))
                .andExpect(jsonPath("$.items[0].title").value("完成 OUAC 账户注册"));

        mockMvc.perform(patch("/api/teacher/tasks/{taskId}/status", goalId)
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\",\"progressNote\":\"已验收\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.progressNote").value("已验收"));
    }

    @Test
    void studentListAndUpdateStatus_successAndInvalidTransition400() throws Exception {
        Teacher teacher = createTeacherAccount("task_teacher_student_flow", "Teacher Flow");
        Student student = createStudentAccount("task_student_flow", "Lily", "Wang", "Lily");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        long goalId = createGoalAsTeacher(
                teacher.getUser(),
                student.getId(),
                "提交选校草案",
                "至少包含两所冲刺院校",
                "2026-03-20"
        );

        mockMvc.perform(get("/api/student/tasks")
                        .header("Authorization", bearerFor(student.getUser()))
                        .param("type", "GOAL")
                        .param("status", "ALL")
                        .param("page", "1")
                        .param("size", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(goalId));

        mockMvc.perform(patch("/api/student/tasks/{taskId}/status", goalId)
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"progressNote\":\"已开始\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.progressNote").value("已开始"))
                .andExpect(jsonPath("$.completedAt").isEmpty());

        mockMvc.perform(patch("/api/student/tasks/{taskId}/status", goalId)
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_STARTED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("status transition not allowed"));
    }

    @Test
    void studentCannotUpdateOtherStudentGoal_returns403() throws Exception {
        Teacher teacher = createTeacherAccount("task_teacher_ownership", "Teacher Ownership");
        Student owner = createStudentAccount("task_student_owner", "Owner", "One", "Owner");
        Student attacker = createStudentAccount("task_student_attacker", "Attacker", "Two", "Attacker");
        assignTeacherStudent(teacher, owner, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, attacker, TeacherStudentStatus.ACTIVE);

        long goalId = createGoalAsTeacher(
                teacher.getUser(),
                owner.getId(),
                "提交文书提纲",
                "文书提纲一页",
                null
        );

        mockMvc.perform(patch("/api/student/tasks/{taskId}/status", goalId)
                        .header("Authorization", bearerFor(attacker.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void teacherCannotUpdateOtherTeacherGoal_returns403() throws Exception {
        Teacher teacherA = createTeacherAccount("task_teacher_a", "Teacher A");
        Teacher teacherB = createTeacherAccount("task_teacher_b", "Teacher B");
        Student student = createStudentAccount("task_teacher_cross_student", "Cross", "Student", "Cross");
        assignTeacherStudent(teacherA, student, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacherB, student, TeacherStudentStatus.ACTIVE);

        long goalId = createGoalAsTeacher(
                teacherA.getUser(),
                student.getId(),
                "完成活动清单",
                "补充活动经历",
                null
        );

        mockMvc.perform(patch("/api/teacher/tasks/{taskId}/status", goalId)
                        .header("Authorization", bearerFor(teacherB.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanViewAndUpdateAllTeacherGoals_success() throws Exception {
        User admin = createAdmin("task_admin_global");
        Teacher teacher = createTeacherAccount("task_teacher_for_admin", "Teacher For Admin");
        Student student = createStudentAccount("task_student_for_admin", "Mia", "Li", "Mia");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);
        long goalId = createGoalAsTeacher(
                teacher.getUser(),
                student.getId(),
                "完成申请材料打包",
                "材料提交前复核",
                "2026-03-22"
        );

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(admin))
                        .param("type", "GOAL")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem((int) goalId)));

        mockMvc.perform(patch("/api/teacher/tasks/{taskId}/status", goalId)
                        .header("Authorization", bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"progressNote\":\"管理员代更新\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.progressNote").value("管理员代更新"));
    }

    @Test
    void listAssignableStudents_teacherCanSeeAllStudents() throws Exception {
        Teacher teacher = createTeacherAccount("task_teacher_assignable", "Teacher Assignable");
        Student activeStudent = createStudentAccount("task_student_active_assignable", "Active", "One", "Active");
        Student archivedStudent = createStudentAccount("task_student_archived_assignable", "Archived", "Two", "Archived");
        assignTeacherStudent(teacher, activeStudent, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, archivedStudent, TeacherStudentStatus.ARCHIVED);

        mockMvc.perform(get("/api/teacher/tasks/assignable-students")
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].studentId", hasItem(activeStudent.getId().intValue())))
                .andExpect(jsonPath("$[*].studentId", hasItem(archivedStudent.getId().intValue())));
    }

    @Test
    void listGoals_missingType_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("task_teacher_missing_type", "Teacher Missing Type");
        Student student = createStudentAccount("task_student_missing_type", "No", "Type", "NoType");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("type must be GOAL, INFO or DLL"));
    }

    @Test
    void createGoal_teacherCannotAssignUnrelatedStudent_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("task_teacher_need_relation", "Teacher Need Relation");
        Student student = createStudentAccount("task_student_need_relation", "Need", "Relation", "Need");

        mockMvc.perform(post("/api/teacher/tasks/goals")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + "," +
                                "\"title\":\"目标任务\"," +
                                "\"description\":\"老师无关系不能创建\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_ASSIGNABLE"));
    }

    @Test
    void createGoalWithInvalidDueAt_returns400() throws Exception {
        Teacher teacher = createTeacherAccount("task_teacher_bad_due", "Teacher Bad Due");
        Student student = createStudentAccount("task_student_bad_due", "Bad", "Due", "BadDue");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(post("/api/teacher/tasks/goals")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + "," +
                                "\"title\":\"目标任务\"," +
                                "\"description\":\"校验 dueAt\"," +
                                "\"dueAt\":\"2026/03/30\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("dueAt must be yyyy-mm-dd"));
    }

    @Test
    void teacherEditGoal_canUpdateStudentAndKeepStatusAndCompletedAt() throws Exception {
        Teacher teacher = createTeacherAccount("task_teacher_edit", "Task Teacher Edit");
        Student studentA = createStudentAccount("task_edit_student_a", "Student", "A", "A");
        Student studentB = createStudentAccount("task_edit_student_b", "Student", "B", "B");
        assignTeacherStudent(teacher, studentA, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentB, TeacherStudentStatus.ACTIVE);

        long goalId = createGoalAsTeacher(
                teacher.getUser(),
                studentA.getId(),
                "Original title",
                "Original description",
                "2026-04-01"
        );

        MvcResult statusResult = mockMvc.perform(patch("/api/teacher/tasks/{taskId}/status", goalId)
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\",\"progressNote\":\"done\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andReturn();
        String completedAtBeforeEdit = objectMapper.readTree(statusResult.getResponse().getContentAsString())
                .path("completedAt")
                .asText();

        MvcResult editResult = mockMvc.perform(patch("/api/teacher/tasks/goals/{goalId}", goalId)
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + studentB.getId() + "," +
                                "\"title\":\"Updated title\"," +
                                "\"description\":\"Updated description\"," +
                                "\"dueAt\":\"2026-04-15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(goalId))
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.dueAt").value("2026-04-15"))
                .andExpect(jsonPath("$.assignedStudentId").value(studentB.getId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressNote").value("done"))
                .andReturn();
        String completedAtAfterEdit = objectMapper.readTree(editResult.getResponse().getContentAsString())
                .path("completedAt")
                .asText();
        assertEquals(completedAtBeforeEdit, completedAtAfterEdit);

        mockMvc.perform(get("/api/teacher/tasks")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .param("type", "GOAL")
                        .param("studentId", String.valueOf(studentB.getId()))
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(goalId))
                .andExpect(jsonPath("$.items[0].title").value("Updated title"))
                .andExpect(jsonPath("$.items[0].assignedStudentId").value(studentB.getId()));
    }

    @Test
    void teacherCannotEditOtherTeacherGoal_returns403() throws Exception {
        Teacher teacherA = createTeacherAccount("task_goal_edit_teacher_a", "Task Edit Teacher A");
        Teacher teacherB = createTeacherAccount("task_goal_edit_teacher_b", "Task Edit Teacher B");
        Student student = createStudentAccount("task_goal_edit_student", "Edit", "Student", "EditStudent");
        assignTeacherStudent(teacherA, student, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacherB, student, TeacherStudentStatus.ACTIVE);

        long goalId = createGoalAsTeacher(
                teacherA.getUser(),
                student.getId(),
                "Teacher A Goal",
                "Owned by teacher A",
                "2026-04-12"
        );

        mockMvc.perform(patch("/api/teacher/tasks/goals/{goalId}", goalId)
                        .header("Authorization", bearerFor(teacherB.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + "," +
                                "\"title\":\"Hack title\"," +
                                "\"description\":\"Hack description\"," +
                                "\"dueAt\":\"2026-04-13\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void teacherCreateAndOverwriteGoalGroup_keepsExistingStatusFields() throws Exception {
        Teacher teacher = createTeacherAccount("task_group_teacher", "Task Group Teacher");
        Student studentA = createStudentAccount("task_group_student_a", "Group", "A", "GA");
        Student studentB = createStudentAccount("task_group_student_b", "Group", "B", "GB");
        Student studentC = createStudentAccount("task_group_student_c", "Group", "C", "GC");
        assignTeacherStudent(teacher, studentA, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentB, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentC, TeacherStudentStatus.ACTIVE);

        MvcResult createGroupResult = mockMvc.perform(post("/api/teacher/tasks/goal-groups")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Group title v1\"," +
                                "\"description\":\"Group description v1\"," +
                                "\"dueAt\":\"2026-04-20\"," +
                                "\"studentIds\":[" + studentA.getId() + "," + studentB.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andReturn();

        JsonNode createdGroupJson = objectMapper.readTree(createGroupResult.getResponse().getContentAsString());
        String taskGroupId = createdGroupJson.path("taskGroupId").asText();
        long goalAId = 0L;
        for (JsonNode item : createdGroupJson.path("items")) {
            if (item.path("assignedStudentId").asLong() == studentA.getId()) {
                goalAId = item.path("id").asLong();
                break;
            }
        }
        assertTrue(goalAId > 0L);

        mockMvc.perform(patch("/api/teacher/tasks/{taskId}/status", goalAId)
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\",\"progressNote\":\"group-done\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressNote").value("group-done"));

        MvcResult overwriteResult = mockMvc.perform(put("/api/teacher/tasks/goal-groups/{taskGroupId}", taskGroupId)
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Group title v2\"," +
                                "\"description\":\"Group description v2\"," +
                                "\"dueAt\":\"2026-04-28\"," +
                                "\"studentIds\":[" + studentA.getId() + "," + studentC.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskGroupId").value(taskGroupId))
                .andExpect(jsonPath("$.total").value(2))
                .andReturn();

        JsonNode overwriteJson = objectMapper.readTree(overwriteResult.getResponse().getContentAsString());
        boolean foundA = false;
        boolean foundC = false;
        for (JsonNode item : overwriteJson.path("items")) {
            long assignedStudentId = item.path("assignedStudentId").asLong();
            if (assignedStudentId == studentA.getId()) {
                foundA = true;
                assertEquals(goalAId, item.path("id").asLong());
                assertEquals("COMPLETED", item.path("status").asText());
                assertEquals("group-done", item.path("progressNote").asText());
                assertEquals("2026-04-28", item.path("dueAt").asText());
            } else if (assignedStudentId == studentC.getId()) {
                foundC = true;
                assertEquals("NOT_STARTED", item.path("status").asText());
                assertEquals("Group title v2", item.path("title").asText());
            } else if (assignedStudentId == studentB.getId()) {
                throw new AssertionError("studentB should have been removed from task group");
            }
        }
        assertTrue(foundA);
        assertTrue(foundC);
    }

    @Test
    void teacherCanViewGoalGroupStudentStatuses_success() throws Exception {
        Teacher teacher = createTeacherAccount("task_group_status_teacher", "Task Group Status Teacher");
        Student studentA = createStudentAccount("task_group_status_student_a", "Status", "A", "StatusA");
        Student studentB = createStudentAccount("task_group_status_student_b", "Status", "B", "StatusB");
        assignTeacherStudent(teacher, studentA, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacher, studentB, TeacherStudentStatus.ACTIVE);

        MvcResult createGroupResult = mockMvc.perform(post("/api/teacher/tasks/goal-groups")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Group status title\"," +
                                "\"description\":\"Group status description\"," +
                                "\"dueAt\":\"2026-04-30\"," +
                                "\"studentIds\":[" + studentA.getId() + "," + studentB.getId() + "]}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode createdGroupJson = objectMapper.readTree(createGroupResult.getResponse().getContentAsString());
        String taskGroupId = createdGroupJson.path("taskGroupId").asText();
        long goalAId = 0L;
        for (JsonNode item : createdGroupJson.path("items")) {
            if (item.path("assignedStudentId").asLong() == studentA.getId()) {
                goalAId = item.path("id").asLong();
                break;
            }
        }
        assertTrue(goalAId > 0L);

        mockMvc.perform(patch("/api/teacher/tasks/{taskId}/status", goalAId)
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\",\"progressNote\":\"status-done\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teacher/tasks/goal-groups/{taskGroupId}/students/status", taskGroupId)
                        .header("Authorization", bearerFor(teacher.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskGroupId").value(taskGroupId))
                .andExpect(jsonPath("$.title").value("Group status title"))
                .andExpect(jsonPath("$.totalAssigned").value(2))
                .andExpect(jsonPath("$.completedCount").value(1))
                .andExpect(jsonPath("$.pendingCount").value(1))
                .andExpect(jsonPath("$.students.length()").value(2))
                .andExpect(jsonPath("$.students[0].studentId").value(studentB.getId()))
                .andExpect(jsonPath("$.students[0].completed").value(false))
                .andExpect(jsonPath("$.students[1].studentId").value(studentA.getId()))
                .andExpect(jsonPath("$.students[1].completed").value(true))
                .andExpect(jsonPath("$.students[1].completedAt").isNotEmpty())
                .andExpect(jsonPath("$.students[1].progressNote").value("status-done"));
    }

    @Test
    void teacherCannotOverwriteOtherTeacherGoalGroup_returns403() throws Exception {
        Teacher teacherA = createTeacherAccount("task_group_owner_teacher", "Task Group Owner");
        Teacher teacherB = createTeacherAccount("task_group_other_teacher", "Task Group Other");
        Student student = createStudentAccount("task_group_forbidden_student", "Group", "Forbidden", "GF");
        assignTeacherStudent(teacherA, student, TeacherStudentStatus.ACTIVE);
        assignTeacherStudent(teacherB, student, TeacherStudentStatus.ACTIVE);

        MvcResult createGroupResult = mockMvc.perform(post("/api/teacher/tasks/goal-groups")
                        .header("Authorization", bearerFor(teacherA.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Owner group\"," +
                                "\"description\":\"Owner group desc\"," +
                                "\"dueAt\":\"2026-04-21\"," +
                                "\"studentIds\":[" + student.getId() + "]}"))
                .andExpect(status().isOk())
                .andReturn();
        String taskGroupId = objectMapper.readTree(createGroupResult.getResponse().getContentAsString())
                .path("taskGroupId")
                .asText();

        mockMvc.perform(put("/api/teacher/tasks/goal-groups/{taskGroupId}", taskGroupId)
                        .header("Authorization", bearerFor(teacherB.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hack\"," +
                                "\"description\":\"Hack\"," +
                                "\"dueAt\":\"2026-04-22\"," +
                                "\"studentIds\":[" + student.getId() + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void overwriteGoalGroup_notFound_returns404() throws Exception {
        Teacher teacher = createTeacherAccount("task_group_404_teacher", "Task Group 404");
        Student student = createStudentAccount("task_group_404_student", "Group", "NotFound", "GNF");
        assignTeacherStudent(teacher, student, TeacherStudentStatus.ACTIVE);

        mockMvc.perform(put("/api/teacher/tasks/goal-groups/{taskGroupId}", "group-not-exist")
                        .header("Authorization", bearerFor(teacher.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Any\"," +
                                "\"description\":\"Any\"," +
                                "\"dueAt\":\"2026-04-22\"," +
                                "\"studentIds\":[" + student.getId() + "]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private long createGoalAsTeacher(User teacherUser,
                                     Long studentId,
                                     String title,
                                     String description,
                                     String dueAt) throws Exception {
        String dueAtContent = dueAt == null ? "null" : "\"" + dueAt + "\"";
        MvcResult result = mockMvc.perform(post("/api/teacher/tasks/goals")
                        .header("Authorization", bearerFor(teacherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + studentId + "," +
                                "\"title\":\"" + title + "\"," +
                                "\"description\":\"" + description + "\"," +
                                "\"dueAt\":" + dueAtContent + "}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        long goalId = json.path("id").asLong();
        GoalTask saved = goalTaskRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal task not found after creation"));
        assertEquals(title, saved.getTitle());
        return goalId;
    }

    private User createAdmin(String username) {
        return userRepository.save(new User(username, passwordEncoder.encode("Admin!234"), UserRole.ADMIN));
    }

    private Teacher createTeacherAccount(String username, String displayName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Teacher!234"), UserRole.TEACHER));
        Teacher teacher = teacherRepository.save(new Teacher(user, displayName));
        assertTrue(teacher.getId() > 0);
        return teacher;
    }

    private Student createStudentAccount(String username, String firstName, String lastName, String nickName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Student!234"), UserRole.STUDENT));
        Student student = studentRepository.save(new Student(user, firstName, lastName, nickName));
        assertTrue(student.getId() > 0);
        return student;
    }

    private void assignTeacherStudent(Teacher teacher, Student student, TeacherStudentStatus status) {
        teacherStudentRepository.save(new TeacherStudent(teacher, student, status, "task-center-test-assignment"));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
