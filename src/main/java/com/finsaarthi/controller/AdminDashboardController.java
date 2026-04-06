package com.finsaarthi.controller;

import com.finsaarthi.dto.response.AdminDashboardStatsResponse;
import com.finsaarthi.dto.response.ApiResponse;
import com.finsaarthi.dto.response.ApplicationTrendResponse;
import com.finsaarthi.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getDashboardStats() {
        log.info("Admin requested dashboard statistics");

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Dashboard statistics retrieved successfully.",
                        adminDashboardService.getDashboardStats()
                )
        );
    }

    @GetMapping("/trends")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ApplicationTrendResponse>>> getApplicationTrends() {
        log.info("Admin requested dashboard trends");

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Application trends retrieved successfully.",
                        adminDashboardService.getApplicationTrends()
                )
        );
    }
}
