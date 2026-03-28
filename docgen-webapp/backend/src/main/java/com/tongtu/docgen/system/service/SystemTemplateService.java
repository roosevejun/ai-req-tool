package com.tongtu.docgen.system.service;

import com.tongtu.docgen.system.mapper.SystemTemplateMapper;
import com.tongtu.docgen.system.mapper.SystemTemplateVersionMapper;
import com.tongtu.docgen.system.model.UserContext;
import com.tongtu.docgen.system.model.UserContextHolder;
import com.tongtu.docgen.system.model.entity.SysTemplateEntity;
import com.tongtu.docgen.system.model.entity.SysTemplateVersionEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SystemTemplateService {
    private final SystemTemplateMapper systemTemplateMapper;
    private final SystemTemplateVersionMapper systemTemplateVersionMapper;

    public SystemTemplateService(SystemTemplateMapper systemTemplateMapper,
                                 SystemTemplateVersionMapper systemTemplateVersionMapper) {
        this.systemTemplateMapper = systemTemplateMapper;
        this.systemTemplateVersionMapper = systemTemplateVersionMapper;
    }

    public record TemplateDetail(
            SysTemplateEntity template,
            List<SysTemplateVersionEntity> versions,
            SysTemplateVersionEntity publishedVersion
    ) {
    }

    public record TemplateSnapshot(
            Long templateId,
            Long versionId,
            String versionLabel,
            String templateMarkdown,
            String variablesJson
    ) {
    }

    public List<SysTemplateEntity> listTemplates() {
        return systemTemplateMapper.listTemplates();
    }

    public TemplateDetail getTemplateDetail(Long templateId) {
        SysTemplateEntity template = getTemplate(templateId);
        List<SysTemplateVersionEntity> versions = systemTemplateVersionMapper.listByTemplateId(templateId);
        SysTemplateVersionEntity publishedVersion = systemTemplateVersionMapper.findPublishedByTemplateId(templateId);
        return new TemplateDetail(template, versions, publishedVersion);
    }

    @Transactional
    public Long createTemplate(String templateCode,
                               String templateName,
                               String templateCategory,
                               String description,
                               String scopeLevel,
                               String status,
                               String initialVersionLabel,
                               String contentMarkdown,
                               String variablesJson,
                               String notes) {
        String code = requireText(templateCode, "Template code is required.");
        if (systemTemplateMapper.findByCode(code) != null) {
            throw new IllegalArgumentException("Template code already exists.");
        }
        UserContext operator = requireOperator();
        SysTemplateEntity template = new SysTemplateEntity();
        template.setTemplateCode(code);
        template.setTemplateName(requireText(templateName, "Template name is required."));
        template.setTemplateCategory(defaultIfBlank(templateCategory, "PRD"));
        template.setDescription(normalizeBlank(description));
        template.setScopeLevel(defaultIfBlank(scopeLevel, "SYSTEM"));
        template.setStatus(defaultIfBlank(status, "DRAFT"));
        template.setLatestVersionNo(1);
        template.setPublishedVersionNo(null);
        template.setCreatedBy(operator.getUserId());
        template.setUpdatedBy(operator.getUserId());
        systemTemplateMapper.insertTemplate(template);

        SysTemplateVersionEntity version = new SysTemplateVersionEntity();
        version.setTemplateId(template.getId());
        version.setVersionNo(1);
        version.setVersionLabel(defaultIfBlank(initialVersionLabel, "v1"));
        version.setContentMarkdown(defaultIfBlank(contentMarkdown, "# 模板内容\n"));
        version.setVariablesJson(normalizeBlank(variablesJson));
        version.setNotes(normalizeBlank(notes));
        version.setStatus("DRAFT");
        version.setIsPublished(false);
        version.setCreatedBy(operator.getUserId());
        systemTemplateVersionMapper.insertVersion(version);
        return template.getId();
    }

    public void updateTemplate(Long templateId,
                               String templateName,
                               String templateCategory,
                               String description,
                               String scopeLevel,
                               String status) {
        SysTemplateEntity current = getTemplate(templateId);
        UserContext operator = requireOperator();
        SysTemplateEntity patch = new SysTemplateEntity();
        patch.setId(templateId);
        patch.setTemplateName(templateName == null ? current.getTemplateName() : requireText(templateName, "Template name is required."));
        patch.setTemplateCategory(templateCategory == null ? current.getTemplateCategory() : defaultIfBlank(templateCategory, "PRD"));
        patch.setDescription(description == null ? current.getDescription() : normalizeBlank(description));
        patch.setScopeLevel(scopeLevel == null ? current.getScopeLevel() : defaultIfBlank(scopeLevel, "SYSTEM"));
        patch.setStatus(status == null ? current.getStatus() : defaultIfBlank(status, "DRAFT"));
        patch.setUpdatedBy(operator.getUserId());
        systemTemplateMapper.updateTemplate(patch);
    }

    @Transactional
    public Long createVersion(Long templateId,
                              String versionLabel,
                              String contentMarkdown,
                              String variablesJson,
                              String notes,
                              String status) {
        SysTemplateEntity template = getTemplate(templateId);
        UserContext operator = requireOperator();
        int nextVersion = template.getLatestVersionNo() == null || template.getLatestVersionNo() < 1
                ? 1
                : template.getLatestVersionNo() + 1;
        SysTemplateVersionEntity version = new SysTemplateVersionEntity();
        version.setTemplateId(templateId);
        version.setVersionNo(nextVersion);
        version.setVersionLabel(defaultIfBlank(versionLabel, "v" + nextVersion));
        version.setContentMarkdown(defaultIfBlank(contentMarkdown, "# 模板内容\n"));
        version.setVariablesJson(normalizeBlank(variablesJson));
        version.setNotes(normalizeBlank(notes));
        version.setStatus(defaultIfBlank(status, "DRAFT"));
        version.setIsPublished(false);
        version.setCreatedBy(operator.getUserId());
        systemTemplateVersionMapper.insertVersion(version);
        systemTemplateMapper.updateVersionPointers(templateId, nextVersion, template.getPublishedVersionNo(), operator.getUserId());
        return version.getId();
    }

    @Transactional
    public void publishVersion(Long templateId, Long versionId) {
        SysTemplateEntity template = getTemplate(templateId);
        SysTemplateVersionEntity version = getTemplateVersion(versionId);
        if (!templateId.equals(version.getTemplateId())) {
            throw new IllegalArgumentException("Template version does not belong to template.");
        }
        UserContext operator = requireOperator();

        SysTemplateVersionEntity previous = systemTemplateVersionMapper.findPublishedByTemplateId(templateId);
        if (previous != null && !previous.getId().equals(versionId)) {
            previous.setIsPublished(false);
            previous.setStatus("ARCHIVED");
            systemTemplateVersionMapper.updateVersion(previous);
        }
        systemTemplateVersionMapper.clearPublishedByTemplateId(templateId);
        version.setIsPublished(true);
        version.setStatus("PUBLISHED");
        systemTemplateVersionMapper.updateVersion(version);
        template.setStatus("PUBLISHED");
        systemTemplateMapper.updateTemplate(buildTemplateStatusPatch(templateId, template, operator.getUserId(), "PUBLISHED"));
        systemTemplateMapper.updateVersionPointers(templateId, template.getLatestVersionNo(), version.getVersionNo(), operator.getUserId());
    }

    public TemplateSnapshot resolveSnapshot(Long templateId, Long versionId) {
        SysTemplateEntity template = getTemplate(templateId);
        SysTemplateVersionEntity version = versionId == null
                ? systemTemplateVersionMapper.findPublishedByTemplateId(templateId)
                : getTemplateVersion(versionId);
        if (version == null) {
            List<SysTemplateVersionEntity> versions = systemTemplateVersionMapper.listByTemplateId(templateId);
            if (!versions.isEmpty()) {
                version = versions.get(0);
            }
        }
        if (version == null) {
            throw new IllegalArgumentException("Template version not found.");
        }
        if (!templateId.equals(version.getTemplateId())) {
            throw new IllegalArgumentException("Template version does not belong to template.");
        }
        return new TemplateSnapshot(
                template.getId(),
                version.getId(),
                defaultIfBlank(version.getVersionLabel(), "v" + version.getVersionNo()),
                defaultIfBlank(version.getContentMarkdown(), "# 模板内容\n"),
                normalizeBlank(version.getVariablesJson())
        );
    }

    public void updateVersion(Long templateId,
                              Long versionId,
                              String versionLabel,
                              String contentMarkdown,
                              String variablesJson,
                              String notes,
                              String status) {
        getTemplate(templateId);
        SysTemplateVersionEntity version = getTemplateVersion(versionId);
        if (!templateId.equals(version.getTemplateId())) {
            throw new IllegalArgumentException("Template version does not belong to template.");
        }
        version.setVersionLabel(versionLabel == null ? version.getVersionLabel() : defaultIfBlank(versionLabel, "v" + version.getVersionNo()));
        version.setContentMarkdown(contentMarkdown == null ? version.getContentMarkdown() : defaultIfBlank(contentMarkdown, "# 模板内容\n"));
        version.setVariablesJson(variablesJson == null ? version.getVariablesJson() : normalizeBlank(variablesJson));
        version.setNotes(notes == null ? version.getNotes() : normalizeBlank(notes));
        version.setStatus(status == null ? version.getStatus() : defaultIfBlank(status, "DRAFT"));
        systemTemplateVersionMapper.updateVersion(version);
    }

    public SysTemplateEntity getTemplate(Long templateId) {
        SysTemplateEntity template = systemTemplateMapper.findById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template not found.");
        }
        return template;
    }

    public SysTemplateVersionEntity getTemplateVersion(Long versionId) {
        SysTemplateVersionEntity version = systemTemplateVersionMapper.findById(versionId);
        if (version == null) {
            throw new IllegalArgumentException("Template version not found.");
        }
        return version;
    }

    private UserContext requireOperator() {
        UserContext operator = UserContextHolder.get();
        if (operator == null) {
            throw new IllegalStateException("Current operator not found.");
        }
        return operator;
    }

    private String requireText(String text, String message) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return text.trim();
    }

    private String defaultIfBlank(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text.trim();
    }

    private String normalizeBlank(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private SysTemplateEntity buildTemplateStatusPatch(Long templateId, SysTemplateEntity current, Long updatedBy, String status) {
        SysTemplateEntity patch = new SysTemplateEntity();
        patch.setId(templateId);
        patch.setTemplateName(current.getTemplateName());
        patch.setTemplateCategory(current.getTemplateCategory());
        patch.setDescription(current.getDescription());
        patch.setScopeLevel(current.getScopeLevel());
        patch.setStatus(status);
        patch.setUpdatedBy(updatedBy);
        return patch;
    }
}
