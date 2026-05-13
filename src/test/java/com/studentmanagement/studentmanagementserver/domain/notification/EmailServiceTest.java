package com.studentmanagement.studentmanagementserver.domain.notification;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EmailServiceTest {

    @Test
    void sendTextEmailSkipsMailSenderWhenPasswordIsMissing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender, "noreply@global-vip.ca", "");

        emailService.sendTextEmail(
                Arrays.asList("student@example.com"),
                "Notification",
                "Body"
        );

        verifyNoInteractions(mailSender);
    }

    @Test
    void sendTextEmailUsesMailSenderWhenPasswordIsConfigured() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender, "noreply@global-vip.ca", "configured-password");

        emailService.sendTextEmail(
                Arrays.asList("student@example.com"),
                "Notification",
                "Body"
        );

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
