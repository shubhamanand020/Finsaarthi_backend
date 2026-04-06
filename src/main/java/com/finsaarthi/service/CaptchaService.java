package com.finsaarthi.service;

import com.finsaarthi.dto.response.CaptchaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
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
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(248, 250, 252));
            graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

            for (int index = 0; index < 12; index++) {
                graphics.setColor(new Color(210, 214, 220));
                int x1 = SECURE_RANDOM.nextInt(IMAGE_WIDTH);
                int y1 = SECURE_RANDOM.nextInt(IMAGE_HEIGHT);
                int x2 = SECURE_RANDOM.nextInt(IMAGE_WIDTH);
                int y2 = SECURE_RANDOM.nextInt(IMAGE_HEIGHT);
                graphics.drawLine(x1, y1, x2, y2);
            }

            graphics.setStroke(new BasicStroke(1.6f));
            graphics.setFont(new Font("SansSerif", Font.BOLD, 32));

            for (int index = 0; index < text.length(); index++) {
                char character = text.charAt(index);
                AffineTransform originalTransform = graphics.getTransform();
                double rotation = (SECURE_RANDOM.nextDouble() - 0.5) * 0.4;
                graphics.rotate(rotation, 28 + index * 28, 34);
                graphics.setColor(new Color(35 + SECURE_RANDOM.nextInt(80), 55, 55 + SECURE_RANDOM.nextInt(80)));
                graphics.drawString(String.valueOf(character), 18 + index * 28, 38 + SECURE_RANDOM.nextInt(8));
                graphics.setTransform(originalTransform);
            }

            for (int index = 0; index < 40; index++) {
                graphics.setColor(new Color(190, 196, 204));
                graphics.fillOval(
                        SECURE_RANDOM.nextInt(IMAGE_WIDTH),
                        SECURE_RANDOM.nextInt(IMAGE_HEIGHT),
                        2,
                        2
                );
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", outputStream);
                return Base64.getEncoder().encodeToString(outputStream.toByteArray());
            }
        } catch (IOException ex) {
            log.error("Failed to render captcha image: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Unable to generate captcha image.");
        } finally {
            graphics.dispose();
        }
    }

    private record CaptchaEntry(String answer, LocalDateTime expiresAt) {
    }
}
