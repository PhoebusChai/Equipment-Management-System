package com.ems.modules.review.repository;

import com.ems.modules.review.entity.LabReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabReviewRepository extends JpaRepository<LabReviewEntity, Long> {
    Optional<LabReviewEntity> findByBookingId(Long bookingId);
    List<LabReviewEntity> findByLabIdOrderByCreatedAtDesc(Long labId);
    List<LabReviewEntity> findByLabIdInOrderByCreatedAtDesc(List<Long> labIds);
}
