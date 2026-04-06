package com.finsaarthi.service;

import com.finsaarthi.dto.request.UpdateProfileRequest;
import com.finsaarthi.dto.response.UserResponse;
import com.finsaarthi.entity.User;
import com.finsaarthi.exception.AccessDeniedException;
import com.finsaarthi.exception.DuplicateEmailException;
import com.finsaarthi.exception.InvalidCredentialsException;
import com.finsaarthi.exception.ResourceNotFoundException;
import com.finsaarthi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.finsaarthi.enums.Role;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllStudents() {
        return userRepository.findAllByRoleOrderByCreatedAtDesc(Role.STUDENT)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "email", email)
                );

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(
            Long id,
            UpdateProfileRequest request,
            String authenticatedEmail
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Only the account owner or an admin may update the profile
        if (!user.getEmail().equalsIgnoreCase(authenticatedEmail)) {
            User requester = userRepository.findByEmail(authenticatedEmail)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("User", "email", authenticatedEmail)
                    );

            if (requester.getRole() != Role.ADMIN) {
                throw new AccessDeniedException(
                        "You are not allowed to update another user's profile."
                );
            }
        }

        String newEmail = request.getEmail().toLowerCase().trim();

        // Check email uniqueness only if it changed
        if (!user.getEmail().equalsIgnoreCase(newEmail)
                && userRepository.existsByEmail(newEmail)) {
            throw new DuplicateEmailException(newEmail);
        }

        // Handle optional password change
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new InvalidCredentialsException(
                        "Current password is required to set a new password."
                );
            }

            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new InvalidCredentialsException("Current password is incorrect.");
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            log.info("Password updated for user id: {}", id);
        }

        user.setName(request.getName().trim());
        user.setEmail(newEmail);
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setEducation(request.getEducation());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setPhoto(request.getPhoto());
        user.setResume(request.getResume());

        User updated = userRepository.save(user);
        log.info("Profile updated for user id: {}", id);

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        userRepository.deleteById(id);
        log.info("User deleted with id: {}", id);
    }

    @Transactional(readOnly = true)
    public long countStudents() {
        return userRepository.countByRole(Role.STUDENT);
    }

    public UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .address(user.getAddress())
                .education(user.getEducation())
                .dateOfBirth(user.getDateOfBirth())
                .photo(user.getPhoto())
                .resume(user.getResume())
                .isVerified(user.isVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
