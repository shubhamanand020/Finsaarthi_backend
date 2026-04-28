package com.finsaarthi.controller;

import com.finsaarthi.dto.response.ApiResponse;
import com.finsaarthi.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final EmailService emailService;

    @Value("${brevo.sender-email:}")
    private String configuredSenderEmail;

    @GetMapping("/email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendTestEmail(
            @RequestParam(required = false) String to
    ) {
        String fallbackRecipient = configuredSenderEmail == null ? "" : configuredSenderEmail.trim();
        String recipient = (to == null || to.isBlank()) ? fallbackRecipient : to.trim();

        if (recipient.isBlank()) {
            throw new IllegalStateException(
                    "No recipient available for test email. Provide ?to=email@example.com or configure BREVO_SENDER_EMAIL."
            );
        }

        log.info("Test email requested for recipient {}", recipient);

        emailService.sendPlainEmail(
                recipient,
                "FinSaarthi Brevo Test",
                "This is a test email from FinSaarthi to verify Brevo email delivery."
        );

        return ResponseEntity.ok(ApiResponse.success("Test email sent successfully."));
    }
}
