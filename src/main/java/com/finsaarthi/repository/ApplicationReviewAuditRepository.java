package com.finsaarthi.repository;

import com.finsaarthi.entity.ApplicationReviewAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationReviewAuditRepository extends JpaRepository<ApplicationReviewAudit, Long> {
}
