package com.finsaarthi.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BrevoEmailService implements EmailService {

    private static final URI BREVO_SEND_EMAIL_URI = URI.create("https://api.brevo.com/v3/smtp/email");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Set<Integer> TRANSIENT_STATUS_CODES = Set.of(408, 429, 500, 502, 503, 504);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${brevo.api-key:}")
    private String apiKey;

    @Value("${brevo.sender-email:}")
    private String senderEmail;

    @Value("${brevo.sender-name:}")
    private String senderName;

    @Value("${brevo.connect-timeout-seconds:10}")
    private long connectTimeoutSeconds;

    @Value("${brevo.read-timeout-seconds:20}")
    private long readTimeoutSeconds;

    @Value("${brevo.max-attempts:3}")
    private int maxAttempts;

    @Value("${brevo.retry-backoff-millis:750}")
    private long retryBackoffMillis;

    private HttpClient httpClient;

    @PostConstruct
    void initialize() {
        requireConfigured(apiKey, "BREVO_API_KEY");
        requireConfigured(senderEmail, "BREVO_SENDER_EMAIL");
        requireConfigured(senderName, "BREVO_SENDER_NAME");

        if (!EMAIL_PATTERN.matcher(senderEmail.trim()).matches()) {
            throw new IllegalStateException("BREVO_SENDER_EMAIL must be a valid email address.");
        }
        if (maxAttempts < 1) {
            throw new IllegalStateException("brevo.max-attempts must be at least 1.");
        }
        if (connectTimeoutSeconds < 1 || readTimeoutSeconds < 1) {
            throw new IllegalStateException("Brevo timeout values must be at least 1 second.");
        }

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();

        log.info(
                "Brevo email configuration loaded. senderEmail={}, senderName={}, connectTimeoutSeconds={}, readTimeoutSeconds={}, maxAttempts={}",
                maskEmail(senderEmail),
                senderName.trim(),
                connectTimeoutSeconds,
                readTimeoutSeconds,
                maxAttempts
        );
    }

    @Override
    public void sendOtpEmail(String email, String otp, String purposeLabel) {
        String recipient = normalizeRecipient(email);
        String safePurpose = purposeLabel == null || purposeLabel.isBlank() ? "Verification" : purposeLabel.trim();

        BrevoSendEmailRequest request = BrevoSendEmailRequest.builder()
                .sender(new BrevoSender(senderName.trim(), senderEmail.trim()))
                .to(List.of(new BrevoRecipient(recipient, null)))
                .subject("FinSaarthi " + safePurpose + " OTP")
                .textContent(buildOtpBody(otp, safePurpose))
                .tags(List.of("otp", safePurpose.toLowerCase(Locale.ROOT).replace(' ', '-')))
                .build();

        try {
            dispatchWithRetry(request, "otp-email", recipient);
        } catch (EmailDispatchException ex) {
            throw new IllegalStateException("Unable to send OTP email at the moment. Please try again later.");
        }
    }

    @Override
    public void sendPlainEmail(String email, String subject, String message) {
        String recipient = normalizeRecipient(email);

        BrevoSendEmailRequest request = BrevoSendEmailRequest.builder()
                .sender(new BrevoSender(senderName.trim(), senderEmail.trim()))
                .to(List.of(new BrevoRecipient(recipient, null)))
                .subject(safeSubject(subject))
                .textContent(message == null ? "" : message)
                .tags(List.of("notification"))
                .build();

        try {
            dispatchWithRetry(request, "plain-email", recipient);
        } catch (EmailDispatchException ex) {
            throw new IllegalStateException("Unable to send email at the moment. Please try again later.");
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
        String recipient = normalizeRecipient(email);

        if (attachment == null || attachment.length == 0) {
            throw new IllegalArgumentException("Attachment is required.");
        }
        if (attachmentFilename == null || attachmentFilename.isBlank()) {
            throw new IllegalArgumentException("Attachment filename is required.");
        }

        BrevoSendEmailRequest request = BrevoSendEmailRequest.builder()
                .sender(new BrevoSender(senderName.trim(), senderEmail.trim()))
                .to(List.of(new BrevoRecipient(recipient, null)))
                .subject(safeSubject(subject))
                .textContent(message == null ? "" : message)
                .attachment(List.of(new BrevoAttachment(
                        Base64.getEncoder().encodeToString(attachment),
                        attachmentFilename.trim()
                )))
                .tags(List.of("attachment", "application-pdf"))
                .build();

        try {
            dispatchWithRetry(request, "attachment-email", recipient);
        } catch (EmailDispatchException ex) {
            throw new IllegalStateException("Unable to send email at the moment. Please try again later.");
        }
    }

    private void dispatchWithRetry(BrevoSendEmailRequest payload, String operation, String recipient) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<String> response = sendRequest(payload);
                int statusCode = response.statusCode();
                String body = response.body();

                if (statusCode >= 200 && statusCode < 300) {
                    BrevoSendEmailResponse parsed = objectMapper.readValue(body, BrevoSendEmailResponse.class);
                    if (parsed == null || parsed.messageId() == null || parsed.messageId().isBlank()) {
                        log.error("Brevo returned a success status without messageId. operation={}, recipient={}, statusCode={}, body={}",
                                operation, maskEmail(recipient), statusCode, truncate(body));
                        throw new EmailDispatchException(false, "Missing messageId in Brevo response.");
                    }

                    log.info("Brevo email sent successfully. operation={}, recipient={}, messageId={}, attempt={}",
                            operation, maskEmail(recipient), parsed.messageId(), attempt);
                    return;
                }

                boolean transientFailure = TRANSIENT_STATUS_CODES.contains(statusCode);
                log.warn("Brevo email request failed. operation={}, recipient={}, statusCode={}, transientFailure={}, attempt={}, body={}",
                        operation, maskEmail(recipient), statusCode, transientFailure, attempt, truncate(body));

                if (!transientFailure || attempt == maxAttempts) {
                    throw new EmailDispatchException(transientFailure, "Brevo request failed with status " + statusCode);
                }
            } catch (EmailDispatchException ex) {
                if (!ex.transientFailure() || attempt == maxAttempts) {
                    throw ex;
                }
                log.warn("Retrying Brevo email after transient error. operation={}, recipient={}, attempt={}",
                        operation, maskEmail(recipient), attempt + 1);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new EmailDispatchException(true, "Brevo email send interrupted.", ex);
            } catch (IOException ex) {
                boolean transientFailure = isTransientTransportFailure(ex);
                log.warn("Brevo transport error. operation={}, recipient={}, transientFailure={}, attempt={}, error={}",
                        operation, maskEmail(recipient), transientFailure, attempt, ex.getMessage());

                if (!transientFailure || attempt == maxAttempts) {
                    throw new EmailDispatchException(transientFailure, "Brevo transport failure.", ex);
                }
            }

            sleepBeforeRetry(attempt);
        }

        throw new EmailDispatchException(false, "Brevo email send exhausted retries.");
    }

    private HttpResponse<String> sendRequest(BrevoSendEmailRequest payload) throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder(BREVO_SEND_EMAIL_URI)
                .timeout(Duration.ofSeconds(readTimeoutSeconds))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("api-key", apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private boolean isTransientTransportFailure(IOException ex) {
        return true;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(retryBackoffMillis * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EmailDispatchException(true, "Brevo retry interrupted.", ex);
        }
    }

    private String buildOtpBody(String otp, String purposeLabel) {
        return """
                Your FinSaarthi %s OTP is: %s

                This OTP will expire shortly. Do not share it with anyone.
                """.formatted(purposeLabel, otp);
    }

    private String normalizeRecipient(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("A valid recipient email address is required.");
        }
        return normalized;
    }

    private String safeSubject(String subject) {
        String normalized = subject == null ? "" : subject.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Email subject is required.");
        }
        return normalized;
    }

    private void requireConfigured(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    variableName + " is not configured. Set it in the environment before sending email."
            );
        }
    }

    private String maskEmail(String email) {
        String normalized = email == null ? "" : email.trim();
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }

        return normalized.charAt(0) + "***" + normalized.substring(atIndex);
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 300 ? value : value.substring(0, 300) + "...";
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record BrevoSender(String name, String email) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record BrevoRecipient(String email, String name) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record BrevoAttachment(String content, String name) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @lombok.Builder
    private record BrevoSendEmailRequest(
            BrevoSender sender,
            List<BrevoRecipient> to,
            String subject,
            String textContent,
            String htmlContent,
            List<BrevoAttachment> attachment,
            List<String> tags
    ) {
    }

    private record BrevoSendEmailResponse(String messageId) {
    }

    private static final class EmailDispatchException extends RuntimeException {
        private final boolean transientFailure;

        private EmailDispatchException(boolean transientFailure, String message) {
            super(message);
            this.transientFailure = transientFailure;
        }

        private EmailDispatchException(boolean transientFailure, String message, Throwable cause) {
            super(message, cause);
            this.transientFailure = transientFailure;
        }

        private boolean transientFailure() {
            return transientFailure;
        }
    }
}
