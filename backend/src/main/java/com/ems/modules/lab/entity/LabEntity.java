package com.ems.modules.lab.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "labs")
public class LabEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lab_code", nullable = false, unique = true)
    private String labCode;

    @Column(name = "lab_name", nullable = false)
    private String labName;

    @Column(name = "lab_type", nullable = false)
    private String labType;

    @Column(name = "building")
    private String building;

    @Column(name = "college")
    private String college;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "manager_teacher_id")
    private Long managerTeacherId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "description")
    private String description;

    @Column(name = "image_urls")
    private String imageUrls;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getLabCode() {
        return labCode;
    }
    public void setLabCode(String labCode) { this.labCode = labCode; }

    public String getLabName() {
        return labName;
    }
    public void setLabName(String labName) { this.labName = labName; }

    public String getLabType() {
        return labType;
    }
    public void setLabType(String labType) { this.labType = labType; }

    public String getBuilding() {
        return building;
    }
    public void setBuilding(String building) { this.building = building; }

    public String getCollege() {
        return college;
    }
    public void setCollege(String college) { this.college = college; }

    public Integer getCapacity() {
        return capacity;
    }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Long getManagerTeacherId() {
        return managerTeacherId;
    }
    public void setManagerTeacherId(Long managerTeacherId) { this.managerTeacherId = managerTeacherId; }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrls() {
        return imageUrls;
    }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
}

