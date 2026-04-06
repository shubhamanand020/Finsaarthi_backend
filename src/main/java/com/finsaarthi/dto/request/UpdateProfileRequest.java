package com.finsaarthi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    private String phone;

    private String address;

    private String education;

    private String dateOfBirth;

    private String photo;

    private String resume;

    // Optional password change fields
    private String currentPassword;

    @Size(min = 6, message = "New password must be at least 6 characters long")
    private String newPassword;
}