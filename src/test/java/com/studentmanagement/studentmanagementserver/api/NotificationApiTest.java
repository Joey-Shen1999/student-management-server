package com.studentmanagement.studentmanagementserver.api;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.UserRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionService authSessionService;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void teacherCanSendDefaultTestEmail_success() throws Exception {
        User teacher = userRepository.save(new User(
                "notification_teacher",
                passwordEncoder.encode("Teacher!234"),
                UserRole.TEACHER
        ));

        mockMvc.perform(post("/api/notifications/test-email")
                        .header("Authorization", bearerFor(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("sent"))
                .andExpect(jsonPath("$.to[0]").value("shenchupeng0807@gmail.com"))
                .andExpect(jsonPath("$.to[1]").value("joey.shen@globalielts.org"))
                .andExpect(jsonPath("$.to[2]").value("yong.chen@global-vip.ca"))
                .andExpect(jsonPath("$.subject").value("Student Management Platform Email Test"));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("noreply@global-vip.ca", message.getFrom());
        assertArrayEquals(new String[] {
                "shenchupeng0807@gmail.com",
                "joey.shen@globalielts.org",
                "yong.chen@global-vip.ca"
        }, message.getTo());
        assertEquals("Student Management Platform Email Test", message.getSubject());
        assertEquals(
                "This is a test email from the Student Management Platform notification module.",
                message.getText()
        );
    }

    @Test
    void studentCannotSendTestEmail_returns403() throws Exception {
        User student = userRepository.save(new User(
                "notification_student",
                passwordEncoder.encode("Student!234"),
                UserRole.STUDENT
        ));

        mockMvc.perform(post("/api/notifications/test-email")
                        .header("Authorization", bearerFor(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private String bearerFor(User user) {
        AuthSessionService.IssuedSession issuedSession = authSessionService.issueSession(user);
        return issuedSession.getTokenType() + " " + issuedSession.getAccessToken();
    }
}
