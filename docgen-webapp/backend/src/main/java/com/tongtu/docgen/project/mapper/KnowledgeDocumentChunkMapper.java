package com.tongtu.docgen.project.mapper;

import com.tongtu.docgen.project.model.entity.KnowledgeDocumentChunkEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeDocumentChunkMapper {
    @Insert("""
            INSERT INTO km_document_chunk(document_id, chunk_no, chunk_text, token_count, summary, embedding_status, vector_ref,
                                          embedding_model, embedding_vector, embedded_at, created_at)
            VALUES(#{documentId}, #{chunkNo}, #{chunkText}, #{tokenCount}, #{summary}, #{embeddingStatus}, #{vectorRef},
                   #{embeddingModel},
                   CASE WHEN #{embeddingVectorLiteral} IS NULL OR #{embeddingVectorLiteral} = '' THEN NULL
                        ELSE CAST(#{embeddingVectorLiteral} AS vector) END,
                   #{embeddedAt},
                   NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeDocumentChunkEntity entity);

    @Select("""
            SELECT id, document_id AS documentId, chunk_no AS chunkNo, chunk_text AS chunkText,
                   token_count AS tokenCount, summary, embedding_status AS embeddingStatus,
                   vector_ref AS vectorRef, embedding_model AS embeddingModel,
                   embedding_vector::text AS embeddingVectorLiteral,
                   embedded_at AS embeddedAt, created_at AS createdAt
            FROM km_document_chunk
            WHERE document_id = #{documentId}
            ORDER BY chunk_no ASC
            """)
    List<KnowledgeDocumentChunkEntity> listByDocumentId(@Param("documentId") Long documentId);

    @Select("""
            SELECT c.id, c.document_id AS documentId, c.chunk_no AS chunkNo, c.chunk_text AS chunkText,
                   c.token_count AS tokenCount, c.summary, c.embedding_status AS embeddingStatus,
                   c.vector_ref AS vectorRef, c.embedding_model AS embeddingModel,
                   c.embedding_vector::text AS embeddingVectorLiteral,
                   c.embedded_at AS embeddedAt, c.created_at AS createdAt
            FROM km_document_chunk c
            JOIN km_document d ON d.id = c.document_id
            WHERE c.embedding_status = 'PENDING'
              AND d.retrievable = TRUE
              AND d.status = 'READY'
            ORDER BY c.id ASC
            LIMIT #{limit}
            """)
    List<KnowledgeDocumentChunkEntity> listPendingEmbeddingChunks(@Param("limit") int limit);

    @Select("""
            SELECT id, document_id AS documentId, chunk_no AS chunkNo, chunk_text AS chunkText,
                   token_count AS tokenCount, summary, embedding_status AS embeddingStatus,
                   vector_ref AS vectorRef, embedding_model AS embeddingModel,
                   embedding_vector::text AS embeddingVectorLiteral,
                   embedded_at AS embeddedAt, created_at AS createdAt
            FROM km_document_chunk
            WHERE id = #{id}
            LIMIT 1
            """)
    KnowledgeDocumentChunkEntity findById(@Param("id") Long id);

    @Update("""
            UPDATE km_document_chunk
            SET embedding_status = #{embeddingStatus},
                vector_ref = #{vectorRef},
                embedding_model = #{embeddingModel},
                embedding_vector = CASE WHEN #{embeddingVectorLiteral} IS NULL OR #{embeddingVectorLiteral} = '' THEN NULL
                                        ELSE CAST(#{embeddingVectorLiteral} AS vector) END,
                embedded_at = #{embeddedAt}
            WHERE id = #{id}
            """)
    int updateEmbedding(KnowledgeDocumentChunkEntity entity);

    @Select("""
            SELECT c.id, c.document_id AS documentId, c.chunk_no AS chunkNo, c.chunk_text AS chunkText,
                   c.token_count AS tokenCount, c.summary, c.embedding_status AS embeddingStatus,
                   c.vector_ref AS vectorRef, c.embedding_model AS embeddingModel,
                   c.embedding_vector::text AS embeddingVectorLiteral,
                   c.embedded_at AS embeddedAt, c.created_at AS createdAt,
                   1 - (c.embedding_vector <=> CAST(#{queryVectorLiteral} AS vector)) AS score
            FROM km_document_chunk c
            JOIN km_document d ON d.id = c.document_id
            WHERE d.project_id = #{projectId}
              AND d.retrievable = TRUE
              AND d.status = 'READY'
              AND c.embedding_status = 'READY'
              AND c.embedding_vector IS NOT NULL
            ORDER BY c.embedding_vector <=> CAST(#{queryVectorLiteral} AS vector)
            LIMIT #{topK}
            """)
    List<KnowledgeDocumentChunkEntity> searchProjectChunks(@Param("projectId") Long projectId,
                                                           @Param("queryVectorLiteral") String queryVectorLiteral,
                                                           @Param("topK") int topK);

    @Select("""
            SELECT c.id, c.document_id AS documentId, c.chunk_no AS chunkNo, c.chunk_text AS chunkText,
                   c.token_count AS tokenCount, c.summary, c.embedding_status AS embeddingStatus,
                   c.vector_ref AS vectorRef, c.embedding_model AS embeddingModel,
                   c.embedding_vector::text AS embeddingVectorLiteral,
                   c.embedded_at AS embeddedAt, c.created_at AS createdAt,
                   1 - (c.embedding_vector <=> CAST(#{queryVectorLiteral} AS vector)) AS score
            FROM km_document_chunk c
            JOIN km_document d ON d.id = c.document_id
            WHERE d.requirement_id = #{requirementId}
              AND d.retrievable = TRUE
              AND d.status = 'READY'
              AND c.embedding_status = 'READY'
              AND c.embedding_vector IS NOT NULL
            ORDER BY c.embedding_vector <=> CAST(#{queryVectorLiteral} AS vector)
            LIMIT #{topK}
            """)
    List<KnowledgeDocumentChunkEntity> searchRequirementChunks(@Param("requirementId") Long requirementId,
                                                               @Param("queryVectorLiteral") String queryVectorLiteral,
                                                               @Param("topK") int topK);

    @Delete("""
            DELETE FROM km_document_chunk
            WHERE document_id = #{documentId}
            """)
    int deleteByDocumentId(@Param("documentId") Long documentId);
}
