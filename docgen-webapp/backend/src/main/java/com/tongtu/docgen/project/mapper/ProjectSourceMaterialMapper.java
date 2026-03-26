package com.tongtu.docgen.project.mapper;

import com.tongtu.docgen.project.model.entity.ProjectSourceMaterialEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectSourceMaterialMapper {
    @Insert("""
            INSERT INTO pm_project_source_material(project_id, session_id, material_type, title, source_uri,
                                                   raw_content, ai_extracted_summary, created_by, created_at, updated_at)
            VALUES(#{projectId}, #{sessionId}, #{materialType}, #{title}, #{sourceUri},
                   #{rawContent}, #{aiExtractedSummary}, #{createdBy}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProjectSourceMaterialEntity entity);

    @Select("""
            SELECT id, project_id AS projectId, session_id AS sessionId,
                   material_type AS materialType, title, source_uri AS sourceUri,
                   raw_content AS rawContent, ai_extracted_summary AS aiExtractedSummary,
                   created_by AS createdBy, created_at AS createdAt, updated_at AS updatedAt
            FROM pm_project_source_material
            WHERE id = #{id}
            LIMIT 1
            """)
    ProjectSourceMaterialEntity findById(@Param("id") Long id);

    @Select("""
            SELECT id, project_id AS projectId, session_id AS sessionId,
                   material_type AS materialType, title, source_uri AS sourceUri,
                   raw_content AS rawContent, ai_extracted_summary AS aiExtractedSummary,
                   created_by AS createdBy, created_at AS createdAt, updated_at AS updatedAt
            FROM pm_project_source_material
            WHERE session_id = #{sessionId}
            ORDER BY id ASC
            """)
    List<ProjectSourceMaterialEntity> listBySessionId(@Param("sessionId") Long sessionId);

    @Select("""
            SELECT id, project_id AS projectId, session_id AS sessionId,
                   material_type AS materialType, title, source_uri AS sourceUri,
                   raw_content AS rawContent, ai_extracted_summary AS aiExtractedSummary,
                   created_by AS createdBy, created_at AS createdAt, updated_at AS updatedAt
            FROM pm_project_source_material
            WHERE project_id = #{projectId}
            ORDER BY id ASC
            """)
    List<ProjectSourceMaterialEntity> listByProjectId(@Param("projectId") Long projectId);

    @Delete("""
            DELETE FROM pm_project_source_material
            WHERE project_id = #{projectId}
            """)
    int deleteByProjectId(@Param("projectId") Long projectId);
}
