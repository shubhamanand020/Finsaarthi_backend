package com.finsaarthi.service;

import com.finsaarthi.dto.response.AdminDashboardStatsResponse;
import com.finsaarthi.dto.response.ApplicationTrendResponse;
import com.finsaarthi.entity.Application;
import com.finsaarthi.enums.ApplicationStatus;
import com.finsaarthi.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        long totalApplications = applicationRepository.count();
        long approvedApplications = applicationRepository.countByStatus(ApplicationStatus.APPROVED);
        long rejectedApplications = applicationRepository.countByStatus(ApplicationStatus.REJECTED);
        long pendingApplications = applicationRepository.countByStatus(ApplicationStatus.PENDING)
                + applicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW);

        double approvalRate = totalApplications == 0
                ? 0.0
                : roundToSingleDecimal((approvedApplications * 100.0) / totalApplications);

        return AdminDashboardStatsResponse.builder()
                .totalApplications(totalApplications)
                .approvedApplications(approvedApplications)
                .rejectedApplications(rejectedApplications)
                .pendingApplications(pendingApplications)
                .approvalRate(approvalRate)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ApplicationTrendResponse> getApplicationTrends() {
        Map<LocalDate, Long> groupedApplications = applicationRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        application -> application.getSubmittedAt().toLocalDate(),
                        Collectors.counting()
                ));

        return groupedApplications.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> ApplicationTrendResponse.builder()
                        .date(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    private double roundToSingleDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
