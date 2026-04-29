package com.studentmanagement.studentmanagementserver.domain.notification;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private static final String DEFAULT_SUBJECT = "Student Management Platform Email Test";
    private static final String DEFAULT_BODY =
            "This is a test email from the Student Management Platform notification module.";

    private final EmailService emailService;
    private final AuthSessionService authSessionService;
    private final String testRecipients;

    public NotificationController(EmailService emailService,
                                  AuthSessionService authSessionService,
                                  @Value("${app.notifications.test-recipients:shenchupeng0807@gmail.com}") String testRecipients) {
        this.emailService = emailService;
        this.authSessionService = authSessionService;
        this.testRecipients = testRecipients;
    }

    @PostMapping("/test-email")
    public ResponseEntity<Map<String, Object>> sendTestEmail(@RequestBody(required = false) EmailRequest requestBody,
                                                             HttpServletRequest request) {
        requireTeacherOrAdmin(request);

        List<String> recipients = parseRecipients(pick(requestBody == null ? null : requestBody.getTo(), testRecipients));
        String subject = pick(requestBody == null ? null : requestBody.getSubject(), DEFAULT_SUBJECT);
        String body = pick(requestBody == null ? null : requestBody.getBody(), DEFAULT_BODY);

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("to", recipients);
        response.put("subject", subject);

        try {
            emailService.sendTextEmail(recipients, subject, body);
            response.put("status", "sent");
            response.put("message", "Test email sent.");
            return ResponseEntity.ok(response);
        } catch (MailException ex) {
            log.warn("Failed to send test email.", ex);
            response.put("status", "failed");
            response.put("message", "Failed to send test email. Check SMTP host, username, password, TLS, and provider policy.");
            response.put("error", rootMessage(ex));
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        }
    }

    private void requireTeacherOrAdmin(HttpServletRequest request) {
        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.getRole() != UserRole.TEACHER && operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }
    }

    private String pick(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private List<String> parseRecipients(String rawRecipients) {
        List<String> recipients = new ArrayList<String>();
        if (!StringUtils.hasText(rawRecipients)) {
            return recipients;
        }

        String[] parts = rawRecipients.split("[,;\\s]+");
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                recipients.add(part.trim());
            }
        }
        return recipients;
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage();
    }
}
