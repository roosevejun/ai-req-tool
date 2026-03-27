package com.tongtu.docgen.project.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class KnowledgeFileStorageService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeFileStorageService.class);
    private static final String LOCAL_BUCKET = "local";

    @Value("${agent.storage.provider:local}")
    private String storageProvider;

    @Value("${agent.storage.local.baseDir:./data/knowledge-assets}")
    private String localBaseDir;

    @Value("${agent.storage.minio.endpoint:}")
    private String minioEndpoint;

    @Value("${agent.storage.minio.accessKey:}")
    private String minioAccessKey;

    @Value("${agent.storage.minio.secretKey:}")
    private String minioSecretKey;

    @Value("${agent.storage.minio.bucket:ai-req-tool-knowledge}")
    private String minioBucket;

    public record StoredFile(
            String originalFilename,
            String storageBucket,
            String storageKey,
            String mimeType,
            Long sizeBytes,
            String sha256
    ) {
    }

    public StoredFile storeProjectConversationFile(Long sessionId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is required.");
        }
        String originalFilename = normalizeFilename(file.getOriginalFilename());
        String storageKey = "project-ai/" + sessionId + "/" + UUID.randomUUID() + "-" + originalFilename;
        try {
            byte[] bytes = file.getBytes();
            String sha256 = sha256(bytes);
            if (useMinio()) {
                storeToMinio(storageKey, file.getContentType(), bytes);
                return new StoredFile(
                        originalFilename,
                        minioBucket,
                        storageKey,
                        file.getContentType(),
                        file.getSize(),
                        sha256
                );
            }
            storeToLocal(storageKey, bytes);
            return new StoredFile(
                    originalFilename,
                    LOCAL_BUCKET,
                    storageKey,
                    file.getContentType(),
                    file.getSize(),
                    sha256
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded file.", ex);
        }
    }

    public String readUtf8(String storageBucket, String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key is required.");
        }
        try {
            if (isLocalStorageBucket(storageBucket)) {
                Path path = resolveLocalPath(storageKey);
                if (!Files.exists(path)) {
                    throw new IllegalStateException("Stored file does not exist.");
                }
                return Files.readString(path, StandardCharsets.UTF_8);
            }
            try (InputStream inputStream = buildMinioClient().getObject(
                    GetObjectArgs.builder()
                            .bucket(resolveBucket(storageBucket))
                            .object(storageKey)
                            .build()
            )) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (ErrorResponseException ex) {
            throw new IllegalStateException("Failed to read object from MinIO: " + ex.errorResponse().message(), ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read stored file.", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load stored object.", ex);
        }
    }

    public Path resolvePath(String storageKey) {
        return resolveLocalPath(storageKey);
    }

    private void storeToLocal(String storageKey, byte[] bytes) throws IOException {
        Path target = resolveLocalPath(storageKey);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
    }

    private void storeToMinio(String storageKey, String contentType, byte[] bytes) {
        try {
            MinioClient client = buildMinioClient();
            ensureBucketExists(client, minioBucket);
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                client.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioBucket)
                                .object(storageKey)
                                .stream(inputStream, bytes.length, -1)
                                .contentType(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType)
                                .build()
                );
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to store file to MinIO.", ex);
        }
    }

    private Path resolveLocalPath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key is required.");
        }
        return Path.of(localBaseDir).resolve(storageKey).normalize();
    }

    private boolean useMinio() {
        return "minio".equalsIgnoreCase(storageProvider);
    }

    private boolean isLocalStorageBucket(String storageBucket) {
        if (storageBucket == null || storageBucket.isBlank()) {
            return !useMinio();
        }
        return LOCAL_BUCKET.equalsIgnoreCase(storageBucket.trim());
    }

    private String resolveBucket(String storageBucket) {
        if (storageBucket == null || storageBucket.isBlank() || LOCAL_BUCKET.equalsIgnoreCase(storageBucket.trim())) {
            return minioBucket;
        }
        return storageBucket.trim();
    }

    private void ensureBucketExists(MinioClient client, String bucket) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Created MinIO bucket {}", bucket);
        }
    }

    private MinioClient buildMinioClient() {
        if (minioEndpoint == null || minioEndpoint.isBlank()) {
            throw new IllegalStateException("MinIO endpoint is not configured.");
        }
        if (minioAccessKey == null || minioAccessKey.isBlank() || minioSecretKey == null || minioSecretKey.isBlank()) {
            throw new IllegalStateException("MinIO access key or secret key is not configured.");
        }
        return MinioClient.builder()
                .endpoint(minioEndpoint.trim())
                .credentials(minioAccessKey.trim(), minioSecretKey.trim())
                .build();
    }

    private String normalizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload.bin";
        }
        return originalFilename.replace("\\", "_").replace("/", "_").trim();
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available.", ex);
        }
    }
}
