package com.tongtu.docgen.project.mapper;

import com.tongtu.docgen.project.model.entity.RequirementEntity;
import com.tongtu.docgen.project.model.entity.RequirementVersionEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RequirementMapper {
    @Select("""
            SELECT id, project_id AS projectId, requirement_no AS requirementNo, title, summary, priority, status,
                   assignee_user_id AS assigneeUserId, reporter_user_id AS reporterUserId, current_version_id AS currentVersionId,
                   created_by AS createdBy, updated_by AS updatedBy, created_at AS createdAt, updated_at AS updatedAt
            FROM rm_requirement
            WHERE project_id = #{projectId}
            ORDER BY id DESC
            """)
    List<RequirementEntity> listByProjectId(@Param("projectId") Long projectId);

    @Select("""
            SELECT id, project_id AS projectId, requirement_no AS requirementNo, title, summary, priority, status,
                   assignee_user_id AS assigneeUserId, reporter_user_id AS reporterUserId, current_version_id AS currentVersionId,
                   created_by AS createdBy, updated_by AS updatedBy, created_at AS createdAt, updated_at AS updatedAt
            FROM rm_requirement
            WHERE id = #{id}
            LIMIT 1
            """)
    RequirementEntity findById(@Param("id") Long id);

    @Insert("""
            INSERT INTO rm_requirement(project_id, requirement_no, title, summary, priority, status, assignee_user_id, reporter_user_id, created_by, updated_by, created_at, updated_at)
            VALUES(#{projectId}, #{requirementNo}, #{title}, #{summary}, #{priority}, #{status}, #{assigneeUserId}, #{reporterUserId}, #{createdBy}, #{updatedBy}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RequirementEntity entity);

    @Update("""
            UPDATE rm_requirement
            SET title = #{title},
                summary = #{summary},
                priority = #{priority},
                status = #{status},
                assignee_user_id = #{assigneeUserId},
                updated_by = #{updatedBy},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int update(RequirementEntity entity);

    @Select("""
            SELECT COUNT(1) + 1
            FROM rm_requirement
            WHERE project_id = #{projectId}
            """)
    int nextSeqByProjectId(@Param("projectId") Long projectId);

    @Insert("""
            INSERT INTO rm_requirement_version(requirement_id, version_no, prd_markdown, change_summary, source_job_id, generated_by, generated_at, is_current)
            VALUES(#{requirementId}, #{versionNo}, #{prdMarkdown}, #{changeSummary}, #{sourceJobId}, #{generatedBy}, NOW(), #{isCurrent})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertVersion(RequirementVersionEntity entity);

    @Select("""
            SELECT id, requirement_id AS requirementId, version_no AS versionNo, prd_markdown AS prdMarkdown,
                   change_summary AS changeSummary, source_job_id AS sourceJobId, generated_by AS generatedBy,
                   generated_at AS generatedAt, is_current AS isCurrent
            FROM rm_requirement_version
            WHERE requirement_id = #{requirementId}
            ORDER BY id DESC
            """)
    List<RequirementVersionEntity> listVersions(@Param("requirementId") Long requirementId);

    @Select("""
            SELECT id, requirement_id AS requirementId, version_no AS versionNo, prd_markdown AS prdMarkdown,
                   change_summary AS changeSummary, source_job_id AS sourceJobId, generated_by AS generatedBy,
                   generated_at AS generatedAt, is_current AS isCurrent
            FROM rm_requirement_version
            WHERE requirement_id = #{requirementId} AND id = #{versionId}
            LIMIT 1
            """)
    RequirementVersionEntity findVersionById(@Param("requirementId") Long requirementId, @Param("versionId") Long versionId);

    @Update("""
            UPDATE rm_requirement_version
            SET is_current = FALSE
            WHERE requirement_id = #{requirementId}
            """)
    int clearCurrentVersion(@Param("requirementId") Long requirementId);

    @Update("""
            UPDATE rm_requirement
            SET current_version_id = #{versionId},
                updated_by = #{updatedBy},
                updated_at = NOW()
            WHERE id = #{requirementId}
            """)
    int updateCurrentVersion(@Param("requirementId") Long requirementId, @Param("versionId") Long versionId, @Param("updatedBy") Long updatedBy);
}

