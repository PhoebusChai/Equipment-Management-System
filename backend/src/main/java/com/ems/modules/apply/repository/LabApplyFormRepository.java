package com.ems.modules.apply.repository;

import com.ems.modules.apply.entity.LabApplyFormEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabApplyFormRepository extends JpaRepository<LabApplyFormEntity, Long> {
    List<LabApplyFormEntity> findByStatusAndLabNameIgnoreCase(String status, String labName);
}

