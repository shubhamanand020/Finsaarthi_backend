package com.finsaarthi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationReviewAuditResponse {

    private Long id;
    private Long adminId;
    private String adminEmail;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String notes;
    private LocalDateTime timestamp;
}
