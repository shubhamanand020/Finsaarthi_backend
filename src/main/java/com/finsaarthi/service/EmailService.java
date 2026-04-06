package com.finsaarthi.service;

public interface EmailService {

    void sendOtpEmail(String email, String otp, String purposeLabel);

    void sendPlainEmail(String email, String subject, String message);

    void sendEmailWithAttachment(
            String email,
            String subject,
            String message,
            byte[] attachment,
            String attachmentFilename
    );
}
