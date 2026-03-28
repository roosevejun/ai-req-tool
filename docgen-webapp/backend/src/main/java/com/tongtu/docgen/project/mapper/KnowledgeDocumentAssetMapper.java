package com.tongtu.docgen.project.mapper;

import com.tongtu.docgen.project.model.entity.KnowledgeDocumentAssetEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeDocumentAssetMapper {
    @Insert("""
            INSERT INTO km_document_asset(document_id, asset_role, storage_bucket, storage_key, mime_type, size_bytes, sha256, created_at)
            VALUES(#{documentId}, #{assetRole}, #{storageBucket}, #{storageKey}, #{mimeType}, #{sizeBytes}, #{sha256}, NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeDocumentAssetEntity entity);

    @Select("""
            SELECT id, document_id AS documentId, asset_role AS assetRole,
                   storage_bucket AS storageBucket, storage_key AS storageKey,
                   mime_type AS mimeType, size_bytes AS sizeBytes, sha256, created_at AS createdAt
            FROM km_document_asset
            WHERE id = #{id}
            """)
    KnowledgeDocumentAssetEntity findById(@Param("id") Long id);

    @Select("""
            SELECT id, document_id AS documentId, asset_role AS assetRole,
                   storage_bucket AS storageBucket, storage_key AS storageKey,
                   mime_type AS mimeType, size_bytes AS sizeBytes, sha256, created_at AS createdAt
            FROM km_document_asset
            WHERE document_id = #{documentId}
            ORDER BY id ASC
            """)
    List<KnowledgeDocumentAssetEntity> listByDocumentId(@Param("documentId") Long documentId);
}
