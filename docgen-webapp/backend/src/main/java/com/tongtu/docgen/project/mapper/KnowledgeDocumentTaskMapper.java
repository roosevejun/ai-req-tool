package com.tongtu.docgen.project.mapper;

import com.tongtu.docgen.project.model.entity.KnowledgeDocumentTaskEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeDocumentTaskMapper {
    @Insert("""
            INSERT INTO km_document_task(document_id, task_type, status, attempt_count, last_error, started_at, finished_at, created_at)
            VALUES(#{documentId}, #{taskType}, #{status}, #{attemptCount}, #{lastError}, #{startedAt}, #{finishedAt}, NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeDocumentTaskEntity entity);

    @Select("""
            SELECT id, document_id AS documentId, task_type AS taskType, status,
                   attempt_count AS attemptCount, last_error AS lastError,
                   started_at AS startedAt, finished_at AS finishedAt, created_at AS createdAt
            FROM km_document_task
            WHERE document_id = #{documentId}
            ORDER BY id ASC
            """)
    List<KnowledgeDocumentTaskEntity> listByDocumentId(@Param("documentId") Long documentId);

    @Select("""
            SELECT id, document_id AS documentId, task_type AS taskType, status,
                   attempt_count AS attemptCount, last_error AS lastError,
                   started_at AS startedAt, finished_at AS finishedAt, created_at AS createdAt
            FROM km_document_task
            WHERE id = #{id}
            LIMIT 1
            """)
    KnowledgeDocumentTaskEntity findById(@Param("id") Long id);

    @Select("""
            SELECT id, document_id AS documentId, task_type AS taskType, status,
                   attempt_count AS attemptCount, last_error AS lastError,
                   started_at AS startedAt, finished_at AS finishedAt, created_at AS createdAt
            FROM km_document_task
            WHERE status = 'PENDING'
            ORDER BY id ASC
            LIMIT #{limit}
            """)
    List<KnowledgeDocumentTaskEntity> listPendingTasks(@Param("limit") int limit);

    @Update("""
            UPDATE km_document_task
            SET status = #{status},
                attempt_count = #{attemptCount},
                last_error = #{lastError},
                started_at = #{startedAt},
                finished_at = #{finishedAt}
            WHERE id = #{id}
            """)
    int updateStatus(KnowledgeDocumentTaskEntity entity);
}
