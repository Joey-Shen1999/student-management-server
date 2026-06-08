package com.studentmanagement.studentmanagementserver.domain.notification;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceTest {

    @Test
    void sendTextEmailThrowsWhenPasswordIsMissing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender, "noreply@global-vip.ca", "");

        assertThrows(IllegalStateException.class, () -> emailService.sendTextEmail(
                Arrays.asList("student@example.com"),
                "Notification",
                "Body"
        ));
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

    @Test
    void sendTextEmailWithAttachmentsUsesMimeMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        EmailService emailService = new EmailService(mailSender, "noreply@global-vip.ca", "configured-password");

        emailService.sendTextEmail(
                Arrays.asList("student@example.com"),
                "Notification",
                "Body",
                Collections.singletonList(new EmailService.EmailAttachment(
                        "attachment.pdf",
                        "application/pdf",
                        new byte[] {1, 2, 3}
                ))
        );

        verify(mailSender).send(any(MimeMessage.class));
    }
}
