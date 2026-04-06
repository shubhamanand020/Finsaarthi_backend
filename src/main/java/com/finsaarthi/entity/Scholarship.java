package com.finsaarthi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.finsaarthi.enums.ScholarshipCategory;
import com.finsaarthi.persistence.ScholarshipCategoryConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(
    name = "scholarships",
    indexes = {
        @Index(name = "idx_scholarship_category", columnList = "category_enum"),
        @Index(name = "idx_scholarship_is_active", columnList = "is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scholarship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "scholarship_eligibility",
        joinColumns = @JoinColumn(name = "scholarship_id")
    )
    @Column(name = "criteria", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> eligibilityCriteria = new ArrayList<>();

    @Column(nullable = false)
    private LocalDate deadline;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String provider;

    @Convert(converter = ScholarshipCategoryConverter.class)
    @Column(name = "category_enum", nullable = false, length = 50)
    private ScholarshipCategory categoryEnum;

    @OneToMany(
            mappedBy = "scholarship",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<RequiredDocument> requiredDocuments = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private String category;

    @Transient
    private List<String> requirements;

    public String getCategory() {
        if (categoryEnum == null) {
            return category;
        }
        return categoryEnum.getDisplayName();
    }

    public void setCategory(String category) {
        this.category = category;
        if (category != null && !category.isBlank()) {
            this.categoryEnum = ScholarshipCategory.fromString(category);
        }
    }

    public ScholarshipCategory getCategoryEnum() {
        return categoryEnum;
    }

    public List<String> getRequirements() {
        if (requiredDocuments == null || requiredDocuments.isEmpty()) {
            return requirements != null ? requirements : new ArrayList<>();
        }
        return requiredDocuments.stream()
                .map(RequiredDocument::getName)
                .collect(Collectors.toList());
    }

    public void setRequirements(List<String> requirements) {
        this.requirements = requirements;
        if (requirements == null) {
            this.requiredDocuments = new ArrayList<>();
            return;
        }

        this.requiredDocuments = requirements.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> RequiredDocument.builder()
                        .name(value.trim())
                        .scholarship(this)
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void setRequiredDocuments(List<RequiredDocument> requiredDocuments) {
        this.requiredDocuments = requiredDocuments != null
                ? new ArrayList<>(requiredDocuments)
                : new ArrayList<>();
        for (RequiredDocument requiredDocument : this.requiredDocuments) {
            requiredDocument.setScholarship(this);
        }
    }

    public void addRequiredDocument(RequiredDocument requiredDocument) {
        if (requiredDocuments == null) {
            requiredDocuments = new ArrayList<>();
        }
        requiredDocuments.add(requiredDocument);
        requiredDocument.setScholarship(this);
    }

    @PostLoad
    void hydrateTransientFields() {
        this.category = getCategory();
        this.requirements = getRequirements();
    }

    @PrePersist
    @PreUpdate
    void syncCompatibilityFields() {
        if (categoryEnum == null && category != null && !category.isBlank()) {
            categoryEnum = ScholarshipCategory.fromString(category);
        }

        if (requirements != null) {
            setRequirements(requirements);
        } else if (requiredDocuments != null) {
            for (RequiredDocument requiredDocument : requiredDocuments) {
                requiredDocument.setScholarship(this);
            }
        }
    }
}
