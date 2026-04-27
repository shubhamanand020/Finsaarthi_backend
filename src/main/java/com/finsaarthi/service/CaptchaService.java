package com.finsaarthi.service;

import com.finsaarthi.dto.response.CaptchaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CaptchaService {

    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CAPTCHA_LENGTH = 5;
    private static final int IMAGE_WIDTH = 180;
    private static final int IMAGE_HEIGHT = 60;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, CaptchaEntry> captchas = new ConcurrentHashMap<>();

    @Value("${app.captcha.expiration-minutes:5}")
    private int captchaExpirationMinutes;

    public CaptchaResponse generateCaptcha() {
        clearExpiredCaptchas();

        String captchaId = UUID.randomUUID().toString();
        String captchaText = randomCaptchaText();
        captchas.put(
                captchaId,
                new CaptchaEntry(captchaText, LocalDateTime.now().plusMinutes(captchaExpirationMinutes))
        );

        return CaptchaResponse.builder()
                .captchaId(captchaId)
                .image(renderCaptchaImage(captchaText))
                .build();
    }

    public void validateCaptcha(String captchaId, String captchaInput) {
        clearExpiredCaptchas();

        if (captchaId == null || captchaId.isBlank()) {
            throw new IllegalArgumentException("Captcha ID is required.");
        }
        if (captchaInput == null || captchaInput.isBlank()) {
            throw new IllegalArgumentException("Captcha input is required.");
        }

        CaptchaEntry captchaEntry = captchas.remove(captchaId);
        if (captchaEntry == null) {
            throw new IllegalArgumentException("Captcha is invalid or has expired. Please refresh and try again.");
        }

        if (captchaEntry.expiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Captcha has expired. Please refresh and try again.");
        }

        if (!captchaEntry.answer().equalsIgnoreCase(captchaInput.trim())) {
            throw new IllegalArgumentException("Incorrect captcha. Please try again.");
        }
    }

    private void clearExpiredCaptchas() {
        LocalDateTime now = LocalDateTime.now();
        captchas.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private String randomCaptchaText() {
        StringBuilder builder = new StringBuilder(CAPTCHA_LENGTH);
        for (int index = 0; index < CAPTCHA_LENGTH; index++) {
            builder.append(CAPTCHA_CHARS.charAt(SECURE_RANDOM.nextInt(CAPTCHA_CHARS.length())));
        }
        return builder.toString();
    }

    private String renderCaptchaImage(String text) {
        try {
            StringBuilder svg = new StringBuilder();
            svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='")
                    .append(IMAGE_WIDTH)
                    .append("' height='")
                    .append(IMAGE_HEIGHT)
                    .append("' viewBox='0 0 ")
                    .append(IMAGE_WIDTH)
                    .append(' ')
                    .append(IMAGE_HEIGHT)
                    .append("'>");
            svg.append("<rect width='100%' height='100%' rx='12' ry='12' fill='rgb(248,250,252)'/>");

            for (int index = 0; index < 12; index++) {
                svg.append("<line x1='")
                        .append(SECURE_RANDOM.nextInt(IMAGE_WIDTH))
                        .append("' y1='")
                        .append(SECURE_RANDOM.nextInt(IMAGE_HEIGHT))
                        .append("' x2='")
                        .append(SECURE_RANDOM.nextInt(IMAGE_WIDTH))
                        .append("' y2='")
                        .append(SECURE_RANDOM.nextInt(IMAGE_HEIGHT))
                        .append("' stroke='rgb(210,214,220)' stroke-width='1.6' opacity='0.9'/>");
            }

            for (int index = 0; index < text.length(); index++) {
                char character = text.charAt(index);
                double rotation = (SECURE_RANDOM.nextDouble() - 0.5) * 24;
                int x = 24 + index * 28;
                int y = 38 + SECURE_RANDOM.nextInt(8);
                int red = 35 + SECURE_RANDOM.nextInt(80);
                int green = 55;
                int blue = 55 + SECURE_RANDOM.nextInt(80);

                svg.append("<text x='")
                        .append(x)
                        .append("' y='")
                        .append(y)
                        .append("' font-family='Arial, Helvetica, sans-serif' font-size='32' font-weight='700' fill='rgb(")
                        .append(red)
                        .append(',')
                        .append(green)
                        .append(',')
                        .append(blue)
                        .append(")' transform='rotate(")
                        .append(String.format(java.util.Locale.ROOT, "%.2f", rotation))
                        .append(' ')
                        .append(x)
                        .append(' ')
                        .append(y)
                        .append(")'>")
                        .append(escapeXml(Character.toString(character)))
                        .append("</text>");
            }

            for (int index = 0; index < 40; index++) {
                svg.append("<circle cx='")
                        .append(SECURE_RANDOM.nextInt(IMAGE_WIDTH))
                        .append("' cy='")
                        .append(SECURE_RANDOM.nextInt(IMAGE_HEIGHT))
                        .append("' r='1.3' fill='rgb(190,196,204)'/>");
            }

            svg.append("</svg>");

            return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.error("Failed to render captcha image: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Unable to generate captcha image.");
        }
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record CaptchaEntry(String answer, LocalDateTime expiresAt) {
    }
}
