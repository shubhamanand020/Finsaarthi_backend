package com.finsaarthi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String phone;
    private String address;
    private String education;
    private String dateOfBirth;
    private String photo;
    private String resume;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}
