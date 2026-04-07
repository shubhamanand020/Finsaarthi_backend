package com.finsaarthi.service;

import com.finsaarthi.dto.request.ApplicationRequest;
import com.finsaarthi.dto.request.ApplicationDocumentRequest;
import com.finsaarthi.dto.request.UpdateDocumentVerificationRequest;
import com.finsaarthi.dto.request.UpdateApplicationStatusRequest;
import com.finsaarthi.dto.request.SendApplicationPdfRequest;
import com.finsaarthi.dto.response.ApplicationDocumentResponse;
import com.finsaarthi.dto.response.ApplicationReviewAuditResponse;
import com.finsaarthi.dto.response.ApplicationResponse;
import com.finsaarthi.entity.Application;
import com.finsaarthi.entity.ApplicationDocument;
import com.finsaarthi.entity.ApplicationReviewAudit;
import com.finsaarthi.entity.RequiredDocument;
import com.finsaarthi.entity.Scholarship;
import com.finsaarthi.entity.User;
import com.finsaarthi.enums.ApplicationStatus;
import com.finsaarthi.enums.Role;
import com.finsaarthi.exception.AccessDeniedException;
import com.finsaarthi.exception.ApplicationAlreadyExistsException;
import com.finsaarthi.exception.ResourceNotFoundException;
import com.finsaarthi.repository.ApplicationRepository;
import com.finsaarthi.repository.ScholarshipRepository;
import com.finsaarthi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository        userRepository;
    private final ScholarshipRepository scholarshipRepository;
    private final ApplicationPdfService applicationPdfService;
    private final EmailService emailService;
    private final CaptchaService captchaService;

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplications() {
        return applicationRepository.findAllWithDetails()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplications(Integer page, Integer size) {
        return paginate(getAllApplications(), page, size);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByStudent(Long studentId) {
        if (!userRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("User", "id", studentId);
        }
        return applicationRepository.findByStudentIdWithDetails(studentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(String authenticatedEmail) {
        User student = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "email", authenticatedEmail)
                );
        return applicationRepository.findByStudentIdWithDetails(student.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(
            String authenticatedEmail,
            Integer page,
            Integer size
    ) {
        return paginate(getMyApplications(authenticatedEmail), page, size);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id, String authenticatedEmail) {
        Application application = applicationRepository.findByIdWithDetails(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Application", "id", id)
                );

        User requester = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "email", authenticatedEmail)
                );

        boolean isOwner = application.getStudent()
                .getEmail().equalsIgnoreCase(authenticatedEmail);
        boolean isAdmin = requester.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException(
                    "You do not have permission to view this application."
            );
        }

        return mapToResponse(application);
    }

    @Transactional
    public ApplicationResponse submitApplication(
            ApplicationRequest request,
            String authenticatedEmail
    ) {
        User student = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "email", authenticatedEmail)
                );

        captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptchaInput());

        Scholarship scholarship = scholarshipRepository
                .findById(request.getScholarshipId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Scholarship", "id", request.getScholarshipId()
                        )
                );

        if (!scholarship.getIsActive()) {
            throw new IllegalArgumentException(
                    "This scholarship is no longer accepting applications."
            );
        }

        if (scholarship.getDeadline().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "The application deadline for this scholarship has passed."
            );
        }

        if (applicationRepository.existsByStudentIdAndScholarshipId(
                student.getId(), scholarship.getId())) {
            throw new ApplicationAlreadyExistsException(
                    student.getId(), scholarship.getId()
            );
        }

        Application application = Application.builder()
                .student(student)
                .scholarship(scholarship)
                .status(ApplicationStatus.PENDING)
                .applicantName(request.getApplicantName())
                .applicantEmail(request.getApplicantEmail())
                .applicantPhone(request.getApplicantPhone())
                .applicantAddress(request.getApplicantAddress())
                .applicantEducation(request.getApplicantEducation())
                .studentClass(request.getStudentClass())
                .location(request.getLocation())
                .parentName(request.getParentName())
                .parentOccupation(request.getParentOccupation())
                .parentMobile(request.getParentMobile())
                .marks10th(request.getMarks10th())
                .marks12th(request.getMarks12th())
                .applicationDocuments(buildApplicationDocuments(request, scholarship))
                .gpa(request.getGpa())
                .build();

        Application saved = applicationRepository.save(application);
        log.info("Application submitted by student id: {} for scholarship id: {}",
                student.getId(), scholarship.getId());
        sendApplicationSubmittedNotification(saved);

        return mapToResponse(saved);
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(
            Long id,
            UpdateApplicationStatusRequest request,
            String adminEmail
    ) {
        Application application = applicationRepository.findByIdWithDetails(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Application", "id", id)
                );

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "email", adminEmail)
                );

        ApplicationStatus currentStatus = application.getStatus();
        ApplicationStatus targetStatus = request.getStatus();

        validateStatusTransition(currentStatus, targetStatus);

        String notes = request.getAdminNotes() != null ? request.getAdminNotes().trim() : "";

        if (targetStatus == ApplicationStatus.REJECTED && notes.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required.");
        }

        if ((targetStatus == ApplicationStatus.VERIFIED || targetStatus == ApplicationStatus.APPROVED)
                && !areAllDocumentsVerified(application)) {
            throw new IllegalArgumentException("All submitted documents must be verified before this action.");
        }

        application.setStatus(targetStatus);

        if (!notes.isBlank()) {
            application.setAdminNotes(notes);
        }

        application.addReviewAudit(ApplicationReviewAudit.builder()
                .admin(admin)
                .action("STATUS_CHANGED")
                .fromStatus(currentStatus)
                .toStatus(targetStatus)
                .notes(notes.isBlank() ? null : notes)
                .build());

        applicationRepository.saveAndFlush(application);
        log.info("Application id: {} status updated to: {}", id, request.getStatus());

        Application updated = applicationRepository.findByIdWithDetails(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Application", "id", id)
                );

        sendApplicationStatusNotification(updated);
        return mapToResponse(updated);
    }

    @Transactional
    public ApplicationResponse updateDocumentVerification(
            Long applicationId,
            Long documentId,
            UpdateDocumentVerificationRequest request,
            String adminEmail
    ) {
        try {
                        if (request == null) {
                                throw new RuntimeException("Request body is null");
                        }

            if (documentId == null) {
                throw new RuntimeException("Document ID is null");
            }

            log.info("Document verification request received. applicationId: {}, documentId: {}, verified: {}",
                    applicationId, documentId, request != null ? request.getVerified() : null);

            Application application = applicationRepository.findByIdWithDetails(applicationId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Application", "id", applicationId)
                    );

            User admin = userRepository.findByEmail(adminEmail)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("User", "email", adminEmail)
                    );

            application.getApplicationDocuments()
                    .forEach(doc -> System.out.println("Doc ID: " + doc.getId()));

            ApplicationDocument document = application.getApplicationDocuments().stream()
                    .filter(item -> Objects.equals(item.getId(), documentId))
                    .findFirst()
                    .orElseThrow(() ->
                            new RuntimeException("Document not found for ID: " + documentId)
                    );

            document.setVerified(request.isVerified());

            String notes = request.getNotes() != null ? request.getNotes().trim() : "";
            String action = document.isVerified() ? "DOCUMENT_VERIFIED" : "DOCUMENT_MARKED_INVALID";

            application.addReviewAudit(ApplicationReviewAudit.builder()
                    .admin(admin)
                    .action(action)
                    .fromStatus(application.getStatus())
                    .toStatus(application.getStatus())
                    .notes(notes.isBlank() ? "Document: " + document.getDocumentName() : notes)
                    .build());

            applicationRepository.saveAndFlush(application);

            return buildApplicationResponse(application);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public void sendApplicationPdf(Long id, SendApplicationPdfRequest request) {
        Application application = applicationRepository.findByIdWithDetails(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Application", "id", id)
                );

        byte[] pdfBytes = applicationPdfService.generateApplicationPdf(application);
        String message = (request != null && request.getMessage() != null && !request.getMessage().isBlank())
                ? request.getMessage().trim()
                : "Please find your FinSaarthi application summary attached as a PDF.";

        emailService.sendEmailWithAttachment(
                application.getApplicantEmail(),
                "FinSaarthi Application PDF - " + application.getScholarship().getTitle(),
                message,
                pdfBytes,
                "application-" + application.getId() + ".pdf"
        );

        log.info("Application PDF sent for application id: {}", id);
    }

    @Transactional(readOnly = true)
    public boolean hasStudentApplied(Long studentId, Long scholarshipId) {
        return applicationRepository.existsByStudentIdAndScholarshipId(
                studentId, scholarshipId
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getApplicationStats() {
        return Map.of(
                "total",       applicationRepository.count(),
                "pending",     applicationRepository.countByStatus(ApplicationStatus.PENDING),
                "underReview", applicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW),
                "approved",    applicationRepository.countByStatus(ApplicationStatus.APPROVED),
                "rejected",    applicationRepository.countByStatus(ApplicationStatus.REJECTED)
        );
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByStatus(ApplicationStatus status) {
        return applicationRepository.findByStatusWithDetails(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ApplicationResponse mapToResponse(Application a) {
        List<ApplicationDocumentResponse> documents = mapApplicationDocuments(
                a.getApplicationDocuments()
        );
        boolean allDocumentsVerified = !documents.isEmpty()
                && documents.stream().allMatch(ApplicationDocumentResponse::isVerified);

        return ApplicationResponse.builder()
                .id(a.getId())
                .scholarshipId(a.getScholarship().getId())
                .scholarshipTitle(a.getScholarship().getTitle())
                .scholarshipProvider(a.getScholarship().getProvider())
                .scholarshipAmount(a.getScholarship().getAmount())
                .studentId(a.getStudent().getId())
                .studentName(a.getStudent().getName())
                .studentEmail(a.getStudent().getEmail())
                .status(a.getStatus().name())
                .adminNotes(a.getAdminNotes())
                .applicantName(a.getApplicantName())
                .applicantEmail(a.getApplicantEmail())
                .applicantPhone(a.getApplicantPhone())
                .applicantAddress(a.getApplicantAddress())
                .applicantEducation(a.getApplicantEducation())
                .studentClass(a.getStudentClass())
                .location(a.getLocation())
                .parentName(a.getParentName())
                .parentOccupation(a.getParentOccupation())
                .parentMobile(a.getParentMobile())
                .marks10th(a.getMarks10th())
                .marks12th(a.getMarks12th())
                .documentLinks(documents.stream()
                        .map(ApplicationDocumentResponse::getLink)
                        .collect(Collectors.joining("\n")))
                .documents(documents)
                .allDocumentsVerified(allDocumentsVerified)
                .reviewHistory(mapReviewHistory(a.getReviewAudits()))
                .gpa(a.getGpa())
                .submittedAt(a.getSubmittedAt())
                .build();
    }

            private ApplicationResponse buildApplicationResponse(Application application) {
                if (application == null) {
                    throw new RuntimeException("Application is null while building response");
                }

                List<ApplicationDocumentResponse> documents = mapApplicationDocuments(application.getApplicationDocuments());
                boolean allDocumentsVerified = !documents.isEmpty()
                        && documents.stream().allMatch(ApplicationDocumentResponse::isVerified);

                return ApplicationResponse.builder()
                        .id(application.getId())
                        .scholarshipId(application.getScholarship() != null ? application.getScholarship().getId() : null)
                        .scholarshipTitle(application.getScholarship() != null ? application.getScholarship().getTitle() : null)
                        .scholarshipProvider(application.getScholarship() != null ? application.getScholarship().getProvider() : null)
                        .scholarshipAmount(application.getScholarship() != null ? application.getScholarship().getAmount() : null)
                        .studentId(application.getStudent() != null ? application.getStudent().getId() : null)
                        .studentName(application.getStudent() != null ? application.getStudent().getName() : null)
                        .studentEmail(application.getStudent() != null ? application.getStudent().getEmail() : null)
                        .status(application.getStatus() != null ? application.getStatus().name() : null)
                        .adminNotes(application.getAdminNotes())
                        .applicantName(application.getApplicantName())
                        .applicantEmail(application.getApplicantEmail())
                        .applicantPhone(application.getApplicantPhone())
                        .applicantAddress(application.getApplicantAddress())
                        .applicantEducation(application.getApplicantEducation())
                        .studentClass(application.getStudentClass())
                        .location(application.getLocation())
                        .parentName(application.getParentName())
                        .parentOccupation(application.getParentOccupation())
                        .parentMobile(application.getParentMobile())
                        .marks10th(application.getMarks10th())
                        .marks12th(application.getMarks12th())
                        .documentLinks(documents.stream()
                                .map(ApplicationDocumentResponse::getLink)
                                .collect(Collectors.joining("\n")))
                        .documents(documents)
                        .allDocumentsVerified(allDocumentsVerified)
                        .reviewHistory(mapReviewHistory(application.getReviewAudits()))
                        .gpa(application.getGpa())
                        .submittedAt(application.getSubmittedAt())
                        .build();
            }

    private List<ApplicationDocument> buildApplicationDocuments(
            ApplicationRequest request,
            Scholarship scholarship
    ) {
        List<RequiredDocument> requiredDocuments = scholarship.getRequiredDocuments();

        if (requiredDocuments == null || requiredDocuments.isEmpty()) {
            throw new IllegalArgumentException(
                    "This scholarship is not configured with required documents."
            );
        }

        List<ApplicationDocumentRequest> documents = normalizeDocumentRequests(request, requiredDocuments);
        Map<String, String> expectedDocuments = requiredDocuments.stream()
                .collect(Collectors.toMap(
                        document -> normalizeDocumentName(document.getName()),
                        RequiredDocument::getName,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Set<String> expectedNames = expectedDocuments.keySet();

        Set<String> submittedNames = documents.stream()
                .map(ApplicationDocumentRequest::getDocumentName)
                .map(this::normalizeDocumentName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> missingDocuments = expectedNames.stream()
                .filter(name -> !submittedNames.contains(name))
                .collect(Collectors.toList());
        if (!missingDocuments.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required documents: " + String.join(", ", missingDocuments)
            );
        }

        List<String> unexpectedDocuments = submittedNames.stream()
                .filter(name -> !expectedNames.contains(name))
                .collect(Collectors.toList());
        if (!unexpectedDocuments.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unexpected documents submitted: " + String.join(", ", unexpectedDocuments)
            );
        }

        return documents.stream()
                .map(document -> ApplicationDocument.builder()
                        .documentName(expectedDocuments.get(
                                normalizeDocumentName(document.getDocumentName())
                        ))
                        .link(validateAndNormalizeLink(document.getLink()))
                        .verified(false)
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<ApplicationDocumentRequest> normalizeDocumentRequests(
            ApplicationRequest request,
            List<RequiredDocument> requiredDocuments
    ) {
        if (request.getDocuments() != null && !request.getDocuments().isEmpty()) {
            ensureNoDuplicateDocumentNames(request.getDocuments());
            return request.getDocuments();
        }

        if (request.getDocumentLinks() == null || request.getDocumentLinks().isBlank()) {
            throw new IllegalArgumentException("Documents are required.");
        }

        String[] links = request.getDocumentLinks().split("\\r?\\n");
        if (links.length != requiredDocuments.size()) {
            throw new IllegalArgumentException(
                    "Submitted documents do not match the required document list."
            );
        }

        List<ApplicationDocumentRequest> fallbackDocuments = new ArrayList<>();

        for (int i = 0; i < links.length; i++) {
            String link = links[i].trim();
            if (link.isBlank()) {
                continue;
            }
            ApplicationDocumentRequest requestDocument = new ApplicationDocumentRequest();
            requestDocument.setDocumentName(requiredDocuments.get(i).getName());
            requestDocument.setLink(link);
            fallbackDocuments.add(requestDocument);
        }

        if (fallbackDocuments.isEmpty()) {
            throw new IllegalArgumentException("Documents are required.");
        }

        return fallbackDocuments;
    }

    private void ensureNoDuplicateDocumentNames(List<ApplicationDocumentRequest> documents) {
        Set<String> names = new LinkedHashSet<>();
        List<String> duplicates = new ArrayList<>();

        for (ApplicationDocumentRequest document : documents) {
            String normalizedName = normalizeDocumentName(document.getDocumentName());
            if (!names.add(normalizedName)) {
                duplicates.add(normalizedName);
            }
        }

        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException(
                    "Duplicate documents submitted: " + String.join(", ", duplicates)
            );
        }
    }

    private String normalizeDocumentName(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String validateAndNormalizeLink(String link) {
        String normalizedLink = link == null ? "" : link.trim();
        if (normalizedLink.isBlank()) {
            throw new IllegalArgumentException("Document link is required.");
        }

        try {
            URI uri = new URI(normalizedLink);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || host == null ||
                    (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException(
                        "Invalid document link: '" + normalizedLink + "'. Only HTTP/HTTPS links are allowed."
                );
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(
                    "Invalid document link: '" + normalizedLink + "'."
            );
        }

        return normalizedLink;
    }

    private List<ApplicationDocumentResponse> mapApplicationDocuments(
            List<ApplicationDocument> documents
    ) {
        if (documents == null) {
            return List.of();
        }

        return documents.stream()
                .sorted(Comparator.comparing(ApplicationDocument::getId, Comparator.nullsLast(Long::compareTo)))
                .map(document -> ApplicationDocumentResponse.builder()
                        .id(document.getId())
                        .name(document.getDocumentName())
                        .link(document.getLink())
                                                .verified(document.isVerified())
                        .build())
                .collect(Collectors.toList());
    }

        private List<ApplicationReviewAuditResponse> mapReviewHistory(List<ApplicationReviewAudit> audits) {
                if (audits == null) {
                        return List.of();
                }

                return audits.stream()
                                .sorted(Comparator.comparing(ApplicationReviewAudit::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                                .map(audit -> ApplicationReviewAuditResponse.builder()
                                                .id(audit.getId())
                                                .adminId(audit.getAdmin() != null ? audit.getAdmin().getId() : null)
                                                .adminEmail(audit.getAdmin() != null ? audit.getAdmin().getEmail() : null)
                                                .action(audit.getAction())
                                                .fromStatus(audit.getFromStatus() != null ? audit.getFromStatus().name() : null)
                                                .toStatus(audit.getToStatus() != null ? audit.getToStatus().name() : null)
                                                .notes(audit.getNotes())
                                                .timestamp(audit.getCreatedAt())
                                                .build())
                                .collect(Collectors.toList());
        }

        private boolean areAllDocumentsVerified(Application application) {
                List<ApplicationDocument> documents = application.getApplicationDocuments();
                return documents != null && !documents.isEmpty()
                                && documents.stream().allMatch(ApplicationDocument::isVerified);
        }

        private void validateStatusTransition(ApplicationStatus currentStatus, ApplicationStatus nextStatus) {
                if (currentStatus == nextStatus) {
                        return;
                }

                boolean allowed = switch (currentStatus) {
                        case PENDING -> nextStatus == ApplicationStatus.UNDER_REVIEW;
                        case UNDER_REVIEW -> nextStatus == ApplicationStatus.VERIFIED;
                        case VERIFIED -> nextStatus == ApplicationStatus.APPROVED || nextStatus == ApplicationStatus.REJECTED;
                        case APPROVED, REJECTED -> false;
                };

                if (!allowed) {
                        throw new IllegalArgumentException(
                                        "Invalid review workflow transition: " + currentStatus + " -> " + nextStatus
                        );
                }
        }

    private <T> List<T> paginate(List<T> items, Integer page, Integer size) {
        if (page == null && size == null) {
            return items;
        }

        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 10;

        if (resolvedPage < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0.");
        }
        if (resolvedSize <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0.");
        }

        int fromIndex = resolvedPage * resolvedSize;
        if (fromIndex >= items.size()) {
            return new ArrayList<>();
        }

        int toIndex = Math.min(fromIndex + resolvedSize, items.size());
        return items.subList(fromIndex, toIndex);
    }

    private void sendApplicationSubmittedNotification(Application application) {
        String subject = "FinSaarthi Application Submitted - " + application.getScholarship().getTitle();
        String message = """
                Your scholarship application has been submitted successfully.

                Scholarship: %s
                Status: %s
                Submitted At: %s

                We will notify you when the review status changes.
                """.formatted(
                application.getScholarship().getTitle(),
                application.getStatus().name(),
                application.getSubmittedAt()
        );

        sendEmailSafely(application.getApplicantEmail(), subject, message);
    }

    private void sendApplicationStatusNotification(Application application) {
        String notes = application.getAdminNotes() != null && !application.getAdminNotes().isBlank()
                ? "\nAdmin Notes: " + application.getAdminNotes().trim()
                : "";

        String subject = "FinSaarthi Application Status Updated - " + application.getScholarship().getTitle();
        String message = """
                Your scholarship application status has been updated.

                Scholarship: %s
                Current Status: %s%s
                """.formatted(
                application.getScholarship().getTitle(),
                application.getStatus().name(),
                notes
        );

        sendEmailSafely(application.getApplicantEmail(), subject, message);
    }

    private void sendEmailSafely(String email, String subject, String message) {
        try {
            emailService.sendPlainEmail(email, subject, message);
        } catch (Exception ex) {
            log.warn("Notification email could not be sent to {}: {}", email, ex.getMessage());
        }
    }
}
