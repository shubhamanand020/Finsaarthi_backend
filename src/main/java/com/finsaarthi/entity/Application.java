package com.finsaarthi.entity;

import com.finsaarthi.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(
    name = "applications",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_student_scholarship",
            columnNames = {"student_id", "scholarship_id"}
        )
    },
    indexes = {
        @Index(name = "idx_app_student", columnList = "student_id"),
        @Index(name = "idx_app_scholarship", columnList = "scholarship_id"),
        @Index(name = "idx_app_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scholarship_id", nullable = false)
    private Scholarship scholarship;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @Column(nullable = false)
    private String applicantName;

    @Column(nullable = false)
    private String applicantEmail;

    @Column(nullable = false, length = 20)
    private String applicantPhone;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String applicantAddress;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String applicantEducation;

    private String studentClass;
    private String location;
    private String parentName;
    private String parentOccupation;
    private String parentMobile;
    private Double marks10th;
    private Double marks12th;
    private Double gpa;

    @OneToMany(
            mappedBy = "application",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ApplicationDocument> applicationDocuments = new ArrayList<>();

        @OneToMany(
            mappedBy = "application",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
        )
        @Builder.Default
        private List<ApplicationReviewAudit> reviewAudits = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Transient
    private String documentLinks;

    public void setApplicationDocuments(List<ApplicationDocument> applicationDocuments) {
        if (this.applicationDocuments == null) {
            this.applicationDocuments = new ArrayList<>();
        } else {
            this.applicationDocuments.clear();
        }

        if (applicationDocuments == null) {
            return;
        }

        for (ApplicationDocument applicationDocument : applicationDocuments) {
            this.applicationDocuments.add(applicationDocument);
            applicationDocument.setApplication(this);
        }
    }

    public void addApplicationDocument(ApplicationDocument applicationDocument) {
        if (applicationDocuments == null) {
            applicationDocuments = new ArrayList<>();
        }
        applicationDocuments.add(applicationDocument);
        applicationDocument.setApplication(this);
    }

    public void addReviewAudit(ApplicationReviewAudit reviewAudit) {
        if (reviewAudits == null) {
            reviewAudits = new ArrayList<>();
        }
        reviewAudits.add(reviewAudit);
        reviewAudit.setApplication(this);
    }

    public String getDocumentLinks() {
        if (applicationDocuments == null || applicationDocuments.isEmpty()) {
            return documentLinks;
        }
        return applicationDocuments.stream()
                .map(ApplicationDocument::getLink)
                .collect(Collectors.joining("\n"));
    }

    public void setDocumentLinks(String documentLinks) {
        this.documentLinks = documentLinks;
        if (this.applicationDocuments == null) {
            this.applicationDocuments = new ArrayList<>();
        }

        if (documentLinks == null || documentLinks.isBlank()) {
            this.applicationDocuments.clear();
            return;
        }

        String[] links = documentLinks.split("\\r?\\n");
        this.applicationDocuments.clear();

        for (int i = 0; i < links.length; i++) {
            String link = links[i].trim();
            if (link.isBlank()) {
                continue;
            }
            ApplicationDocument document = ApplicationDocument.builder()
                    .application(this)
                    .documentName("Document " + (i + 1))
                    .link(link)
                    .build();
            this.applicationDocuments.add(document);
        }
    }

    @PostLoad
    void hydrateTransientFields() {
        this.documentLinks = getDocumentLinks();
    }

    @PrePersist
    @PreUpdate
    void syncCompatibilityFields() {
        if (applicationDocuments != null && !applicationDocuments.isEmpty()) {
            for (ApplicationDocument applicationDocument : applicationDocuments) {
                applicationDocument.setApplication(this);
            }
            this.documentLinks = getDocumentLinks();
        } else if (documentLinks != null) {
            setDocumentLinks(documentLinks);
        }

        if (reviewAudits != null) {
            for (ApplicationReviewAudit reviewAudit : reviewAudits) {
                reviewAudit.setApplication(this);
            }
        }
    }
}
