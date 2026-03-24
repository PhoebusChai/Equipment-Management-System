package com.ems.modules.booking.repository;

import com.ems.modules.booking.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    List<BookingEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<BookingEntity> findByApproverIdOrderByCreatedAtDesc(Long approverId);

    List<BookingEntity> findByResourceTypeAndResourceIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            String resourceType,
            Long resourceId,
            List<String> statuses,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    List<BookingEntity> findByStatusInAndEndTimeBefore(List<String> statuses, LocalDateTime endTime);

    long countByResourceTypeAndResourceIdAndStatusInAndEndTimeAfter(
            String resourceType,
            Long resourceId,
            List<String> statuses,
            LocalDateTime endTime
    );
}

