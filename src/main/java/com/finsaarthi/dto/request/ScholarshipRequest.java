package com.finsaarthi.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ScholarshipRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be a positive value")
    private BigDecimal amount;

    @NotEmpty(message = "At least one eligibility criterion is required")
    private List<String> eligibilityCriteria;

    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be a future date")
    private LocalDate deadline;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Category is required")
    private String category;

    @Deprecated
    private List<String> requirements;

    private List<String> requiredDocuments;

    private Boolean isActive = true;

    public List<String> resolveRequiredDocuments() {
        if (requiredDocuments != null) {
            return requiredDocuments;
        }
        return requirements;
    }
}
