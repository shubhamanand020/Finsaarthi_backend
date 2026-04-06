package com.finsaarthi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScholarshipResponse {

    private Long id;
    private String title;
    private BigDecimal amount;
    private List<String> eligibilityCriteria;
    private LocalDate deadline;
    private String description;
    private String provider;
    private String category;
    private List<RequiredDocumentResponse> requiredDocuments;
    private List<String> requirements;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
