package com.finsaarthi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.finsaarthi.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_role", columnList = "role")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private StudentProfile studentProfile;

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private AdminProfile adminProfile;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private String name;

    @Transient
    private String phone;

    @Transient
    private String address;

    @Transient
    private String education;

    @Transient
    private String dateOfBirth;

    @Transient
    private String photo;

    @Transient
    private String resume;

    public void setStudentProfile(StudentProfile studentProfile) {
        this.studentProfile = studentProfile;
        if (studentProfile != null) {
            studentProfile.setUser(this);
        }
    }

    public void setAdminProfile(AdminProfile adminProfile) {
        this.adminProfile = adminProfile;
        if (adminProfile != null) {
            adminProfile.setUser(this);
        }
    }

    public String getName() {
        if (role == Role.ADMIN && adminProfile != null) {
            return adminProfile.getFullName();
        }
        if (studentProfile != null) {
            return studentProfile.getFullName();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (role == Role.ADMIN) {
            ensureAdminProfile().setFullName(name);
            return;
        }
        ensureStudentProfile().setFullName(name);
    }

    public String getPhone() {
        if (role == Role.ADMIN) {
            return phone;
        }
        return studentProfile != null ? studentProfile.getPhone() : phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        if (role == Role.ADMIN) {
            return;
        }
        ensureStudentProfile().setPhone(phone);
    }

    public String getAddress() {
        if (role == Role.ADMIN) {
            return address;
        }
        return studentProfile != null ? studentProfile.getAddress() : address;
    }

    public void setAddress(String address) {
        this.address = address;
        if (role == Role.ADMIN) {
            return;
        }
        ensureStudentProfile().setAddress(address);
    }

    public String getEducation() {
        if (role == Role.ADMIN) {
            return education;
        }
        return studentProfile != null ? studentProfile.getEducationDetails() : education;
    }

    public void setEducation(String education) {
        this.education = education;
        if (role == Role.ADMIN) {
            return;
        }
        ensureStudentProfile().setEducationDetails(education);
    }

    public String getDateOfBirth() {
        if (role == Role.ADMIN) {
            return dateOfBirth;
        }
        return studentProfile != null ? studentProfile.getDateOfBirth() : dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        if (role == Role.ADMIN) {
            return;
        }
        ensureStudentProfile().setDateOfBirth(dateOfBirth);
    }

    public String getPhoto() {
        if (role == Role.ADMIN) {
            return photo;
        }
        return studentProfile != null ? studentProfile.getPhotoUrl() : photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
        if (role == Role.ADMIN) {
            return;
        }
        ensureStudentProfile().setPhotoUrl(photo);
    }

    public String getResume() {
        if (role == Role.ADMIN) {
            return resume;
        }
        return studentProfile != null ? studentProfile.getResumeUrl() : resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
        if (role == Role.ADMIN) {
            return;
        }
        ensureStudentProfile().setResumeUrl(resume);
    }

    @PostLoad
    void hydrateTransientFields() {
        this.name = getName();
        this.phone = getPhone();
        this.address = getAddress();
        this.education = getEducation();
        this.dateOfBirth = getDateOfBirth();
        this.photo = getPhoto();
        this.resume = getResume();
    }

    @PrePersist
    @PreUpdate
    void syncProfiles() {
        if (role == Role.ADMIN) {
            isVerified = true;
        }

        if (role == Role.ADMIN) {
            if (name != null && !name.isBlank()) {
                ensureAdminProfile().setFullName(name);
            }
            if (adminProfile != null) {
                adminProfile.setUser(this);
            }
            return;
        }

        StudentProfile profile = ensureStudentProfile();
        if (name != null && !name.isBlank()) {
            profile.setFullName(name);
        }
        if (education != null) {
            profile.setEducationDetails(education);
        }
        if (phone != null) {
            profile.setPhone(phone);
        }
        if (address != null) {
            profile.setAddress(address);
        }
        if (dateOfBirth != null) {
            profile.setDateOfBirth(dateOfBirth);
        }
        if (photo != null) {
            profile.setPhotoUrl(photo);
        }
        if (resume != null) {
            profile.setResumeUrl(resume);
        }
        profile.setUser(this);
    }

    private StudentProfile ensureStudentProfile() {
        if (studentProfile == null) {
            studentProfile = StudentProfile.builder().build();
            studentProfile.setUser(this);
        }
        return studentProfile;
    }

    private AdminProfile ensureAdminProfile() {
        if (adminProfile == null) {
            adminProfile = AdminProfile.builder().build();
            adminProfile.setUser(this);
        }
        return adminProfile;
    }
}
