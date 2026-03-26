package com.tongtu.docgen.project.service;

import com.tongtu.docgen.project.mapper.ProjectMapper;
import com.tongtu.docgen.project.mapper.ProjectChatSessionMapper;
import com.tongtu.docgen.project.mapper.ProjectSourceMaterialMapper;
import com.tongtu.docgen.project.model.entity.ProjectEntity;
import com.tongtu.docgen.project.model.entity.ProjectMemberEntity;
import com.tongtu.docgen.llm.AgentClient;
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
    private final ProjectChatSessionMapper projectChatSessionMapper;
    private final ProjectSourceMaterialMapper projectSourceMaterialMapper;
    private final SystemUserMapper systemUserMapper;
    private final AgentClient agentClient;

    public ProjectService(ProjectMapper projectMapper,
                          ProjectChatSessionMapper projectChatSessionMapper,
                          ProjectSourceMaterialMapper projectSourceMaterialMapper,
                          SystemUserMapper systemUserMapper,
                          AgentClient agentClient) {
        this.projectMapper = projectMapper;
        this.projectChatSessionMapper = projectChatSessionMapper;
        this.projectSourceMaterialMapper = projectSourceMaterialMapper;
        this.systemUserMapper = systemUserMapper;
        this.agentClient = agentClient;
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
            String projectBackground,
            String similarProducts,
            String targetCustomerGroups,
            String commercialValue,
            String coreProductValue,
            String projectType,
            String priority,
            LocalDate startDate,
            LocalDate targetDate,
            String tags,
            Long ownerUserId,
            String visibility,
            UserContext operator
    ) {
        return createProjectInternal(
                projectKey,
                projectName,
                description,
                projectBackground,
                similarProducts,
                targetCustomerGroups,
                commercialValue,
                coreProductValue,
                null,
                projectType,
                "FORM",
                priority,
                startDate,
                targetDate,
                tags,
                ownerUserId,
                visibility,
                operator
        );
    }

    public Long createProjectFromAiConversation(String projectKey,
                                                String projectName,
                                                String description,
                                                String projectBackground,
                                                String similarProducts,
                                                String targetCustomerGroups,
                                                String commercialValue,
                                                String coreProductValue,
                                                String businessKnowledgeSummary,
                                                String projectType,
                                                String priority,
                                                LocalDate startDate,
                                                LocalDate targetDate,
                                                String tags,
                                                Long ownerUserId,
                                                String visibility,
                                                UserContext operator) {
        return createProjectInternal(
                projectKey,
                projectName,
                description,
                projectBackground,
                similarProducts,
                targetCustomerGroups,
                commercialValue,
                coreProductValue,
                businessKnowledgeSummary,
                projectType,
                "AI_CHAT",
                priority,
                startDate,
                targetDate,
                tags,
                ownerUserId,
                visibility,
                operator
        );
    }

    private Long createProjectInternal(String projectKey,
                                       String projectName,
                                       String description,
                                       String projectBackground,
                                       String similarProducts,
                                       String targetCustomerGroups,
                                       String commercialValue,
                                       String coreProductValue,
                                       String businessKnowledgeSummary,
                                       String projectType,
                                       String creationMode,
                                       String priority,
                                       LocalDate startDate,
                                       LocalDate targetDate,
                                       String tags,
                                       Long ownerUserId,
                                       String visibility,
                                       UserContext operator) {
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
        p.setDescription(normalizeBlank(description));
        p.setProjectBackground(normalizeBlank(projectBackground));
        p.setSimilarProducts(normalizeBlank(similarProducts));
        p.setTargetCustomerGroups(normalizeBlank(targetCustomerGroups));
        p.setCommercialValue(normalizeBlank(commercialValue));
        p.setCoreProductValue(normalizeBlank(coreProductValue));
        p.setBusinessKnowledgeSummary(normalizeBlank(businessKnowledgeSummary));
        p.setProjectType(normalizeBlank(projectType));
        p.setCreationMode(creationMode == null || creationMode.isBlank() ? "FORM" : creationMode);
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
            String projectBackground,
            String similarProducts,
            String targetCustomerGroups,
            String commercialValue,
            String coreProductValue,
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
        p.setDescription(description == null ? old.getDescription() : normalizeBlank(description));
        p.setProjectBackground(projectBackground == null ? old.getProjectBackground() : normalizeBlank(projectBackground));
        p.setSimilarProducts(similarProducts == null ? old.getSimilarProducts() : normalizeBlank(similarProducts));
        p.setTargetCustomerGroups(targetCustomerGroups == null ? old.getTargetCustomerGroups() : normalizeBlank(targetCustomerGroups));
        p.setCommercialValue(commercialValue == null ? old.getCommercialValue() : normalizeBlank(commercialValue));
        p.setCoreProductValue(coreProductValue == null ? old.getCoreProductValue() : normalizeBlank(coreProductValue));
        p.setBusinessKnowledgeSummary(old.getBusinessKnowledgeSummary());
        p.setProjectType(projectType == null ? old.getProjectType() : normalizeBlank(projectType));
        p.setCreationMode(old.getCreationMode());
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

    public void deleteProject(Long projectId) {
        getById(projectId);
        // Force-remove project AI artifacts before deleting the project itself.
        // Chat messages are deleted via pm_project_chat_session ON DELETE CASCADE.
        projectSourceMaterialMapper.deleteByProjectId(projectId);
        projectChatSessionMapper.deleteByProjectId(projectId);
        if (projectMapper.deleteProject(projectId) <= 0) {
            throw new IllegalArgumentException("Project not found.");
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

    public String buildProductContext(ProjectEntity project) {
        if (project == null) {
            return "";
        }
        List<String> sections = new ArrayList<>();
        appendContextSection(sections, "项目名称", project.getProjectName());
        appendContextSection(sections, "项目描述", project.getDescription());
        appendContextSection(sections, "项目背景", project.getProjectBackground());
        appendContextSection(sections, "类似产品参考", project.getSimilarProducts());
        appendContextSection(sections, "目标客户群体", project.getTargetCustomerGroups());
        appendContextSection(sections, "商业价值", project.getCommercialValue());
        appendContextSection(sections, "产品核心价值", project.getCoreProductValue());
        return String.join("\n\n", sections).trim();
    }

    public AgentClient.ProjectProductGuideResult guideProjectProductInfo(String traceId,
                                                                         String projectName,
                                                                         String description,
                                                                         String projectBackground,
                                                                         String similarProducts,
                                                                         String targetCustomerGroups,
                                                                         String commercialValue,
                                                                         String coreProductValue,
                                                                         List<AgentClient.ProjectProductAnswer> answers) {
        return agentClient.guideProjectProductInfo(
                traceId,
                projectName,
                description,
                projectBackground,
                similarProducts,
                targetCustomerGroups,
                commercialValue,
                coreProductValue,
                answers
        );
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

    private void appendContextSection(List<String> sections, String title, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        sections.add("【" + title + "】\n" + value.trim());
    }
}


