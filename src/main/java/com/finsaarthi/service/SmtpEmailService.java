package com.finsaarthi.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:}")
    private String mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuthEnabled;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean startTlsEnabled;

    @Value("${app.mail.from}")
    private String fromAddress;

    @PostConstruct
    void logSafeMailConfiguration() {
        log.info(
                "SMTP configuration loaded. host={}, port={}, username={}, from={}, authEnabled={}, startTlsEnabled={}",
                safeValue(mailHost),
                safeValue(mailPort),
                safeValue(mailUsername),
                safeValue(fromAddress),
                smtpAuthEnabled,
                startTlsEnabled
        );
    }

    @Override
    public void sendOtpEmail(String email, String otp, String purposeLabel) {
        validateMailConfiguration();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("FinSaarthi " + purposeLabel + " OTP");
        message.setText(buildEmailBody(otp, purposeLabel));

        try {
            mailSender.send(message);
            log.info("OTP email sent to {} for {}", email, purposeLabel);
        } catch (MailException ex) {
            log.error("Failed to send OTP email to {}: {}", email, ex.getMessage());
            throw new IllegalStateException(
                    "Unable to send OTP email at the moment. Please try again later."
            );
        }
    }

    private String buildEmailBody(String otp, String purposeLabel) {
        return """
                Your FinSaarthi %s OTP is: %s

                This OTP will expire shortly. Do not share it with anyone.
                """.formatted(purposeLabel, otp);
    }

    @Override
    public void sendPlainEmail(String email, String subject, String message) {
        validateMailConfiguration();

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromAddress);
        mailMessage.setTo(email);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        try {
            mailSender.send(mailMessage);
            log.info("Plain email sent to {}", email);
        } catch (MailException ex) {
            log.error("Failed to send plain email to {}: {}", email, ex.getMessage());
            throw new IllegalStateException(
                    "Unable to send email at the moment. Please try again later."
            );
        }
    }

    @Override
    public void sendEmailWithAttachment(
            String email,
            String subject,
            String message,
            byte[] attachment,
            String attachmentFilename
    ) {
        validateMailConfiguration();

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(message);
            helper.addAttachment(
                    attachmentFilename,
                    new ByteArrayResource(attachment),
                    "application/pdf"
            );

            mailSender.send(mimeMessage);
            log.info("Email with attachment sent to {}", email);
        } catch (MailException | MessagingException ex) {
            log.error("Failed to send email with attachment to {}: {}", email, ex.getMessage());
            throw new IllegalStateException(
                    "Unable to send email at the moment. Please try again later."
            );
        }
    }

    private void validateMailConfiguration() {
        requireConfigured(mailHost, "MAIL_HOST");
        requireConfigured(mailPort, "MAIL_PORT");
        requireConfigured(mailUsername, "MAIL_USERNAME");
        requireConfigured(fromAddress, "MAIL_FROM");
        requireConfigured(mailPassword, "MAIL_PASSWORD");

        if ("REPLACE_WITH_GMAIL_APP_PASSWORD".equals(mailPassword.trim())) {
            throw new IllegalStateException(
                    "MAIL_PASSWORD is still a placeholder. Replace it with your Gmail App Password."
            );
        }
    }

    private void requireConfigured(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    variableName + " is not configured. Set it in the environment before sending email."
            );
        }
    }

    private String safeValue(String value) {
        return (value == null || value.isBlank()) ? "<not-set>" : value;
    }
}
