package com.tongtu.docgen.project.model.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProjectEntity {
    private Long id;
    private String projectKey;
    private String projectName;
    private String description;
    private String projectBackground;
    private String similarProducts;
    private String targetCustomerGroups;
    private String commercialValue;
    private String coreProductValue;
    private String projectType;
    private String priority;
    private LocalDate startDate;
    private LocalDate targetDate;
    private String tags;
    private String visibility;
    private String status;
    private Long ownerUserId;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


