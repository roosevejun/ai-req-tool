package com.tongtu.docgen.project.mapper;

import com.tongtu.docgen.project.model.entity.KnowledgeDocumentChunkEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeDocumentChunkMapper {
    @Insert("""
            INSERT INTO km_document_chunk(document_id, chunk_no, chunk_text, token_count, summary, embedding_status, vector_ref, created_at)
            VALUES(#{documentId}, #{chunkNo}, #{chunkText}, #{tokenCount}, #{summary}, #{embeddingStatus}, #{vectorRef}, NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeDocumentChunkEntity entity);

    @Select("""
            SELECT id, document_id AS documentId, chunk_no AS chunkNo, chunk_text AS chunkText,
                   token_count AS tokenCount, summary, embedding_status AS embeddingStatus,
                   vector_ref AS vectorRef, created_at AS createdAt
            FROM km_document_chunk
            WHERE document_id = #{documentId}
            ORDER BY chunk_no ASC
            """)
    List<KnowledgeDocumentChunkEntity> listByDocumentId(@Param("documentId") Long documentId);

    @Delete("""
            DELETE FROM km_document_chunk
            WHERE document_id = #{documentId}
            """)
    int deleteByDocumentId(@Param("documentId") Long documentId);
}
