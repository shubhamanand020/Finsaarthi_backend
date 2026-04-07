package com.finsaarthi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {

    private Long id;
    private Long scholarshipId;
    private String scholarshipTitle;
    private String scholarshipProvider;
    private java.math.BigDecimal scholarshipAmount;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String status;
    private String adminNotes;
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;
    private String applicantAddress;
    private String applicantEducation;
    private String studentClass;
    private String location;
    private String parentName;
    private String parentOccupation;
    private String parentMobile;
    private Double marks10th;
    private Double marks12th;
    private String documentLinks;
    private List<ApplicationDocumentResponse> documents;
    private boolean allDocumentsVerified;
    private List<ApplicationReviewAuditResponse> reviewHistory;
    private Double gpa;
    private LocalDateTime submittedAt;
}
