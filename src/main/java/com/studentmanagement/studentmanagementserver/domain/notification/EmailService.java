package com.studentmanagement.studentmanagementserver.domain.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
        sendTextEmail(recipients, subject, body, Collections.<EmailAttachment>emptyList());
    }

    public void sendTextEmail(Collection<String> recipients,
                              String subject,
                              String body,
                              Collection<EmailAttachment> attachments) {
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
            throw new IllegalStateException("SMTP password is not configured; email delivery is disabled.");
        }

        Collection<EmailAttachment> safeAttachments =
                attachments == null ? Collections.<EmailAttachment>emptyList() : attachments;
        if (safeAttachments.isEmpty()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(validRecipients.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(validRecipients.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(body, false);
            int attachmentIndex = 0;
            for (EmailAttachment attachment : safeAttachments) {
                if (attachment == null || attachment.isEmpty()) {
                    continue;
                }
                String attachmentName = attachment.getFileName();
                if (!StringUtils.hasText(attachmentName)) {
                    attachmentName = "attachment-" + (++attachmentIndex);
                }
                helper.addAttachment(
                        attachmentName,
                        new ByteArrayResource(attachment.getContent()),
                        attachment.getContentType()
                );
            }
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to prepare email with attachments", ex);
        }
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

    public static class EmailAttachment {
        private final String fileName;
        private final String contentType;
        private final byte[] content;

        public EmailAttachment(String fileName, String contentType, byte[] content) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.content = content == null ? new byte[0] : content.clone();
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getContent() {
            return content.clone();
        }

        private boolean isEmpty() {
            return content == null || content.length == 0;
        }
    }
}
