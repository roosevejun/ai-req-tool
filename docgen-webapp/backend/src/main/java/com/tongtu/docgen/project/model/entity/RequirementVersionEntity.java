package com.tongtu.docgen.project.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RequirementVersionEntity {
    private Long id;
    private Long requirementId;
    private String versionNo;
    private String prdMarkdown;
    private String changeSummary;
    private String sourceJobId;
    private Long generatedBy;
    private LocalDateTime generatedAt;
    private Boolean isCurrent;
}


