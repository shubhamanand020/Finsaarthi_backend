package com.finsaarthi.enums;

public enum ApplicationStatus {

    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    VERIFIED("Verified"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;

    ApplicationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ApplicationStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "ApplicationStatus value cannot be null or blank."
            );
        }
        try {
            return ApplicationStatus.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid status: '" + value +
                    "'. Allowed values: PENDING, UNDER_REVIEW, VERIFIED, APPROVED, REJECTED."
            );
        }
    }

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isUnderReview() {
        return this == UNDER_REVIEW;
    }

    public boolean isVerified() {
        return this == VERIFIED;
    }
}