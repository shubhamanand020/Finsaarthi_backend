package com.finsaarthi.repository;

import com.finsaarthi.entity.OtpVerification;
import com.finsaarthi.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    List<OtpVerification> findByEmailAndPurpose(String email, OtpPurpose purpose);

    Optional<OtpVerification> findFirstByEmailAndPurposeOrderByIdDesc(
            String email,
            OtpPurpose purpose
    );

    Optional<OtpVerification> findFirstByEmailAndPurposeAndVerifiedTrueOrderByIdDesc(
            String email,
            OtpPurpose purpose
    );

    void deleteByEmailAndPurpose(String email, OtpPurpose purpose);
}
