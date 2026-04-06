package com.finsaarthi.enums;

public enum ScholarshipCategory {

    MERIT_BASED("Merit-based"),
    NEED_BASED("Need-based"),
    FIELD_SPECIFIC("Field-specific"),
    SPORTS("Sports"),
    MINORITY("Minority"),
    INTERNATIONAL("International");

    private final String displayName;

    ScholarshipCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ScholarshipCategory fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "ScholarshipCategory value cannot be null or blank."
            );
        }

        // match by enum name
        for (ScholarshipCategory cat : values()) {
            if (cat.name().equalsIgnoreCase(value.trim())
                    || cat.displayName.equalsIgnoreCase(value.trim())) {
                return cat;
            }
        }

        throw new IllegalArgumentException(
                "Invalid category: '" + value + "'. Allowed values: " +
                getAllDisplayNames()
        );
    }

    public static String getAllDisplayNames() {
        StringBuilder sb = new StringBuilder();
        ScholarshipCategory[] cats = values();
        for (int i = 0; i < cats.length; i++) {
            sb.append(cats[i].displayName);
            if (i < cats.length - 1) sb.append(", ");
        }
        return sb.toString();
    }
}