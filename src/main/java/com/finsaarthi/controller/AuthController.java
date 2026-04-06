package com.finsaarthi.controller;

import com.finsaarthi.dto.request.ForgotPasswordRequest;
import com.finsaarthi.dto.request.LoginRequest;
import com.finsaarthi.dto.request.OtpVerificationRequest;
import com.finsaarthi.dto.request.RegisterRequest;
import com.finsaarthi.dto.request.UpdatePasswordRequest;
import com.finsaarthi.dto.response.ApiResponse;
import com.finsaarthi.dto.response.AuthResponse;
import com.finsaarthi.dto.response.CaptchaResponse;
import com.finsaarthi.service.AuthService;
import com.finsaarthi.service.CaptchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/register
    // Public — registers a new student account and sends an email OTP
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        log.info("Registration request received for email: {}", request.getEmail());

        AuthResponse authResponse = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Account created successfully. Please verify your email using the OTP sent to you.",
                        authResponse
                ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/login
    // Public — authenticates credentials and returns a JWT
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        log.info("Login request received for email: {}", request.getEmail());

        AuthResponse authResponse = authService.login(
                request,
                extractClientIpAddress(httpServletRequest)
        );

        return ResponseEntity.ok(
                ApiResponse.success("Login successful.", authResponse)
        );
    }

    @GetMapping("/captcha")
    public ResponseEntity<ApiResponse<CaptchaResponse>> getCaptcha() {
        log.info("Captcha requested");

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Captcha generated successfully.",
                        captchaService.generateCaptcha()
                )
        );
    }

    @PostMapping("/verify-registration-otp")
    public ResponseEntity<ApiResponse<Void>> verifyRegistrationOtp(
            @Valid @RequestBody OtpVerificationRequest request
    ) {
        log.info("Registration OTP verification requested for email: {}", request.getEmail());

        authService.verifyRegistrationOtp(request);

        return ResponseEntity.ok(
                ApiResponse.success("Email verified successfully.")
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        log.info("Forgot password requested for email: {}", request.getEmail());

        authService.forgotPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success("Password reset OTP sent successfully.")
        );
    }

    @PostMapping("/verify-forgot-otp")
    public ResponseEntity<ApiResponse<Void>> verifyForgotOtp(
            @Valid @RequestBody OtpVerificationRequest request
    ) {
        log.info("Forgot password OTP verification requested for email: {}", request.getEmail());

        authService.verifyForgotPasswordOtp(request);

        return ResponseEntity.ok(
                ApiResponse.success("OTP verified successfully. You can now update your password.")
        );
    }

    @PostMapping("/update-password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        log.info("Password update requested for email: {}", request.getEmail());

        authService.updatePassword(request);

        return ResponseEntity.ok(
                ApiResponse.success("Password updated successfully.")
        );
    }

    private String extractClientIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
