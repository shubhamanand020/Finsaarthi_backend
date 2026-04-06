package com.finsaarthi.repository;

import com.finsaarthi.entity.Scholarship;
import com.finsaarthi.enums.ScholarshipCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScholarshipRepository extends JpaRepository<Scholarship, Long> {

    List<Scholarship> findByIsActiveTrue();

    List<Scholarship> findByIsActiveTrueOrderByDeadlineAsc();

    List<Scholarship> findByCategoryEnum(ScholarshipCategory category);

    List<Scholarship> findByCategoryEnumAndIsActiveTrue(ScholarshipCategory category);

    @Query("""
            SELECT s FROM Scholarship s
            WHERE s.isActive = true
            AND (
                LOWER(s.title)       LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(s.provider)    LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    List<Scholarship> searchActiveScholarships(@Param("keyword") String keyword);

    @Query("""
            SELECT s FROM Scholarship s
            WHERE s.isActive = true
            AND s.deadline >= :today
            ORDER BY s.deadline ASC
            """)
    List<Scholarship> findActiveNonExpiredScholarships(@Param("today") LocalDate today);

    boolean existsByTitleAndProvider(String title, String provider);

    long countByIsActiveTrue();
}
