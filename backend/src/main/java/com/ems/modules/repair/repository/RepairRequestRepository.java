package com.ems.modules.repair.repository;

import com.ems.modules.repair.entity.RepairRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairRequestRepository extends JpaRepository<RepairRequestEntity, Long> {
    List<RepairRequestEntity> findByConfirmerIdOrderByCreatedAtDesc(Long confirmerId);
}

