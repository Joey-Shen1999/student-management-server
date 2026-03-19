package com.studentmanagement.studentmanagementserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.student.StudentCourseRecord;
import com.studentmanagement.studentmanagementserver.domain.student.StudentIdentityFileStorageService;
import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolTranscriptStorageService;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentCourseRecordRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentProfileApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private StudentCourseRecordRepository studentCourseRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private StudentSchoolTranscriptStorageService transcriptStorageService;

    @SpyBean
    private StudentIdentityFileStorageService identityFileStorageService;

    @Test
    void getProfile_withStudentToken_returns200AndDefaultStructure() throws Exception {
        Student student = createStudentAccount("profile_get_student", "Amy", "Chen", "Amy");

        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalFirstName").value("Amy"))
                .andExpect(jsonPath("$.legalLastName").value("Chen"))
                .andExpect(jsonPath("$.preferredName").value("Amy"))
                .andExpect(jsonPath("$.firstName").value("Amy"))
                .andExpect(jsonPath("$.lastName").value("Chen"))
                .andExpect(jsonPath("$.nickName").value("Amy"))
                .andExpect(jsonPath("$.ap").value(false))
                .andExpect(jsonPath("$.address").isMap())
                .andExpect(jsonPath("$.schools").isArray())
                .andExpect(jsonPath("$.schools").isEmpty())
                .andExpect(jsonPath("$.identityFiles").isArray())
                .andExpect(jsonPath("$.identityFiles").isEmpty())
                .andExpect(jsonPath("$.otherCourses").isArray())
                .andExpect(jsonPath("$.otherCourses").isEmpty())
                .andExpect(jsonPath("$.schoolRecords").isArray())
                .andExpect(jsonPath("$.externalCourses").isArray());
    }

    @Test
    void putProfile_withValidBody_returns200AndPersists() throws Exception {
        Student student = createStudentAccount("profile_put_student", "Legacy", "Name", "Nick");

        Map<String, Object> payload = buildProfilePayload(
                "  Amy ",
                " Chen  ",
                "  A. Chen ",
                true,
                Arrays.asList(
                        buildSchool("MAIN", "A High School", "2023-09-01", null),
                        buildSchool("OTHER", "B High School", "2021-09-01", "2023-06-30")
                ),
                Arrays.asList(
                        buildCourse("Summer School C", "MHF4U", 93, 12, "2025-07-02", "2025-08-20")
                )
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalFirstName").value("Amy"))
                .andExpect(jsonPath("$.legalLastName").value("Chen"))
                .andExpect(jsonPath("$.preferredName").value("A. Chen"))
                .andExpect(jsonPath("$.address.streetAddress").value("123 Main St"))
                .andExpect(jsonPath("$.schools[0].schoolType").value("MAIN"))
                .andExpect(jsonPath("$.schools[0].schoolName").value("A High School"))
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"))
                .andExpect(jsonPath("$.ap").value(true));

        Student updatedStudent = studentRepository.findById(student.getId())
                .orElseThrow(() -> new RuntimeException("student not found after update"));
        assertEquals("Amy", updatedStudent.getFirstName());
        assertEquals("Chen", updatedStudent.getLastName());
        assertEquals("A. Chen", updatedStudent.getNickName());
        assertTrue(studentProfileRepository.findByStudent_Id(student.getId()).isPresent());

        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearerFor(updatedStudent.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("amy@example.com"))
                .andExpect(jsonPath("$.birthday").value("2008-06-01"))
                .andExpect(jsonPath("$.firstBoardingDate").value("2024-09-01"))
                .andExpect(jsonPath("$.schools[1].schoolType").value("OTHER"))
                .andExpect(jsonPath("$.otherCourses[0].mark").value(93))
                .andExpect(jsonPath("$.otherCourses[0].gradeLevel").value(12))
                .andExpect(jsonPath("$.externalCourses[0].courseCode").value("MHF4U"))
                .andExpect(jsonPath("$.schoolRecords[0].schoolType").value("MAIN"));
    }

    @Test
    void putProfile_withOtherGenderAndGenderOther_returnsSeparatedGenderFields() throws Exception {
        Student student = createStudentAccount("profile_gender_other_student", "Amy", "Chen", "Amy");

        Map<String, Object> payload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                new ArrayList<Map<String, Object>>(),
                new ArrayList<Map<String, Object>>()
        );
        payload.put("gender", "Other");
        payload.put("genderOther", "Non-binary");

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gender").value("Other"))
                .andExpect(jsonPath("$.genderOther").value("Non-binary"));

        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gender").value("Other"))
                .andExpect(jsonPath("$.genderOther").value("Non-binary"));
    }

    @Test
    void putProfile_withLegacyCombinedOtherGender_stillReturnsSeparatedGenderFields() throws Exception {
        Student student = createStudentAccount("profile_gender_legacy_student", "Amy", "Chen", "Amy");

        Map<String, Object> payload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                new ArrayList<Map<String, Object>>(),
                new ArrayList<Map<String, Object>>()
        );
        payload.put("gender", "Other: Prefer not to say");
        payload.remove("genderOther");

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gender").value("Other"))
                .andExpect(jsonPath("$.genderOther").value("Prefer not to say"));
    }

    @Test
    void putProfile_withOtherGenderButMissingGenderOther_returns400() throws Exception {
        Student student = createStudentAccount("profile_gender_missing_other_student", "Amy", "Chen", "Amy");

        Map<String, Object> payload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                new ArrayList<Map<String, Object>>(),
                new ArrayList<Map<String, Object>>()
        );
        payload.put("gender", "Other");
        payload.put("genderOther", "   ");

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("genderOther is required when gender is Other"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void putProfile_invalidDate_returns400() throws Exception {
        Student student = createStudentAccount("profile_invalid_date_student", "Amy", "Chen", "Amy");

        Map<String, Object> payload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                new ArrayList<Map<String, Object>>(),
                new ArrayList<Map<String, Object>>()
        );
        payload.put("birthday", "2008/06/01");

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("birthday must be yyyy-mm-dd"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void putProfile_aliasLists_schoolRecordsAndExternalCourses_supported() throws Exception {
        Student student = createStudentAccount("profile_alias_lists_student", "Amy", "Chen", "Amy");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("legalFirstName", "Amy");
        payload.put("legalLastName", "Chen");
        payload.put("preferredName", "Amy");
        payload.put("birthday", "2008-06-01");
        payload.put("firstBoardingDate", "2024-09-01");
        payload.put("ap", Boolean.FALSE);
        payload.put("address", buildAddress());
        payload.put("schoolRecords", Arrays.asList(
                buildSchool("MAIN", "A High School", "2023-09-01", null)
        ));
        payload.put("externalCourses", Arrays.asList(
                buildCourse("Summer School C", "MHF4U", 95, 12, "2025-07-02", "2025-08-20")
        ));

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolName").value("A High School"))
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"))
                .andExpect(jsonPath("$.schoolRecords[0].schoolName").value("A High School"))
                .andExpect(jsonPath("$.externalCourses[0].courseCode").value("MHF4U"));
    }

    @Test
    void putProfile_withSchoolBoard_roundTripAndReturnsBoardAlias() throws Exception {
        Student student = createStudentAccount("profile_school_board_student", "Amy", "Chen", "Amy");

        Map<String, Object> school = buildSchool("MAIN", "Unionville High School", "2023-09-01", null);
        school.put("schoolBoard", "  YRDSB ");

        Map<String, Object> payload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(school),
                new ArrayList<Map<String, Object>>()
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolBoard").value("YRDSB"))
                .andExpect(jsonPath("$.schools[0].boardName").value("YRDSB"))
                .andExpect(jsonPath("$.schoolRecords[0].schoolBoard").value("YRDSB"))
                .andExpect(jsonPath("$.schoolRecords[0].boardName").value("YRDSB"));

        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolBoard").value("YRDSB"))
                .andExpect(jsonPath("$.schools[0].boardName").value("YRDSB"));
    }

    @Test
    void putProfile_withSchoolBoardAliases_mapsToSchoolBoard() throws Exception {
        Student student = createStudentAccount("profile_school_board_alias_student", "Amy", "Chen", "Amy");

        Map<String, Object> school1 = buildSchool("MAIN", "A High School", "2023-09-01", null);
        school1.put("boardName", "TDSB");
        Map<String, Object> school2 = buildSchool("OTHER", "B Private School", "2021-09-01", "2023-06-30");
        school2.put("educationBureau", "私校");

        Map<String, Object> payload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(school1, school2),
                new ArrayList<Map<String, Object>>()
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolBoard").value("TDSB"))
                .andExpect(jsonPath("$.schools[0].boardName").value("TDSB"))
                .andExpect(jsonPath("$.schools[1].schoolBoard").value("私校"))
                .andExpect(jsonPath("$.schools[1].boardName").value("私校"));
    }

    @Test
    void putProfile_whenSchoolBoardOmitted_keepsExistingValue() throws Exception {
        Student student = createStudentAccount("profile_school_board_keep_student", "Amy", "Chen", "Amy");
        String bearer = bearerFor(student.getUser());

        Map<String, Object> firstSchool = buildSchool("MAIN", "A High School", "2023-09-01", null);
        firstSchool.put("schoolBoard", "YRDSB");
        Map<String, Object> firstPayload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(firstSchool),
                new ArrayList<Map<String, Object>>()
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(firstPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolBoard").value("YRDSB"));

        Map<String, Object> secondSchool = buildSchool("MAIN", "A High School", "2023-09-01", null);
        Map<String, Object> secondPayload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(secondSchool),
                new ArrayList<Map<String, Object>>()
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(secondPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolBoard").value("YRDSB"))
                .andExpect(jsonPath("$.schools[0].boardName").value("YRDSB"));
    }

    @Test
    void putProfile_withInvalidSchoolBoard_returns400() throws Exception {
        Student student = createStudentAccount("profile_school_board_invalid_student", "Amy", "Chen", "Amy");
        String bearer = bearerFor(student.getUser());

        Map<String, Object> blankBoardSchool = buildSchool("MAIN", "A High School", "2023-09-01", null);
        blankBoardSchool.put("schoolBoard", "   ");
        Map<String, Object> blankBoardPayload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(blankBoardSchool),
                new ArrayList<Map<String, Object>>()
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(blankBoardPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("schools[0].schoolBoard is invalid"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        StringBuilder longBoardBuilder = new StringBuilder();
        for (int i = 0; i < 65; i++) {
            longBoardBuilder.append('A');
        }
        Map<String, Object> longBoardSchool = buildSchool("MAIN", "A High School", "2023-09-01", null);
        longBoardSchool.put("schoolBoard", longBoardBuilder.toString());
        Map<String, Object> longBoardPayload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(longBoardSchool),
                new ArrayList<Map<String, Object>>()
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(longBoardPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("schools[0].schoolBoard is invalid"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void putProfile_withSchoolAddress_returnsAndPersistsSchoolAddress() throws Exception {
        Student student = createStudentAccount("profile_school_address_student", "Amy", "Chen", "Amy");

        Map<String, Object> payload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(
                        buildSchoolWithAddress(
                                "MAIN",
                                "Unionville High School",
                                "201 Town Centre Boulevard",
                                "Markham",
                                "Ontario",
                                "Canada",
                                "L3R 8G5",
                                "2023-09-01",
                                null
                        )
                ),
                new ArrayList<Map<String, Object>>()
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolName").value("Unionville High School"))
                .andExpect(jsonPath("$.schools[0].address.streetAddress").value("201 Town Centre Boulevard"))
                .andExpect(jsonPath("$.schools[0].address.city").value("Markham"))
                .andExpect(jsonPath("$.schools[0].address.state").value("Ontario"))
                .andExpect(jsonPath("$.schools[0].address.country").value("Canada"))
                .andExpect(jsonPath("$.schools[0].address.postal").value("L3R 8G5"))
                .andExpect(jsonPath("$.schools[0].streetAddress").value("201 Town Centre Boulevard"));

        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolName").value("Unionville High School"))
                .andExpect(jsonPath("$.schools[0].address.streetAddress").value("201 Town Centre Boulevard"))
                .andExpect(jsonPath("$.schools[0].address.city").value("Markham"))
                .andExpect(jsonPath("$.schools[0].address.state").value("Ontario"))
                .andExpect(jsonPath("$.schools[0].address.country").value("Canada"))
                .andExpect(jsonPath("$.schools[0].address.postal").value("L3R 8G5"));
    }

    @Test
    void putProfile_withExternalCourseAddress_returnsAndPersistsCourseAddress() throws Exception {
        Student student = createStudentAccount("profile_course_address_student", "Amy", "Chen", "Amy");

        Map<String, Object> payload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(
                        buildSchool("MAIN", "A High School", "2023-09-01", null)
                ),
                Arrays.asList(
                        buildCourseWithAddress(
                                "Bayview Secondary Night School",
                                "1000 Finch Ave W",
                                "Toronto",
                                "Ontario",
                                "Canada",
                                "M3J 2V5",
                                "MHF4U",
                                95,
                                12,
                                "2025-07-02",
                                "2025-08-20"
                        )
                )
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherCourses[0].schoolName").value("Bayview Secondary Night School"))
                .andExpect(jsonPath("$.otherCourses[0].address.streetAddress").value("1000 Finch Ave W"))
                .andExpect(jsonPath("$.otherCourses[0].address.city").value("Toronto"))
                .andExpect(jsonPath("$.otherCourses[0].address.state").value("Ontario"))
                .andExpect(jsonPath("$.otherCourses[0].address.country").value("Canada"))
                .andExpect(jsonPath("$.otherCourses[0].address.postal").value("M3J 2V5"))
                .andExpect(jsonPath("$.otherCourses[0].streetAddress").value("1000 Finch Ave W"));

        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherCourses[0].schoolName").value("Bayview Secondary Night School"))
                .andExpect(jsonPath("$.otherCourses[0].address.streetAddress").value("1000 Finch Ave W"))
                .andExpect(jsonPath("$.otherCourses[0].address.city").value("Toronto"))
                .andExpect(jsonPath("$.otherCourses[0].address.state").value("Ontario"))
                .andExpect(jsonPath("$.otherCourses[0].address.country").value("Canada"))
                .andExpect(jsonPath("$.otherCourses[0].address.postal").value("M3J 2V5"));
    }

    @Test
    void identityFiles_uploadThreeSizes_thenPutDeleteOne_keepsFinalState() throws Exception {
        Student student = createStudentAccount("profile_identity_file_student", "Amy", "Chen", "Amy");
        String bearer = bearerFor(student.getUser());

        Map<String, Object> payload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(buildSchool("MAIN", "Unionville High School", "2023-09-01", null)),
                new ArrayList<Map<String, Object>>()
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk());

        MockMultipartFile file1Mb = new MockMultipartFile(
                "file",
                "id-1mb.pdf",
                "application/pdf",
                new byte[1 * 1024 * 1024]
        );
        mockMvc.perform(multipart("/api/student/profile/identity-files")
                        .file(file1Mb)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasIdentityFile").value(true))
                .andExpect(jsonPath("$.identityFiles.length()").value(1));

        MockMultipartFile file20Mb = new MockMultipartFile(
                "identity",
                "id-20mb.pdf",
                "application/pdf",
                new byte[20 * 1024 * 1024]
        );
        mockMvc.perform(multipart("/api/student/profile/identity-files")
                        .file(file20Mb)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasIdentityFile").value(true))
                .andExpect(jsonPath("$.identityFiles.length()").value(2));

        MockMultipartFile file45Mb = new MockMultipartFile(
                "file",
                "id-45mb.pdf",
                "application/pdf",
                new byte[45 * 1024 * 1024]
        );
        mockMvc.perform(multipart("/api/student/profile/identity-files")
                        .file(file45Mb)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasIdentityFile").value(true))
                .andExpect(jsonPath("$.identityFiles.length()").value(3));

        MvcResult profileResult = mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identityFiles.length()").value(3))
                .andReturn();
        JsonNode profile = objectMapper.readTree(profileResult.getResponse().getContentAsString());
        JsonNode identityFiles = profile.path("identityFiles");
        long removedId = identityFiles.path(1).path("id").asLong();

        List<Map<String, Object>> finalIdentityFiles = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < identityFiles.size(); i++) {
            JsonNode node = identityFiles.path(i);
            if (node.path("id").asLong() == removedId) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", node.path("id").asLong());
            item.put("storageKey", node.path("storageKey").isNull() ? null : node.path("storageKey").asText());
            item.put("identityFileName", node.path("identityFileName").isNull() ? null : node.path("identityFileName").asText());
            item.put(
                    "identityFileContentType",
                    node.path("identityFileContentType").isNull() ? null : node.path("identityFileContentType").asText()
            );
            item.put("identityFileSizeBytes", node.path("identityFileSizeBytes").isNull() ? null : node.path("identityFileSizeBytes").asLong());
            item.put("identityFileUploadedAt", node.path("identityFileUploadedAt").isNull() ? null : node.path("identityFileUploadedAt").asText());
            finalIdentityFiles.add(item);
        }

        Map<String, Object> putPayload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(buildSchool("MAIN", "Unionville High School", "2023-09-01", null)),
                new ArrayList<Map<String, Object>>()
        );
        putPayload.put("identityFiles", finalIdentityFiles);

        reset(identityFileStorageService);

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(putPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identityFiles.length()").value(2));

        MvcResult afterDeleteResult = mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identityFiles.length()").value(2))
                .andReturn();
        JsonNode afterDelete = objectMapper.readTree(afterDeleteResult.getResponse().getContentAsString());
        for (JsonNode identityFile : afterDelete.path("identityFiles")) {
            assertTrue(identityFile.path("id").asLong() != removedId);
        }

        verify(identityFileStorageService, atLeastOnce()).deleteRequired(anyString());
    }

    @Test
    void identityFiles_upload60Mb_returns413() throws Exception {
        Student student = createStudentAccount("profile_identity_file_413_student", "Amy", "Chen", "Amy");

        MockMultipartFile tooLarge = new MockMultipartFile(
                "file",
                "id-60mb.pdf",
                "application/pdf",
                new byte[60 * 1024 * 1024]
        );

        mockMvc.perform(multipart("/api/student/profile/identity-files")
                        .file(tooLarge)
                        .header("Authorization", bearerFor(student.getUser())))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"))
                .andExpect(jsonPath("$.message").value("Max upload size is 50MB"));
    }

    @Test
    void schoolTranscript_uploadTwice_getReturnsTwo_andDownloadByIdWorks() throws Exception {
        Student student = createStudentAccount("profile_transcript_student", "Amy", "Chen", "Amy");
        String bearer = bearerFor(student.getUser());

        Map<String, Object> payload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(
                        buildSchool("MAIN", "Unionville High School", "2023-09-01", null)
                ),
                new ArrayList<Map<String, Object>>()
        );

        MvcResult saveResult = mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolRecordId").isNumber())
                .andReturn();

        JsonNode saveJson = objectMapper.readTree(saveResult.getResponse().getContentAsString());
        long schoolRecordId = saveJson.path("schools").path(0).path("schoolRecordId").asLong();

        byte[] transcriptBytes1 = "mock transcript payload 1".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile transcript1 = new MockMultipartFile(
                "transcript",
                "unionville-transcript-1.pdf",
                "application/pdf",
                transcriptBytes1
        );

        MvcResult uploadResult1 = mockMvc.perform(
                        multipart("/api/student/profile/schools/{schoolRecordId}/transcript", schoolRecordId)
                                .file(transcript1)
                                .header("Authorization", bearer)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolRecordId").value(schoolRecordId))
                .andExpect(jsonPath("$.transcriptFileName").value("unionville-transcript-1.pdf"))
                .andExpect(jsonPath("$.hasTranscript").value(true))
                .andExpect(jsonPath("$.transcripts.length()").value(1))
                .andReturn();
        long transcriptId1 = objectMapper.readTree(uploadResult1.getResponse().getContentAsString())
                .path("transcripts")
                .path(0)
                .path("id")
                .asLong();

        byte[] transcriptBytes2 = "mock transcript payload 2".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile transcript2 = new MockMultipartFile(
                "file",
                "unionville-transcript-2.pdf",
                "application/pdf",
                transcriptBytes2
        );

        MvcResult uploadResult2 = mockMvc.perform(
                        multipart("/api/student/profile/schools/{schoolRecordId}/transcript", schoolRecordId)
                                .file(transcript2)
                                .header("Authorization", bearer)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolRecordId").value(schoolRecordId))
                .andExpect(jsonPath("$.transcriptFileName").value("unionville-transcript-2.pdf"))
                .andExpect(jsonPath("$.hasTranscript").value(true))
                .andExpect(jsonPath("$.transcripts.length()").value(2))
                .andReturn();
        long transcriptId2 = objectMapper.readTree(uploadResult2.getResponse().getContentAsString())
                .path("transcripts")
                .path(0)
                .path("id")
                .asLong();

        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].transcripts.length()").value(2))
                .andExpect(jsonPath("$.schools[0].hasTranscript").value(true))
                .andExpect(jsonPath("$.schools[0].transcriptFileName").value("unionville-transcript-2.pdf"))
                .andExpect(jsonPath("$.schools[0].transcripts[0].id").value(transcriptId2))
                .andExpect(jsonPath("$.schools[0].transcripts[0].transcriptFileName").value("unionville-transcript-2.pdf"))
                .andExpect(jsonPath("$.schools[0].transcripts[1].id").value(transcriptId1))
                .andExpect(jsonPath("$.schools[0].transcripts[1].transcriptFileName").value("unionville-transcript-1.pdf"));

        mockMvc.perform(get("/api/student/profile/schools/{schoolRecordId}/transcript", schoolRecordId)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().bytes(transcriptBytes2));

        mockMvc.perform(get("/api/student/profile/schools/{schoolRecordId}/transcripts/{transcriptId}",
                                schoolRecordId,
                                transcriptId1)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(transcriptBytes1));
    }

    @Test
    void putProfile_transcriptFinalState_deletesMissingTranscriptAndStorage() throws Exception {
        Student student = createStudentAccount("profile_transcript_put_delete_student", "Amy", "Chen", "Amy");
        String bearer = bearerFor(student.getUser());

        Map<String, Object> payload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(buildSchool("MAIN", "Unionville High School", "2023-09-01", null)),
                new ArrayList<Map<String, Object>>()
        );

        MvcResult saveResult = mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andReturn();
        long schoolRecordId = objectMapper.readTree(saveResult.getResponse().getContentAsString())
                .path("schools")
                .path(0)
                .path("schoolRecordId")
                .asLong();

        MockMultipartFile transcript1 = new MockMultipartFile(
                "file",
                "delete-target-1.pdf",
                "application/pdf",
                "delete-target-1".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile transcript2 = new MockMultipartFile(
                "file",
                "keep-target-2.pdf",
                "application/pdf",
                "keep-target-2".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/student/profile/schools/{schoolRecordId}/transcript", schoolRecordId)
                        .file(transcript1)
                        .header("Authorization", bearer))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/api/student/profile/schools/{schoolRecordId}/transcript", schoolRecordId)
                        .file(transcript2)
                        .header("Authorization", bearer))
                .andExpect(status().isOk());

        MvcResult profileWithTwoResult = mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].transcripts.length()").value(2))
                .andReturn();

        JsonNode profileWithTwo = objectMapper.readTree(profileWithTwoResult.getResponse().getContentAsString());
        JsonNode schoolNode = profileWithTwo.path("schools").path(0);
        JsonNode keptTranscript = schoolNode.path("transcripts").path(0);

        Map<String, Object> schoolPayload = buildSchool("MAIN", "Unionville High School", "2023-09-01", null);
        schoolPayload.put("schoolRecordId", schoolNode.path("schoolRecordId").asLong());
        List<Map<String, Object>> finalTranscripts = new ArrayList<Map<String, Object>>();
        Map<String, Object> keptTranscriptPayload = new LinkedHashMap<String, Object>();
        keptTranscriptPayload.put("id", keptTranscript.path("id").asLong());
        keptTranscriptPayload.put("transcriptFileName", keptTranscript.path("transcriptFileName").asText());
        keptTranscriptPayload.put("transcriptSizeBytes", keptTranscript.path("transcriptSizeBytes").asLong());
        keptTranscriptPayload.put("transcriptUploadedAt", keptTranscript.path("transcriptUploadedAt").asText());
        finalTranscripts.add(keptTranscriptPayload);
        schoolPayload.put("transcripts", finalTranscripts);

        Map<String, Object> putPayload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(schoolPayload),
                new ArrayList<Map<String, Object>>()
        );

        reset(transcriptStorageService);

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(putPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].transcripts.length()").value(1))
                .andExpect(jsonPath("$.schools[0].transcriptFileName").value("keep-target-2.pdf"));

        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].transcripts.length()").value(1))
                .andExpect(jsonPath("$.schools[0].transcriptFileName").value("keep-target-2.pdf"));

        verify(transcriptStorageService, atLeastOnce()).deleteRequired(anyString());
    }

    @Test
    void schoolTranscript_nonOwnerStudentUpload_returns403() throws Exception {
        Student owner = createStudentAccount("profile_transcript_owner", "Amy", "Chen", "Amy");
        Student attacker = createStudentAccount("profile_transcript_attacker", "Bob", "Li", "Bob");

        String ownerBearer = bearerFor(owner.getUser());
        String attackerBearer = bearerFor(attacker.getUser());

        Map<String, Object> payload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(buildSchool("MAIN", "Unionville High School", "2023-09-01", null)),
                new ArrayList<Map<String, Object>>()
        );

        MvcResult saveResult = mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", ownerBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(payload)))
                .andExpect(status().isOk())
                .andReturn();
        long schoolRecordId = objectMapper.readTree(saveResult.getResponse().getContentAsString())
                .path("schools")
                .path(0)
                .path("schoolRecordId")
                .asLong();

        MockMultipartFile transcript = new MockMultipartFile(
                "file",
                "not-owner.pdf",
                "application/pdf",
                "not-owner".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/student/profile/schools/{schoolRecordId}/transcript", schoolRecordId)
                        .file(transcript)
                        .header("Authorization", attackerBearer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getProfile_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/student/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthenticated."))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getProfile_withTeacherToken_returns403() throws Exception {
        User teacher = userRepository.save(
                new User("profile_teacher_forbidden", passwordEncoder.encode("Teacher!234"), UserRole.TEACHER)
        );

        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", bearerFor(teacher)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden: student role required."))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void putProfile_saveWithTwoThenOneCourse_removesOldExtraCourse() throws Exception {
        Student student = createStudentAccount("profile_replace_courses_student", "Amy", "Chen", "Amy");
        String bearer = bearerFor(student.getUser());

        Map<String, Object> firstPayload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(
                        buildSchool("MAIN", "A High School", "2023-09-01", null)
                ),
                Arrays.asList(
                        buildCourse("ABC Private School", "MHF4U", 93, 12, "2025-02-01", "2025-06-30"),
                        buildCourse("Night School", "ENG4U", 90, 12, "2025-02-01", "2025-06-30")
                )
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(firstPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"))
                .andExpect(jsonPath("$.otherCourses[1].courseCode").value("ENG4U"));

        Map<String, Object> secondPayload = buildProfilePayload(
                "Amy",
                "Chen",
                "Amy",
                false,
                Arrays.asList(
                        buildSchool("MAIN", "A High School", "2023-09-01", null)
                ),
                Arrays.asList(
                        buildCourse("ABC Private School", "MHF4U", 95, 12, "2025-02-01", "2025-06-30")
                )
        );

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(secondPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherCourses").isArray())
                .andExpect(jsonPath("$.otherCourses[0].courseCode").value("MHF4U"));

        List<StudentCourseRecord> records = studentCourseRecordRepository.findByStudent_IdOrderByIdAsc(student.getId());
        assertEquals(1, records.size());
        assertEquals("MHF4U", records.get(0).getCourseCode());
        assertEquals(Integer.valueOf(95), records.get(0).getMark());
    }

    private Student createStudentAccount(String username, String firstName, String lastName, String nickName) {
        User user = userRepository.save(new User(username, passwordEncoder.encode("Student!234"), UserRole.STUDENT));
        return studentRepository.save(new Student(user, firstName, lastName, nickName));
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }

    private String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private Map<String, Object> buildProfilePayload(String legalFirstName,
                                                    String legalLastName,
                                                    String preferredName,
                                                    Boolean ap,
                                                    List<Map<String, Object>> schools,
                                                    List<Map<String, Object>> courses) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("legalFirstName", legalFirstName);
        payload.put("legalLastName", legalLastName);
        payload.put("preferredName", preferredName);
        payload.put("gender", "Female");
        payload.put("birthday", "2008-06-01");
        payload.put("phone", "(647) 111-2222");
        payload.put("email", "amy@example.com");
        payload.put("statusInCanada", "PR");
        payload.put("citizenship", "Canada");
        payload.put("firstLanguage", "English");
        payload.put("firstBoardingDate", "2024-09-01");
        payload.put("oenNumber", "123456789");
        payload.put("ib", "IB DP");
        payload.put("ap", ap);
        payload.put("address", buildAddress());
        payload.put("schools", schools);
        payload.put("otherCourses", courses);
        return payload;
    }

    private Map<String, Object> buildAddress() {
        Map<String, Object> address = new LinkedHashMap<String, Object>();
        address.put("streetAddress", "123 Main St");
        address.put("streetAddressLine2", "Unit 5");
        address.put("city", "Toronto");
        address.put("state", "ON");
        address.put("country", "Canada");
        address.put("postal", "M1M1M1");
        return address;
    }

    private Map<String, Object> buildSchool(String schoolType,
                                            String schoolName,
                                            String startTime,
                                            String endTime) {
        Map<String, Object> school = new LinkedHashMap<String, Object>();
        school.put("schoolType", schoolType);
        school.put("schoolName", schoolName);
        school.put("startTime", startTime);
        school.put("endTime", endTime);
        return school;
    }

    private Map<String, Object> buildSchoolWithAddress(String schoolType,
                                                       String schoolName,
                                                       String streetAddress,
                                                       String city,
                                                       String state,
                                                       String country,
                                                       String postal,
                                                       String startTime,
                                                       String endTime) {
        Map<String, Object> school = buildSchool(schoolType, schoolName, startTime, endTime);
        Map<String, Object> address = new LinkedHashMap<String, Object>();
        address.put("streetAddress", streetAddress);
        address.put("city", city);
        address.put("state", state);
        address.put("country", country);
        address.put("postal", postal);
        school.put("address", address);
        return school;
    }

    private Map<String, Object> buildCourse(String schoolName,
                                            String courseCode,
                                            Integer mark,
                                            Integer gradeLevel,
                                            String startTime,
                                            String endTime) {
        Map<String, Object> course = new LinkedHashMap<String, Object>();
        course.put("schoolName", schoolName);
        course.put("courseCode", courseCode);
        course.put("mark", mark);
        course.put("gradeLevel", gradeLevel);
        course.put("startTime", startTime);
        course.put("endTime", endTime);
        return course;
    }

    private Map<String, Object> buildCourseWithAddress(String schoolName,
                                                       String streetAddress,
                                                       String city,
                                                       String state,
                                                       String country,
                                                       String postal,
                                                       String courseCode,
                                                       Integer mark,
                                                       Integer gradeLevel,
                                                       String startTime,
                                                       String endTime) {
        Map<String, Object> course = buildCourse(schoolName, courseCode, mark, gradeLevel, startTime, endTime);
        Map<String, Object> address = new LinkedHashMap<String, Object>();
        address.put("streetAddress", streetAddress);
        address.put("city", city);
        address.put("state", state);
        address.put("country", country);
        address.put("postal", postal);
        course.put("address", address);
        return course;
    }
}
