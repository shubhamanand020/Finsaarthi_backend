package com.finsaarthi.controller;

import com.finsaarthi.dto.request.ScholarshipRequest;
import com.finsaarthi.dto.response.ApiResponse;
import com.finsaarthi.dto.response.RequiredDocumentResponse;
import com.finsaarthi.dto.response.ScholarshipResponse;
import com.finsaarthi.service.ScholarshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scholarships")
@RequiredArgsConstructor
@Slf4j
public class ScholarshipController {

    private final ScholarshipService scholarshipService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/scholarships
    // Public — retrieves all scholarships (active + inactive, for admin view)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScholarshipResponse>>> getAllScholarships(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        log.info("All scholarships requested");

        List<ScholarshipResponse> scholarships =
                scholarshipService.getAllScholarships(page, size);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Scholarships retrieved successfully.",
                        scholarships
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/scholarships/active
    // Public — retrieves only active scholarships sorted by deadline
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ScholarshipResponse>>> getActiveScholarships() {
        log.info("Active scholarships requested");

        List<ScholarshipResponse> scholarships =
                scholarshipService.getActiveScholarships();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Active scholarships retrieved successfully.",
                        scholarships
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/scholarships/open
    // Public — retrieves active scholarships whose deadline has not passed
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/open")
    public ResponseEntity<ApiResponse<List<ScholarshipResponse>>> getOpenScholarships() {
        log.info("Open (non-expired) scholarships requested");

        List<ScholarshipResponse> scholarships =
                scholarshipService.getActiveNonExpiredScholarships();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Open scholarships retrieved successfully.",
                        scholarships
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/scholarships/search?keyword=
    // Public — full-text search across title, description, and provider
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ScholarshipResponse>>> searchScholarships(
            @RequestParam(required = false, defaultValue = "") String keyword
    ) {
        log.info("Scholarship search requested with keyword: '{}'", keyword);

        List<ScholarshipResponse> results =
                scholarshipService.searchScholarships(keyword);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Search completed successfully.",
                        results
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/scholarships/category?name=
    // Public — filters active scholarships by category
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/category")
    public ResponseEntity<ApiResponse<List<ScholarshipResponse>>> getByCategory(
            @RequestParam String name
    ) {
        log.info("Scholarships requested for category: '{}'", name);

        List<ScholarshipResponse> scholarships =
                scholarshipService.getScholarshipsByCategory(name);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Scholarships retrieved for category: " + name,
                        scholarships
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/scholarships/stats
    // ADMIN only — returns active scholarship count
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getScholarshipStats() {
        long activeCount = scholarshipService.countActiveScholarships();

        Map<String, Long> stats = Map.of(
                "activeScholarships", activeCount
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Scholarship statistics retrieved successfully.",
                        stats
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/scholarships/{id}
    // Public — retrieves a single scholarship by its ID
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScholarshipResponse>> getScholarshipById(
            @PathVariable Long id
    ) {
        log.info("Scholarship requested with id: {}", id);

        ScholarshipResponse scholarship = scholarshipService.getScholarshipById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Scholarship retrieved successfully.",
                        scholarship
                )
        );
    }

    @GetMapping("/{id}/required-documents")
    public ResponseEntity<ApiResponse<List<RequiredDocumentResponse>>> getRequiredDocuments(
            @PathVariable Long id
    ) {
        log.info("Required documents requested for scholarship id: {}", id);

        List<RequiredDocumentResponse> documents = scholarshipService.getRequiredDocuments(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Required documents retrieved successfully.",
                        documents
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/scholarships
    // ADMIN only — creates a new scholarship
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ScholarshipResponse>> createScholarship(
            @Valid @RequestBody ScholarshipRequest request
    ) {
        log.info("Admin creating new scholarship: '{}'", request.getTitle());

        ScholarshipResponse created = scholarshipService.createScholarship(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Scholarship created successfully.",
                        created
                ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/scholarships/{id}
    // ADMIN only — fully updates an existing scholarship
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ScholarshipResponse>> updateScholarship(
            @PathVariable Long id,
            @Valid @RequestBody ScholarshipRequest request
    ) {
        log.info("Admin updating scholarship id: {}", id);

        ScholarshipResponse updated = scholarshipService.updateScholarship(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Scholarship updated successfully.",
                        updated
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/scholarships/{id}/toggle
    // ADMIN only — toggles the isActive flag of a scholarship
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ScholarshipResponse>> toggleActiveStatus(
            @PathVariable Long id
    ) {
        log.info("Admin toggling active status for scholarship id: {}", id);

        ScholarshipResponse updated = scholarshipService.toggleActiveStatus(id);

        String statusMessage = Boolean.TRUE.equals(updated.getIsActive())
                ? "Scholarship activated successfully."
                : "Scholarship deactivated successfully.";

        return ResponseEntity.ok(
                ApiResponse.success(statusMessage, updated)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/scholarships/{id}
    // ADMIN only — permanently deletes a scholarship
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteScholarship(
            @PathVariable Long id
    ) {
        log.info("Admin deleting scholarship id: {}", id);

        scholarshipService.deleteScholarship(id);

        return ResponseEntity.ok(
                ApiResponse.success("Scholarship deleted successfully.")
        );
    }
}
