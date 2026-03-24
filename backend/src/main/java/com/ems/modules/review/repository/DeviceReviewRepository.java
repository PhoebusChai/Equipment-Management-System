package com.ems.modules.review.repository;

import com.ems.modules.review.entity.DeviceReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceReviewRepository extends JpaRepository<DeviceReviewEntity, Long> {
    Optional<DeviceReviewEntity> findByBookingId(Long bookingId);
    List<DeviceReviewEntity> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);
    List<DeviceReviewEntity> findByDeviceIdInOrderByCreatedAtDesc(List<Long> deviceIds);
}
