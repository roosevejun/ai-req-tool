package com.tongtu.docgen.project.service;

import com.tongtu.docgen.project.mapper.ProjectMapper;
import com.tongtu.docgen.project.mapper.RequirementMapper;
import com.tongtu.docgen.project.model.entity.ProjectEntity;
import com.tongtu.docgen.project.model.entity.RequirementEntity;
import com.tongtu.docgen.project.model.entity.RequirementVersionEntity;
import com.tongtu.docgen.system.model.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RequirementService {
    private final RequirementMapper requirementMapper;
    private final ProjectMapper projectMapper;

    public RequirementService(RequirementMapper requirementMapper, ProjectMapper projectMapper) {
        this.requirementMapper = requirementMapper;
        this.projectMapper = projectMapper;
    }

    public List<RequirementEntity> listByProjectId(Long projectId) {
        ensureProject(projectId);
        return requirementMapper.listByProjectId(projectId);
    }

    public RequirementEntity getById(Long id) {
        RequirementEntity req = requirementMapper.findById(id);
        if (req == null) {
            throw new IllegalArgumentException("Requirement not found.");
        }
        return req;
    }

    public List<RequirementVersionEntity> listVersions(Long requirementId) {
        getById(requirementId);
        return requirementMapper.listVersions(requirementId);
    }

    public RequirementVersionEntity getVersionById(Long requirementId, Long versionId) {
        getById(requirementId);
        RequirementVersionEntity v = requirementMapper.findVersionById(requirementId, versionId);
        if (v == null) {
            throw new IllegalArgumentException("Requirement version not found.");
        }
        return v;
    }

    public Long createRequirement(Long projectId, String title, String summary, String priority, String status, Long assigneeUserId, UserContext operator) {
        ensureProject(projectId);
        int seq = requirementMapper.nextSeqByProjectId(projectId);
        RequirementEntity req = new RequirementEntity();
        req.setProjectId(projectId);
        req.setRequirementNo("REQ-" + projectId + "-" + String.format("%03d", seq));
        req.setTitle(title);
        req.setSummary(summary);
        req.setPriority(priority == null || priority.isBlank() ? "P2" : priority);
        req.setStatus(status == null || status.isBlank() ? "DRAFT" : status);
        req.setAssigneeUserId(assigneeUserId);
        req.setReporterUserId(operator.getUserId());
        req.setCreatedBy(operator.getUserId());
        req.setUpdatedBy(operator.getUserId());
        requirementMapper.insert(req);
        return req.getId();
    }

    public void updateRequirement(Long id, String title, String summary, String priority, String status, Long assigneeUserId, UserContext operator) {
        RequirementEntity old = getById(id);
        RequirementEntity req = new RequirementEntity();
        req.setId(id);
        req.setTitle(title == null ? old.getTitle() : title);
        req.setSummary(summary == null ? old.getSummary() : summary);
        req.setPriority(priority == null ? old.getPriority() : priority);
        req.setStatus(status == null ? old.getStatus() : status);
        req.setAssigneeUserId(assigneeUserId == null ? old.getAssigneeUserId() : assigneeUserId);
        req.setUpdatedBy(operator.getUserId());
        requirementMapper.update(req);
    }

    @Transactional
    public Long addVersion(Long requirementId, String versionNo, String prdMarkdown, String changeSummary, String sourceJobId, UserContext operator) {
        RequirementEntity req = getById(requirementId);
        requirementMapper.clearCurrentVersion(requirementId);

        RequirementVersionEntity entity = new RequirementVersionEntity();
        entity.setRequirementId(requirementId);
        entity.setVersionNo(versionNo);
        entity.setPrdMarkdown(prdMarkdown);
        entity.setChangeSummary(changeSummary);
        entity.setSourceJobId(sourceJobId);
        entity.setGeneratedBy(operator.getUserId());
        entity.setIsCurrent(true);
        requirementMapper.insertVersion(entity);

        requirementMapper.updateCurrentVersion(req.getId(), entity.getId(), operator.getUserId());
        return entity.getId();
    }

    public String nextVersionNo(Long requirementId) {
        List<RequirementVersionEntity> versions = requirementMapper.listVersions(requirementId);
        if (versions.isEmpty()) {
            return "V1.0";
        }
        String current = versions.get(0).getVersionNo();
        if (current == null || current.isBlank()) {
            return "V1.0";
        }
        String norm = current.toUpperCase().startsWith("V") ? current.substring(1) : current;
        String[] parts = norm.split("\\.");
        try {
            if (parts.length == 1) {
                return "V" + parts[0] + ".1";
            }
            if (parts.length == 2) {
                int patch = Integer.parseInt(parts[1]) + 1;
                return "V" + parts[0] + "." + patch;
            }
            int last = Integer.parseInt(parts[parts.length - 1]) + 1;
            parts[parts.length - 1] = String.valueOf(last);
            return "V" + String.join(".", parts);
        } catch (Exception ignore) {
            return "V1.0";
        }
    }

    private void ensureProject(Long projectId) {
        ProjectEntity p = projectMapper.findById(projectId);
        if (p == null) {
            throw new IllegalArgumentException("Project not found.");
        }
    }
}

