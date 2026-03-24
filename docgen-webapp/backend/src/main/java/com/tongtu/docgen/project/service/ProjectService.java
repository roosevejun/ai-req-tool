package com.tongtu.docgen.project.service;

import com.tongtu.docgen.project.mapper.ProjectMapper;
import com.tongtu.docgen.project.model.entity.ProjectEntity;
import com.tongtu.docgen.system.model.UserContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {
    private final ProjectMapper projectMapper;

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
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

    public Long createProject(String projectKey, String projectName, String description, String visibility, UserContext operator) {
        if (projectMapper.findByKey(projectKey) != null) {
            throw new IllegalArgumentException("Project key already exists.");
        }
        ProjectEntity p = new ProjectEntity();
        p.setProjectKey(projectKey);
        p.setProjectName(projectName);
        p.setDescription(description);
        p.setVisibility(visibility == null || visibility.isBlank() ? "PRIVATE" : visibility);
        p.setStatus("ACTIVE");
        p.setOwnerUserId(operator.getUserId());
        p.setCreatedBy(operator.getUserId());
        p.setUpdatedBy(operator.getUserId());
        projectMapper.insert(p);
        return p.getId();
    }

    public void updateProject(Long id, String projectName, String description, String visibility, String status, Long ownerUserId, UserContext operator) {
        ProjectEntity old = getById(id);
        ProjectEntity p = new ProjectEntity();
        p.setId(id);
        p.setProjectName(projectName == null ? old.getProjectName() : projectName);
        p.setDescription(description == null ? old.getDescription() : description);
        p.setVisibility(visibility == null ? old.getVisibility() : visibility);
        p.setStatus(status == null ? old.getStatus() : status);
        p.setOwnerUserId(ownerUserId == null ? old.getOwnerUserId() : ownerUserId);
        p.setUpdatedBy(operator.getUserId());
        projectMapper.update(p);
    }
}


