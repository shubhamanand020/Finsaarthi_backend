package com.finsaarthi.enums;

public enum Role {

    STUDENT("Student"),
    ADMIN("Admin");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Role fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Role value cannot be null or blank."
            );
        }
        try {
            return Role.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid role: '" + value +
                    "'. Allowed values: STUDENT, ADMIN."
            );
        }
    }
}