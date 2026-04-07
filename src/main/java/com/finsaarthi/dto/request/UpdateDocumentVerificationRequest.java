package com.finsaarthi.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDocumentVerificationRequest {

    @NotNull(message = "Verification state is required")
    private Boolean verified;

    private String notes;

    public boolean isVerified() {
        return Boolean.TRUE.equals(verified);
    }
}
