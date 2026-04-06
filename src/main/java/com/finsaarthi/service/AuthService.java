package com.finsaarthi.service;

import com.finsaarthi.dto.request.ForgotPasswordRequest;
import com.finsaarthi.dto.request.LoginRequest;
import com.finsaarthi.dto.request.OtpVerificationRequest;
import com.finsaarthi.dto.request.RegisterRequest;
import com.finsaarthi.dto.request.UpdatePasswordRequest;
import com.finsaarthi.dto.response.AuthResponse;
import com.finsaarthi.dto.response.UserResponse;
import com.finsaarthi.entity.OtpVerification;
import com.finsaarthi.entity.User;
import com.finsaarthi.enums.OtpPurpose;
import com.finsaarthi.enums.Role;
import com.finsaarthi.exception.DuplicateEmailException;
import com.finsaarthi.exception.InvalidCredentialsException;
import com.finsaarthi.exception.ResourceNotFoundException;
import com.finsaarthi.repository.OtpVerificationRepository;
import com.finsaarthi.repository.UserRepository;
import com.finsaarthi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final CaptchaService captchaService;
    private final RateLimitService rateLimitService;

    @Value("${app.otp.expiration-minutes}")
    private int otpExpirationMinutes;

    @Value("${app.otp.max-attempts}")
    private int otpMaxAttempts;

    @Value("${app.otp.request-limit.max-requests:3}")
    private int otpRequestLimit;

    @Value("${app.otp.request-limit.window-minutes:10}")
    private int otpRequestWindowMinutes;

    @Value("${app.login.rate-limit.max-attempts:5}")
    private int loginRateLimit;

    @Value("${app.login.rate-limit.window-minutes:10}")
    private int loginRateLimitWindowMinutes;

    private final Map<String, Integer> otpAttempts = new ConcurrentHashMap<>();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .isVerified(false)
                .build();

        User saved = userRepository.save(user);
        generateAndSendOtp(email, OtpPurpose.REGISTER);
        log.info("New student registered with email: {}. Verification OTP sent.", email);

        return AuthResponse.builder()
                .user(mapToUserResponse(saved))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, String clientIpAddress) {
        String email = request.getEmail().toLowerCase().trim();
        String emailKey = loginEmailKey(email);
        String ipKey = loginIpKey(clientIpAddress);

        rateLimitService.assertWithinLimit(
                emailKey,
                loginRateLimit,
                Duration.ofMinutes(loginRateLimitWindowMinutes),
                "Too many login attempts for this email. Please try again later."
        );
        rateLimitService.assertWithinLimit(
                ipKey,
                loginRateLimit,
                Duration.ofMinutes(loginRateLimitWindowMinutes),
                "Too many login attempts from your network. Please try again later."
        );
        captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptchaInput());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(InvalidCredentialsException::new);

            if (user.getRole() == Role.STUDENT && !user.isVerified()) {
                throw new InvalidCredentialsException(
                        "Please verify your email before logging in."
                );
            }

            String token = jwtUtil.generateToken(userDetails);
            rateLimitService.reset(emailKey);
            rateLimitService.reset(ipKey);
            log.info("User logged in: {}", email);

            return AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .user(mapToUserResponse(user))
                    .build();

        } catch (BadCredentialsException ex) {
            recordFailedLogin(emailKey, ipKey);
            throw new InvalidCredentialsException();
        }
    }

    @Transactional
    public void verifyRegistrationOtp(OtpVerificationRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Registration OTP verification is only required for student accounts.");
        }
        if (user.isVerified()) {
            throw new IllegalArgumentException("This email is already verified.");
        }

        OtpVerification otpVerification = validateOtp(email, request.getOtp(), OtpPurpose.REGISTER);
        otpVerification.setVerified(true);
        user.setVerified(true);

        otpVerificationRepository.save(otpVerification);
        userRepository.save(user);
        otpAttempts.remove(attemptKey(email, OtpPurpose.REGISTER));
        log.info("Registration OTP verified for email: {}", email);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Password reset OTP is only available for student accounts.");
        }
        if (!user.isVerified()) {
            throw new IllegalArgumentException("Please verify your email before resetting your password.");
        }

        generateAndSendOtp(email, OtpPurpose.FORGOT_PASSWORD);
        log.info("Forgot password OTP sent for email: {}", email);
    }

    @Transactional
    public void verifyForgotPasswordOtp(OtpVerificationRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Password reset OTP is only available for student accounts.");
        }

        OtpVerification otpVerification = validateOtp(email, request.getOtp(), OtpPurpose.FORGOT_PASSWORD);
        otpVerification.setVerified(true);
        otpVerificationRepository.save(otpVerification);
        otpAttempts.remove(attemptKey(email, OtpPurpose.FORGOT_PASSWORD));
        log.info("Forgot password OTP verified for email: {}", email);
    }

    @Transactional
    public void updatePassword(UpdatePasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Password reset OTP is only available for student accounts.");
        }

        OtpVerification otpVerification = otpVerificationRepository
                .findFirstByEmailAndPurposeAndVerifiedTrueOrderByIdDesc(
                        email,
                        OtpPurpose.FORGOT_PASSWORD
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Please verify your forgot password OTP before updating your password."
                ));

        if (otpVerification.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpVerificationRepository.delete(otpVerification);
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpVerificationRepository.deleteByEmailAndPurpose(email, OtpPurpose.FORGOT_PASSWORD);
        otpAttempts.remove(attemptKey(email, OtpPurpose.FORGOT_PASSWORD));
        log.info("Password updated successfully for email: {}", email);
    }

    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .address(user.getAddress())
                .education(user.getEducation())
                .dateOfBirth(user.getDateOfBirth())
                .photo(user.getPhoto())
                .resume(user.getResume())
                .isVerified(user.isVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private void generateAndSendOtp(String email, OtpPurpose purpose) {
        String rateLimitKey = otpRequestKey(email, purpose);
        rateLimitService.assertWithinLimit(
                rateLimitKey,
                otpRequestLimit,
                Duration.ofMinutes(otpRequestWindowMinutes),
                "Too many OTP requests for this email. Please wait before requesting another OTP."
        );
        rateLimitService.recordAttempt(
                rateLimitKey,
                Duration.ofMinutes(otpRequestWindowMinutes)
        );
        otpVerificationRepository.deleteByEmailAndPurpose(email, purpose);

        String otp = generateOtp();
        OtpVerification otpVerification = OtpVerification.builder()
                .email(email)
                .otp(passwordEncoder.encode(otp))
                .purpose(purpose)
                .expiryTime(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .verified(false)
                .build();

        otpVerificationRepository.save(otpVerification);
        otpAttempts.remove(attemptKey(email, purpose));
        emailService.sendOtpEmail(email, otp, purposeLabel(purpose));
    }

    private OtpVerification validateOtp(String email, String rawOtp, OtpPurpose purpose) {
        OtpVerification otpVerification = otpVerificationRepository
                .findFirstByEmailAndPurposeOrderByIdDesc(email, purpose)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No OTP request found for this email."
                ));

        if (otpVerification.isVerified()) {
            throw new IllegalArgumentException("This OTP has already been used.");
        }

        if (otpVerification.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpVerificationRepository.delete(otpVerification);
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        if (!passwordEncoder.matches(rawOtp.trim(), otpVerification.getOtp())) {
            int attempts = otpAttempts.merge(attemptKey(email, purpose), 1, Integer::sum);
            if (attempts >= otpMaxAttempts) {
                otpVerificationRepository.deleteByEmailAndPurpose(email, purpose);
                otpAttempts.remove(attemptKey(email, purpose));
                throw new IllegalArgumentException(
                        "OTP verification failed too many times. Please request a new OTP."
                );
            }

            throw new IllegalArgumentException("Invalid OTP.");
        }

        return otpVerification;
    }

    private String generateOtp() {
        int otpValue = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otpValue);
    }

    private String purposeLabel(OtpPurpose purpose) {
        return switch (purpose) {
            case REGISTER -> "Registration";
            case FORGOT_PASSWORD -> "Forgot Password";
        };
    }

    private String attemptKey(String email, OtpPurpose purpose) {
        return email + ":" + purpose.name();
    }

    private void recordFailedLogin(String emailKey, String ipKey) {
        Duration window = Duration.ofMinutes(loginRateLimitWindowMinutes);
        rateLimitService.recordAttempt(emailKey, window);
        rateLimitService.recordAttempt(ipKey, window);
    }

    private String otpRequestKey(String email, OtpPurpose purpose) {
        return "otp-request:" + purpose.name() + ":" + email;
    }

    private String loginEmailKey(String email) {
        return "login-email:" + email;
    }

    private String loginIpKey(String clientIpAddress) {
        return "login-ip:" + (clientIpAddress == null || clientIpAddress.isBlank() ? "unknown" : clientIpAddress);
    }
}
