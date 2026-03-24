package com.ems.modules.lab.repository;

import com.ems.modules.lab.entity.LabEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabRepository extends JpaRepository<LabEntity, Long> {
    boolean existsByLabCode(String labCode);
    LabEntity findFirstByLabNameIgnoreCase(String labName);
}

