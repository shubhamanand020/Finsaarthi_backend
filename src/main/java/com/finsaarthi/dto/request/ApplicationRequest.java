package com.finsaarthi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class ApplicationRequest {

    @NotNull(message = "Scholarship ID is required")
    private Long scholarshipId;

    @NotBlank(message = "Applicant name is required")
    private String applicantName;

    @NotBlank(message = "Applicant email is required")
    @Email(message = "Please provide a valid email address")
    private String applicantEmail;

    @NotBlank(message = "Phone number is required")
    private String applicantPhone;

    @NotBlank(message = "Address is required")
    private String applicantAddress;

    @NotBlank(message = "Education details are required")
    private String applicantEducation;

    @NotBlank(message = "Class is required")
    private String studentClass;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Parent name is required")
    private String parentName;

    @NotBlank(message = "Parent occupation is required")
    private String parentOccupation;

    @NotBlank(message = "Parent mobile number is required")
    private String parentMobile;

    @NotNull(message = "10th marks are required")
    private Double marks10th;

    @NotNull(message = "12th marks are required")
    private Double marks12th;

    @Deprecated
    private String documentLinks;

    private List<@Valid ApplicationDocumentRequest> documents;

    @NotNull(message = "GPA/Percentage is required")
    @Min(value = 0, message = "GPA cannot be negative")
    @Max(value = 100, message = "GPA cannot exceed 100")
    private Double gpa;

    @NotBlank(message = "Captcha ID is required")
    private String captchaId;

    @NotBlank(message = "Captcha input is required")
    private String captchaInput;
}
