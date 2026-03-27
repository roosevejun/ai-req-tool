package com.tongtu.docgen.project.mapper;

import com.tongtu.docgen.project.model.entity.KnowledgeDocumentEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {
    @Insert("""
            INSERT INTO km_document(project_id, requirement_id, source_material_id, document_type, source_uri, title,
                                    status, raw_text, clean_text, summary, keywords_json, tags, content_hash,
                                    version_no, is_latest, retrievable, created_by, updated_by, created_at, updated_at)
            VALUES(#{projectId}, #{requirementId}, #{sourceMaterialId}, #{documentType}, #{sourceUri}, #{title},
                   #{status}, #{rawText}, #{cleanText}, #{summary}, #{keywordsJson}, #{tags}, #{contentHash},
                   #{versionNo}, #{isLatest}, #{retrievable}, #{createdBy}, #{updatedBy}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeDocumentEntity entity);

    @Select("""
            SELECT id, project_id AS projectId, requirement_id AS requirementId,
                   source_material_id AS sourceMaterialId, document_type AS documentType,
                   source_uri AS sourceUri, title, status, raw_text AS rawText, clean_text AS cleanText,
                   summary, keywords_json AS keywordsJson, tags, content_hash AS contentHash,
                   version_no AS versionNo, is_latest AS isLatest, retrievable,
                   created_by AS createdBy, updated_by AS updatedBy,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM km_document
            WHERE id = #{id}
            LIMIT 1
            """)
    KnowledgeDocumentEntity findById(@Param("id") Long id);

    @Select("""
            SELECT id, project_id AS projectId, requirement_id AS requirementId,
                   source_material_id AS sourceMaterialId, document_type AS documentType,
                   source_uri AS sourceUri, title, status, raw_text AS rawText, clean_text AS cleanText,
                   summary, keywords_json AS keywordsJson, tags, content_hash AS contentHash,
                   version_no AS versionNo, is_latest AS isLatest, retrievable,
                   created_by AS createdBy, updated_by AS updatedBy,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM km_document
            WHERE project_id = #{projectId}
            ORDER BY id DESC
            """)
    List<KnowledgeDocumentEntity> listByProjectId(@Param("projectId") Long projectId);

    @Select("""
            SELECT id, project_id AS projectId, requirement_id AS requirementId,
                   source_material_id AS sourceMaterialId, document_type AS documentType,
                   source_uri AS sourceUri, title, status, raw_text AS rawText, clean_text AS cleanText,
                   summary, keywords_json AS keywordsJson, tags, content_hash AS contentHash,
                   version_no AS versionNo, is_latest AS isLatest, retrievable,
                   created_by AS createdBy, updated_by AS updatedBy,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM km_document
            WHERE requirement_id = #{requirementId}
            ORDER BY id DESC
            """)
    List<KnowledgeDocumentEntity> listByRequirementId(@Param("requirementId") Long requirementId);

    @Select("""
            SELECT id, project_id AS projectId, requirement_id AS requirementId,
                   source_material_id AS sourceMaterialId, document_type AS documentType,
                   source_uri AS sourceUri, title, status, raw_text AS rawText, clean_text AS cleanText,
                   summary, keywords_json AS keywordsJson, tags, content_hash AS contentHash,
                   version_no AS versionNo, is_latest AS isLatest, retrievable,
                   created_by AS createdBy, updated_by AS updatedBy,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM km_document
            WHERE source_material_id = #{sourceMaterialId}
            ORDER BY id DESC
            """)
    List<KnowledgeDocumentEntity> listBySourceMaterialId(@Param("sourceMaterialId") Long sourceMaterialId);

    @Update("""
            UPDATE km_document
            SET project_id = #{projectId},
                requirement_id = #{requirementId},
                source_material_id = #{sourceMaterialId},
                document_type = #{documentType},
                source_uri = #{sourceUri},
                title = #{title},
                status = #{status},
                raw_text = #{rawText},
                clean_text = #{cleanText},
                summary = #{summary},
                keywords_json = #{keywordsJson},
                tags = #{tags},
                content_hash = #{contentHash},
                version_no = #{versionNo},
                is_latest = #{isLatest},
                retrievable = #{retrievable},
                updated_by = #{updatedBy},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int update(KnowledgeDocumentEntity entity);

    @Delete("""
            DELETE FROM km_document
            WHERE source_material_id = #{sourceMaterialId}
            """)
    int deleteBySourceMaterialId(@Param("sourceMaterialId") Long sourceMaterialId);

    @Update("""
            UPDATE km_document
            SET project_id = #{projectId},
                updated_at = NOW()
            WHERE source_material_id IN (
                SELECT id FROM pm_project_source_material WHERE session_id = #{sessionId}
            )
            """)
    int bindProjectIdBySessionId(@Param("sessionId") Long sessionId, @Param("projectId") Long projectId);
}
