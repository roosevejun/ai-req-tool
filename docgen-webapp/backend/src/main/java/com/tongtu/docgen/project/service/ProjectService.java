package com.tongtu.docgen.project.service;

import com.tongtu.docgen.project.mapper.ProjectMapper;
import com.tongtu.docgen.project.model.entity.ProjectEntity;
import com.tongtu.docgen.project.model.entity.ProjectMemberEntity;
import com.tongtu.docgen.system.mapper.SystemUserMapper;
import com.tongtu.docgen.system.model.UserContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProjectService {
    private final ProjectMapper projectMapper;
    private final SystemUserMapper systemUserMapper;

    public ProjectService(ProjectMapper projectMapper, SystemUserMapper systemUserMapper) {
        this.projectMapper = projectMapper;
        this.systemUserMapper = systemUserMapper;
    }

    public List<ProjectEntity> listProjects() {
        return projectMapper.listProjects();
    }

    public ProjectEntity getById(Long id) {
        ProjectEntity p = projectMapper.findById(id);
        if (p == null) {
            throw new IllegalArgumentException("Project not found.");
        }
        return p;
    }

    public Long createProject(
            String projectKey,
            String projectName,
            String description,
            String projectType,
            String priority,
            LocalDate startDate,
            LocalDate targetDate,
            String tags,
            Long ownerUserId,
            String visibility,
            UserContext operator
    ) {
        if (projectMapper.findByKey(projectKey) != null) {
            throw new IllegalArgumentException("Project key already exists.");
        }
        validateDateRange(startDate, targetDate);
        Long finalOwnerUserId = ownerUserId == null ? operator.getUserId() : ownerUserId;
        if (systemUserMapper.findById(finalOwnerUserId) == null) {
            throw new IllegalArgumentException("Owner user not found.");
        }

        ProjectEntity p = new ProjectEntity();
        p.setProjectKey(projectKey);
        p.setProjectName(projectName);
        p.setDescription(description);
        p.setProjectType(normalizeBlank(projectType));
        p.setPriority(priority == null || priority.isBlank() ? "P2" : priority);
        p.setStartDate(startDate);
        p.setTargetDate(targetDate);
        p.setTags(normalizeBlank(tags));
        p.setVisibility(visibility == null || visibility.isBlank() ? "PRIVATE" : visibility);
        p.setStatus("ACTIVE");
        p.setOwnerUserId(finalOwnerUserId);
        p.setCreatedBy(operator.getUserId());
        p.setUpdatedBy(operator.getUserId());
        projectMapper.insert(p);
        return p.getId();
    }

    public void updateProject(
            Long id,
            String projectName,
            String description,
            String projectType,
            String priority,
            LocalDate startDate,
            LocalDate targetDate,
            String tags,
            String visibility,
            String status,
            Long ownerUserId,
            UserContext operator
    ) {
        ProjectEntity old = getById(id);
        LocalDate finalStartDate = startDate == null ? old.getStartDate() : startDate;
        LocalDate finalTargetDate = targetDate == null ? old.getTargetDate() : targetDate;
        validateDateRange(finalStartDate, finalTargetDate);

        ProjectEntity p = new ProjectEntity();
        p.setId(id);
        p.setProjectName(projectName == null ? old.getProjectName() : projectName);
        p.setDescription(description == null ? old.getDescription() : description);
        p.setProjectType(projectType == null ? old.getProjectType() : normalizeBlank(projectType));
        p.setPriority(priority == null ? old.getPriority() : priority);
        p.setStartDate(finalStartDate);
        p.setTargetDate(finalTargetDate);
        p.setTags(tags == null ? old.getTags() : normalizeBlank(tags));
        p.setVisibility(visibility == null ? old.getVisibility() : visibility);
        p.setStatus(status == null ? old.getStatus() : status);
        p.setOwnerUserId(ownerUserId == null ? old.getOwnerUserId() : ownerUserId);
        p.setUpdatedBy(operator.getUserId());
        projectMapper.update(p);
    }

    public List<ProjectMemberEntity> listMembers(Long projectId) {
        getById(projectId);
        return projectMapper.listMembersByProjectId(projectId);
    }

    public void addMember(Long projectId, Long userId, String projectRole, UserContext operator) {
        getById(projectId);
        if (userId == null || systemUserMapper.findById(userId) == null) {
            throw new IllegalArgumentException("User not found.");
        }
        if (projectMapper.findMember(projectId, userId) != null) {
            throw new IllegalArgumentException("User is already a member of this project.");
        }
        ProjectMemberEntity member = new ProjectMemberEntity();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setProjectRole(projectRole == null || projectRole.isBlank() ? "DEV" : projectRole);
        member.setStatus("ACTIVE");
        member.setCreatedBy(operator.getUserId());
        member.setUpdatedBy(operator.getUserId());
        projectMapper.insertMember(member);
    }

    public void removeMember(Long projectId, Long userId) {
        getById(projectId);
        if (projectMapper.deleteMember(projectId, userId) <= 0) {
            throw new IllegalArgumentException("Project member not found.");
        }
    }

    public void mergeTagsByAi(Long projectId, List<String> aiTags, UserContext operator) {
        if (aiTags == null || aiTags.isEmpty()) {
            return;
        }
        ProjectEntity project = getById(projectId);
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(splitTags(project.getTags()));
        for (String tag : aiTags) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            merged.add(tag.trim());
            if (merged.size() >= 20) {
                break;
            }
        }
        String joined = String.join(",", merged);
        if (joined.length() > 512) {
            joined = joined.substring(0, 512);
            int lastComma = joined.lastIndexOf(",");
            if (lastComma > 0) {
                joined = joined.substring(0, lastComma);
            }
        }
        projectMapper.updateTags(projectId, joined, operator.getUserId());
    }

    private void validateDateRange(LocalDate startDate, LocalDate targetDate) {
        if (startDate != null && targetDate != null && targetDate.isBefore(startDate)) {
            throw new IllegalArgumentException("targetDate cannot be before startDate.");
        }
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private List<String> splitTags(String tags) {
        List<String> result = new ArrayList<>();
        if (tags == null || tags.isBlank()) {
            return result;
        }
        for (String t : tags.split(",")) {
            if (t != null && !t.isBlank()) {
                result.add(t.trim());
            }
        }
        return result;
    }
}


