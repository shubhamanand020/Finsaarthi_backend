package com.finsaarthi.persistence;

import com.finsaarthi.enums.ScholarshipCategory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ScholarshipCategoryConverter implements AttributeConverter<ScholarshipCategory, String> {

    @Override
    public String convertToDatabaseColumn(ScholarshipCategory attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public ScholarshipCategory convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return ScholarshipCategory.fromString(dbData);
    }
}
