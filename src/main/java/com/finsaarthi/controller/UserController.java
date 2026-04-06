package com.finsaarthi.controller;

import com.finsaarthi.dto.request.UpdateProfileRequest;
import com.finsaarthi.dto.response.ApiResponse;
import com.finsaarthi.dto.response.UserResponse;
import com.finsaarthi.exception.AccessDeniedException;
import com.finsaarthi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/users
    // ADMIN only — retrieves all registered users
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        log.info("Admin requested all users");

        List<UserResponse> users = userService.getAllUsers();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Users retrieved successfully.",
                        users
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/users/students
    // ADMIN only — retrieves all students
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllStudents() {
        log.info("Admin requested all students");

        List<UserResponse> students = userService.getAllStudents();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Students retrieved successfully.",
                        students
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/users/stats
    // ADMIN only — returns user count statistics
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUserStats() {
        long studentCount = userService.countStudents();

        Map<String, Long> stats = Map.of(
                "totalStudents", studentCount
        );

        return ResponseEntity.ok(
                ApiResponse.success("User statistics retrieved successfully.", stats)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/users/me
    // Authenticated — returns the currently logged-in user's profile
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Current user profile requested by: {}", userDetails.getUsername());

        UserResponse user = userService.getUserByEmail(userDetails.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success("Profile retrieved successfully.", user)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/users/{id}
    // Authenticated — retrieves a user by ID (owner or admin only)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("User profile id: {} requested by: {}", id, userDetails.getUsername());

        UserResponse requestingUser = userService.getUserByEmail(
                userDetails.getUsername()
        );

        boolean isAdmin = requestingUser.getRole().equals("ADMIN");
        boolean isOwner = requestingUser.getId().equals(id);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "You do not have permission to view this user's profile."
            );
        }

        UserResponse user = userService.getUserById(id);

        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully.", user)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/users/{id}/profile
    // Authenticated — updates user profile (owner or admin only)
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{id}/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Profile update requested for user id: {} by: {}",
                id, userDetails.getUsername());

        UserResponse updated = userService.updateProfile(
                id,
                request,
                userDetails.getUsername()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Profile updated successfully.", updated)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/users/{id}
    // ADMIN only — deletes a user account
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id
    ) {
        log.info("Admin requested deletion of user id: {}", id);

        userService.deleteUser(id);

        return ResponseEntity.ok(
                ApiResponse.success("User deleted successfully.")
        );
    }
}