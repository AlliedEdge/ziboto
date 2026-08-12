package com.ziboto.backend.file.service;

import com.ziboto.backend.config.properties.S3Properties;
import com.ziboto.backend.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AWS S3 Storage Service Implementation.
 * Uses DefaultCredentialsProvider - NO hard-coded credentials.
 * Bucket: ziboto-files-277522752099-eu-north-1-an
 * Region: eu-north-1
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {
    
    private final S3Properties s3Properties;
    private S3Client s3Client;
    
    @PostConstruct
    public void init() {
        log.info("Initializing S3 storage service");
        log.info("S3 Bucket: {}", s3Properties.getBucketName());
        log.info("S3 Region: {}", s3Properties.getRegion());
        
        try {
            this.s3Client = S3Client.builder()
                    .region(Region.of(s3Properties.getRegion()))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
            
            verifyBucketAccess();
            log.info("S3 storage service initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize S3 storage service", e);
            throw new RuntimeException("S3 initialization failed", e);
        }
    }
    
    private void verifyBucketAccess() {
        try {
            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .build();
            s3Client.headBucket(request);
            log.info("Verified S3 bucket access: {}", s3Properties.getBucketName());
        } catch (NoSuchBucketException e) {
            throw new RuntimeException("S3 bucket not found: " + s3Properties.getBucketName(), e);
        } catch (S3Exception e) {
            throw new RuntimeException("S3 bucket access denied. Check IAM permissions.", e);
        }
    }
    
    @Override
    public String uploadFile(Long userId, UUID fileId, MultipartFile file) {
        String fileName = file.getOriginalFilename();
        long fileSize = file.getSize();
        String contentType = file.getContentType();
        
        log.info("Uploading to S3: user={}, file={}, size={}", userId, fileName, fileSize);
        
        String s3Key = generateS3Key(userId, fileId, fileName);
        
        try (InputStream inputStream = file.getInputStream()) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("user-id", String.valueOf(userId));
            metadata.put("file-id", fileId.toString());
            metadata.put("original-filename", fileName);
            
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(s3Key)
                    .contentType(contentType)
                    .contentLength(fileSize)
                    .metadata(metadata)
                    .serverSideEncryption(s3Properties.getEnableEncryption() ? 
                            ServerSideEncryption.AES256 : null)
                    .storageClass(StorageClass.fromValue(s3Properties.getStorageClass()))
                    .build();
            
            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, fileSize));
            
            log.info("Successfully uploaded to S3: key={}", s3Key);
            return s3Key;
            
        } catch (S3Exception e) {
            String errorMessage = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            log.error("S3 upload failed: key={}, error={}", s3Key, errorMessage);
            throw new RuntimeException("S3 upload failed: " + errorMessage, e);
        } catch (IOException e) {
            log.error("IO error during S3 upload: key={}", s3Key, e);
            throw new RuntimeException("Failed to read file for upload", e);
        }
    }
    
    @Override
    public InputStream getFileStream(String storageKey) {
        log.info("Downloading from S3: key={}", storageKey);
        
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(storageKey)
                    .build();
            
            return s3Client.getObject(getRequest, ResponseTransformer.toInputStream());
            
        } catch (NoSuchKeyException e) {
            log.error("File not found in S3: key={}", storageKey);
            throw new ResourceNotFoundException("File not found in S3: " + storageKey);
        } catch (S3Exception e) {
            String errorMessage = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            log.error("S3 download failed: key={}, error={}", storageKey, errorMessage);
            throw new RuntimeException("S3 download failed: " + errorMessage, e);
        }
    }
    
    @Override
    public void deleteFile(String storageKey) {
        log.info("Deleting from S3: key={}", storageKey);
        
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(storageKey)
                    .build();
            
            s3Client.deleteObject(deleteRequest);
            log.info("Successfully deleted from S3: key={}", storageKey);
            
        } catch (S3Exception e) {
            String errorMessage = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            log.error("S3 deletion failed: key={}, error={}", storageKey, errorMessage);
            throw new RuntimeException("S3 deletion failed: " + errorMessage, e);
        }
    }
    
    @Override
    public boolean fileExists(String storageKey) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(storageKey)
                    .build();
            
            s3Client.headObject(headRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("Error checking file existence: key={}", storageKey, e);
            return false;
        }
    }
    
    private String generateS3Key(Long userId, UUID fileId, String originalFileName) {
        String sanitizedFileName = sanitizeFileName(originalFileName);
        return String.format("%suser-%d/%s/%s",
                s3Properties.getKeyPrefix(),
                userId,
                fileId.toString(),
                sanitizedFileName);
    }
    
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unnamed-file";
        }
        
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        
        if (sanitized.length() > 100) {
            String extension = "";
            int dotIndex = sanitized.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = sanitized.substring(dotIndex);
                sanitized = sanitized.substring(0, 
                    Math.min(100 - extension.length(), sanitized.length())) + extension;
            } else {
                sanitized = sanitized.substring(0, 100);
            }
        }
        
        return sanitized;
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("Shutting down S3 storage service");
        if (s3Client != null) {
            s3Client.close();
        }
        log.info("S3 storage service shutdown complete");
    }
}
