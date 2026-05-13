package com.studentmanagement.studentmanagementserver.domain.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String mailPassword;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from:noreply@global-vip.ca}") String fromAddress,
                        @Value("${spring.mail.password:}") String mailPassword) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.mailPassword = mailPassword;
    }

    public void sendTextEmail(Collection<String> recipients, String subject, String body) {
        List<String> validRecipients = validRecipients(recipients);
        if (validRecipients.isEmpty()) {
            throw new IllegalArgumentException("Email recipient is required.");
        }
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("Email subject is required.");
        }
        if (!StringUtils.hasText(body)) {
            throw new IllegalArgumentException("Email body is required.");
        }
        if (!StringUtils.hasText(mailPassword)) {
            log.warn("Skipping email send because spring.mail.password is not configured.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(validRecipients.toArray(new String[0]));
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    private List<String> validRecipients(Collection<String> recipients) {
        List<String> validRecipients = new ArrayList<String>();
        if (recipients == null) {
            return validRecipients;
        }
        for (String recipient : recipients) {
            if (StringUtils.hasText(recipient)) {
                validRecipients.add(recipient.trim());
            }
        }
        return validRecipients;
    }
}
