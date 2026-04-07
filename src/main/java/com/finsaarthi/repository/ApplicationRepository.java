package com.finsaarthi.repository;

import com.finsaarthi.entity.Application;
import com.finsaarthi.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByStudentId(Long studentId);

    List<Application> findByStudentIdOrderBySubmittedAtDesc(Long studentId);

    List<Application> findByScholarshipId(Long scholarshipId);

    List<Application> findByStatus(ApplicationStatus status);

    List<Application> findAllByOrderBySubmittedAtDesc();

    Optional<Application> findByStudentIdAndScholarshipId(
            Long studentId, Long scholarshipId
    );

    boolean existsByStudentIdAndScholarshipId(Long studentId, Long scholarshipId);

    long countByStatus(ApplicationStatus status);

    long countByStudentId(Long studentId);

    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.student s
            JOIN FETCH a.scholarship sc
            WHERE a.status = :status
            ORDER BY a.submittedAt DESC
            """)
    List<Application> findByStatusWithDetails(
            @Param("status") ApplicationStatus status
    );

    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.student s
            JOIN FETCH a.scholarship sc
            ORDER BY a.submittedAt DESC
            """)
    List<Application> findAllWithDetails();

    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.student s
            JOIN FETCH a.scholarship sc
            WHERE s.id = :studentId
            ORDER BY a.submittedAt DESC
            """)
    List<Application> findByStudentIdWithDetails(
            @Param("studentId") Long studentId
    );

    @Query("""
            SELECT DISTINCT a FROM Application a
            JOIN FETCH a.student s
            JOIN FETCH a.scholarship sc
            LEFT JOIN FETCH a.applicationDocuments docs
            WHERE a.id = :id
            """)
    Optional<Application> findByIdWithDetails(@Param("id") Long id);
}
