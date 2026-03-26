package com.tongtu.docgen.project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class KnowledgeFileStorageService {
    private static final String LOCAL_BUCKET = "local";

    @Value("${agent.storage.local.baseDir:./data/knowledge-assets}")
    private String localBaseDir;

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
        Path target = Path.of(localBaseDir).resolve(storageKey).normalize();
        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String sha256 = sha256(target);
            return new StoredFile(
                    originalFilename,
                    LOCAL_BUCKET,
                    storageKey.replace('\\', '/'),
                    file.getContentType(),
                    file.getSize(),
                    sha256
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store uploaded file.", ex);
        }
    }

    public Path resolvePath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key is required.");
        }
        return Path.of(localBaseDir).resolve(storageKey).normalize();
    }

    private String normalizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload.bin";
        }
        return originalFilename.replace("\\", "_").replace("/", "_").trim();
    }

    private String sha256(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    if (read > 0) {
                        digest.update(buffer, 0, read);
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available.", ex);
        }
    }
}
