package com.finsaarthi.service;

import com.finsaarthi.dto.request.ScholarshipRequest;
import com.finsaarthi.dto.response.RequiredDocumentResponse;
import com.finsaarthi.dto.response.ScholarshipResponse;
import com.finsaarthi.entity.RequiredDocument;
import com.finsaarthi.entity.Scholarship;
import com.finsaarthi.enums.ScholarshipCategory;
import com.finsaarthi.exception.ResourceNotFoundException;
import com.finsaarthi.repository.ScholarshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScholarshipService {

    private final ScholarshipRepository scholarshipRepository;

    @Transactional(readOnly = true)
    public List<ScholarshipResponse> getAllScholarships() {
        return scholarshipRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScholarshipResponse> getAllScholarships(Integer page, Integer size) {
        return paginate(getAllScholarships(), page, size);
    }

    @Transactional(readOnly = true)
    public List<ScholarshipResponse> getActiveScholarships() {
        return scholarshipRepository.findByIsActiveTrueOrderByDeadlineAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScholarshipResponse> getActiveNonExpiredScholarships() {
        return scholarshipRepository
                .findActiveNonExpiredScholarships(LocalDate.now())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScholarshipResponse getScholarshipById(Long id) {
        Scholarship scholarship = scholarshipRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Scholarship", "id", id)
                );

        return mapToResponse(scholarship);
    }

    @Transactional(readOnly = true)
    public List<RequiredDocumentResponse> getRequiredDocuments(Long id) {
        Scholarship scholarship = scholarshipRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Scholarship", "id", id)
                );

        return mapRequiredDocuments(scholarship.getRequiredDocuments());
    }

    @Transactional(readOnly = true)
    public List<ScholarshipResponse> searchScholarships(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getActiveScholarships();
        }

        return scholarshipRepository.searchActiveScholarships(keyword.trim())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScholarshipResponse> getScholarshipsByCategory(String category) {
        ScholarshipCategory categoryEnum = ScholarshipCategory.fromString(category);
        return scholarshipRepository.findByCategoryEnumAndIsActiveTrue(categoryEnum)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ScholarshipResponse createScholarship(ScholarshipRequest request) {
        List<String> requiredDocumentNames = normalizeRequiredDocuments(
                request.resolveRequiredDocuments()
        );

        Scholarship scholarship = Scholarship.builder()
                .title(request.getTitle().trim())
                .amount(request.getAmount())
                .eligibilityCriteria(request.getEligibilityCriteria())
                .deadline(request.getDeadline())
                .description(request.getDescription().trim())
                .provider(request.getProvider().trim())
                .category(request.getCategory().trim())
                .requiredDocuments(buildRequiredDocuments(requiredDocumentNames))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Scholarship saved = scholarshipRepository.save(scholarship);
        log.info("Scholarship created with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Transactional
    public ScholarshipResponse updateScholarship(Long id, ScholarshipRequest request) {
        Scholarship scholarship = scholarshipRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Scholarship", "id", id)
                );

        List<String> requiredDocumentNames = normalizeRequiredDocuments(
                request.resolveRequiredDocuments()
        );

        scholarship.setTitle(request.getTitle().trim());
        scholarship.setAmount(request.getAmount());
        scholarship.setEligibilityCriteria(request.getEligibilityCriteria());
        scholarship.setDeadline(request.getDeadline());
        scholarship.setDescription(request.getDescription().trim());
        scholarship.setProvider(request.getProvider().trim());
        scholarship.setCategory(request.getCategory().trim());
        scholarship.setRequiredDocuments(buildRequiredDocuments(requiredDocumentNames));

        if (request.getIsActive() != null) {
            scholarship.setIsActive(request.getIsActive());
        }

        Scholarship updated = scholarshipRepository.save(scholarship);
        log.info("Scholarship updated with id: {}", id);

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteScholarship(Long id) {
        if (!scholarshipRepository.existsById(id)) {
            throw new ResourceNotFoundException("Scholarship", "id", id);
        }
        scholarshipRepository.deleteById(id);
        log.info("Scholarship deleted with id: {}", id);
    }

    @Transactional
    public ScholarshipResponse toggleActiveStatus(Long id) {
        Scholarship scholarship = scholarshipRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Scholarship", "id", id)
                );

        scholarship.setIsActive(!scholarship.getIsActive());
        Scholarship updated = scholarshipRepository.save(scholarship);

        log.info("Scholarship id: {} active status toggled to: {}",
                id, updated.getIsActive());

        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public long countActiveScholarships() {
        return scholarshipRepository.countByIsActiveTrue();
    }

    public ScholarshipResponse mapToResponse(Scholarship s) {
        List<String> eligibilityCriteria = s.getEligibilityCriteria() == null
                ? List.of()
                : s.getEligibilityCriteria().stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(ArrayList::new));

        return ScholarshipResponse.builder()
                .id(s.getId())
                .title(s.getTitle())
                .amount(s.getAmount())
                .eligibilityCriteria(eligibilityCriteria)
                .deadline(s.getDeadline())
                .description(s.getDescription())
                .provider(s.getProvider())
                .category(s.getCategory())
                .requiredDocuments(mapRequiredDocuments(s.getRequiredDocuments()))
                .requirements(s.getRequirements())
                .isActive(s.getIsActive())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private List<String> normalizeRequiredDocuments(List<String> documentNames) {
        if (documentNames == null || documentNames.isEmpty()) {
            throw new IllegalArgumentException("At least one required document is required.");
        }

        List<String> normalized = documentNames.stream()
                .map(name -> name == null ? null : name.trim())
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("At least one required document is required.");
        }

        return normalized;
    }

    private List<RequiredDocument> buildRequiredDocuments(List<String> documentNames) {
        return documentNames.stream()
                .map(name -> RequiredDocument.builder().name(name).build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<RequiredDocumentResponse> mapRequiredDocuments(List<RequiredDocument> documents) {
        if (documents == null) {
            return List.of();
        }

        return documents.stream()
                .sorted(Comparator.comparing(RequiredDocument::getId, Comparator.nullsLast(Long::compareTo)))
                .map(document -> RequiredDocumentResponse.builder()
                        .name(document.getName())
                        .build())
                .collect(Collectors.toList());
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
}
