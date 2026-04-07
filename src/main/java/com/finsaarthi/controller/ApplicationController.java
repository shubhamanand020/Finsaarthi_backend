package com.finsaarthi.controller;

import com.finsaarthi.dto.request.ApplicationRequest;
import com.finsaarthi.dto.request.SendApplicationPdfRequest;
import com.finsaarthi.dto.request.UpdateDocumentVerificationRequest;
import com.finsaarthi.dto.request.UpdateApplicationStatusRequest;
import com.finsaarthi.dto.response.ApiResponse;
import com.finsaarthi.dto.response.ApplicationResponse;
import com.finsaarthi.dto.response.UserResponse;
import com.finsaarthi.service.ApplicationService;
import com.finsaarthi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.finsaarthi.enums.ApplicationStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService        userService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/applications
    // ADMIN only — retrieves all applications across all students
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getAllApplications(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        log.info("Admin requested all applications");

        List<ApplicationResponse> applications =
                applicationService.getAllApplications(page, size);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Applications retrieved successfully.",
                        applications
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/applications/stats
    // ADMIN only — returns application count breakdown by status
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getApplicationStats() {
        log.info("Admin requested application statistics");

        Map<String, Long> stats = applicationService.getApplicationStats();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Application statistics retrieved successfully.",
                        stats
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/applications/status?value=PENDING
    // ADMIN only — retrieves applications filtered by a given status
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplicationsByStatus(
            @RequestParam String value
    ) {
        log.info("Admin requested applications with status: {}", value);

        ApplicationStatus status;

        try {
            status = ApplicationStatus.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid status value: '" + value +
                    "'. Allowed values: PENDING, UNDER_REVIEW, APPROVED, REJECTED."
            );
        }

        List<ApplicationResponse> applications =
                applicationService.getApplicationsByStatus(status);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Applications with status '" + status +
                        "' retrieved successfully.",
                        applications
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/applications/my
    // STUDENT only — retrieves the authenticated student's own applications
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getMyApplications(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Student '{}' requested their applications",
                userDetails.getUsername());

        List<ApplicationResponse> applications =
                applicationService.getMyApplications(userDetails.getUsername(), page, size);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Your applications retrieved successfully.",
                        applications
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/applications/student/{studentId}
    // ADMIN only — retrieves all applications for a specific student
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplicationsByStudent(
            @PathVariable Long studentId
    ) {
        log.info("Admin requested applications for student id: {}", studentId);

        List<ApplicationResponse> applications =
                applicationService.getApplicationsByStudent(studentId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Applications for student id: " + studentId +
                        " retrieved successfully.",
                        applications
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/applications/{id}
    // Authenticated — retrieves a single application by ID
    // (owner student or any admin)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Application id: {} requested by: {}",
                id, userDetails.getUsername());

        ApplicationResponse application =
                applicationService.getApplicationById(id, userDetails.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Application retrieved successfully.",
                        application
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/applications/check?scholarshipId=
    // STUDENT — checks whether the authenticated student has already applied
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/check")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkIfApplied(
            @RequestParam Long scholarshipId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Student '{}' checking application status for scholarship id: {}",
                userDetails.getUsername(), scholarshipId);

        UserResponse student = userService.getUserByEmail(userDetails.getUsername());

        boolean hasApplied =
                applicationService.hasStudentApplied(student.getId(), scholarshipId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Application check completed.",
                        Map.of("hasApplied", hasApplied)
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/applications
    // STUDENT only — submits a new scholarship application
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> submitApplication(
            @Valid @RequestBody ApplicationRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Student '{}' submitting application for scholarship id: {}",
                userDetails.getUsername(), request.getScholarshipId());

        ApplicationResponse application =
                applicationService.submitApplication(
                        request,
                        userDetails.getUsername()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Application submitted successfully.",
                        application
                ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/applications/{id}/status
    // ADMIN only — updates the status and optional notes of an application
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateApplicationStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Admin updating status of application id: {} to: {}",
                id, request.getStatus());

        ApplicationResponse updated =
                applicationService.updateApplicationStatus(id, request, userDetails.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Application status updated to: " + updated.getStatus(),
                        updated
                )
        );
    }

    @PatchMapping("/{applicationId}/documents/{documentId}/verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateDocumentVerification(
            @PathVariable Long applicationId,
            @PathVariable Long documentId,
            @Valid @RequestBody UpdateDocumentVerificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Admin updating document verification. application id: {}, document id: {}", applicationId, documentId);

        ApplicationResponse updated = applicationService.updateDocumentVerification(
                applicationId,
                documentId,
                request,
                userDetails.getUsername()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Document verification updated successfully.", updated)
        );
    }

    @PostMapping("/{id}/send-pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendApplicationPdf(
            @PathVariable Long id,
            @RequestBody(required = false) SendApplicationPdfRequest request
    ) {
        log.info("Admin requested PDF email for application id: {}", id);

        applicationService.sendApplicationPdf(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Application PDF sent successfully.")
        );
    }
}
