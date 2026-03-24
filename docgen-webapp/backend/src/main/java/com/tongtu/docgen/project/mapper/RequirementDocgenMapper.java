package com.tongtu.docgen.project.mapper;

import com.tongtu.docgen.project.model.entity.RequirementChatSessionEntity;
import org.apache.ibatis.annotations.*;

@Mapper
public interface RequirementDocgenMapper {
    @Insert("""
            INSERT INTO rm_requirement_chat_session(requirement_id, job_id, status, pending_question, confirmed_items_json, unconfirmed_items_json, ready_to_generate, created_by, created_at, updated_at)
            VALUES(#{requirementId}, #{jobId}, #{status}, #{pendingQuestion}, #{confirmedItemsJson}, #{unconfirmedItemsJson}, #{readyToGenerate}, #{createdBy}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RequirementChatSessionEntity entity);

    @Select("""
            SELECT id, requirement_id AS requirementId, job_id AS jobId, status, pending_question AS pendingQuestion,
                   confirmed_items_json AS confirmedItemsJson, unconfirmed_items_json AS unconfirmedItemsJson,
                   ready_to_generate AS readyToGenerate, created_by AS createdBy, created_at AS createdAt, updated_at AS updatedAt
            FROM rm_requirement_chat_session
            WHERE requirement_id = #{requirementId}
            ORDER BY id DESC
            LIMIT 1
            """)
    RequirementChatSessionEntity findLatestByRequirementId(@Param("requirementId") Long requirementId);

    @Select("""
            SELECT id, requirement_id AS requirementId, job_id AS jobId, status, pending_question AS pendingQuestion,
                   confirmed_items_json AS confirmedItemsJson, unconfirmed_items_json AS unconfirmedItemsJson,
                   ready_to_generate AS readyToGenerate, created_by AS createdBy, created_at AS createdAt, updated_at AS updatedAt
            FROM rm_requirement_chat_session
            WHERE requirement_id = #{requirementId} AND job_id = #{jobId}
            LIMIT 1
            """)
    RequirementChatSessionEntity findByRequirementIdAndJobId(@Param("requirementId") Long requirementId, @Param("jobId") String jobId);

    @Update("""
            UPDATE rm_requirement_chat_session
            SET status = #{status},
                pending_question = #{pendingQuestion},
                confirmed_items_json = #{confirmedItemsJson},
                unconfirmed_items_json = #{unconfirmedItemsJson},
                ready_to_generate = #{readyToGenerate},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int update(RequirementChatSessionEntity entity);
}


