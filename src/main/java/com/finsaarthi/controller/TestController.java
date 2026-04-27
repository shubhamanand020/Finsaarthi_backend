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

    @Value("${MAIL_USERNAME:}")
    private String configuredMailUsername;

    @GetMapping("/email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendTestEmail(
            @RequestParam(required = false) String to
    ) {
        String fallbackRecipient = configuredMailUsername == null ? "" : configuredMailUsername.trim();
        String recipient = (to == null || to.isBlank()) ? fallbackRecipient : to.trim();

        if (recipient.isBlank()) {
            throw new IllegalStateException(
                    "No recipient available for test email. Provide ?to=email@example.com or configure MAIL_USERNAME."
            );
        }

        log.info("Test email requested for recipient {}", recipient);

        emailService.sendPlainEmail(
                recipient,
                "FinSaarthi SMTP Test",
                "This is a test email from FinSaarthi to verify SMTP configuration."
        );

        return ResponseEntity.ok(ApiResponse.success("Test email sent successfully."));
    }
}
