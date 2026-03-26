package com.tongtu.docgen.project.mapper;

import com.tongtu.docgen.project.model.entity.ProjectChatSessionEntity;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProjectChatSessionMapper {
    @Insert("""
            INSERT INTO pm_project_chat_session(project_id, job_id, status, assistant_summary,
                                                business_knowledge_summary, structured_info_json,
                                                ready_to_create, created_by, created_at, updated_at)
            VALUES(#{projectId}, #{jobId}, #{status}, #{assistantSummary},
                   #{businessKnowledgeSummary}, #{structuredInfoJson},
                   #{readyToCreate}, #{createdBy}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProjectChatSessionEntity entity);

    @Select("""
            SELECT id, project_id AS projectId, job_id AS jobId, status,
                   assistant_summary AS assistantSummary,
                   business_knowledge_summary AS businessKnowledgeSummary,
                   structured_info_json AS structuredInfoJson,
                   ready_to_create AS readyToCreate,
                   created_by AS createdBy, created_at AS createdAt, updated_at AS updatedAt
            FROM pm_project_chat_session
            WHERE id = #{id}
            LIMIT 1
            """)
    ProjectChatSessionEntity findById(@Param("id") Long id);

    @Select("""
            SELECT id, project_id AS projectId, job_id AS jobId, status,
                   assistant_summary AS assistantSummary,
                   business_knowledge_summary AS businessKnowledgeSummary,
                   structured_info_json AS structuredInfoJson,
                   ready_to_create AS readyToCreate,
                   created_by AS createdBy, created_at AS createdAt, updated_at AS updatedAt
            FROM pm_project_chat_session
            WHERE job_id = #{jobId}
            LIMIT 1
            """)
    ProjectChatSessionEntity findByJobId(@Param("jobId") String jobId);

    @Update("""
            UPDATE pm_project_chat_session
            SET project_id = #{projectId},
                status = #{status},
                assistant_summary = #{assistantSummary},
                business_knowledge_summary = #{businessKnowledgeSummary},
                structured_info_json = #{structuredInfoJson},
                ready_to_create = #{readyToCreate},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int update(ProjectChatSessionEntity entity);
}
