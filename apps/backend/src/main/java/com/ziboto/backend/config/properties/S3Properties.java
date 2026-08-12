package com.ziboto.backend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for AWS S3.
 * 
 * Uses AWS SDK DefaultCredentialsProvider chain for credentials.
 * DO NOT configure access keys here - use AWS credential provider chain.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.storage.s3")
public class S3Properties {
    
    /**
     * S3 bucket name.
     * Default: ziboto-files-277522752099-eu-north-1-an
     */
    private String bucketName = "ziboto-files-277522752099-eu-north-1-an";
    
    /**
     * AWS region.
     * Default: eu-north-1
     */
    private String region = "eu-north-1";
    
    /**
     * Multipart upload threshold (bytes).
     * Files larger than this will use multipart upload.
     * Default: 100MB
     */
    private Long multipartThreshold = 104857600L; // 100MB
    
    /**
     * Multipart part size (bytes).
     * Default: 10MB
     */
    private Long partSize = 10485760L; // 10MB
    
    /**
     * Maximum concurrent multipart uploads.
     * Default: 10
     */
    private Integer maxConcurrency = 10;
    
    /**
     * Presigned URL expiration (minutes).
     * Default: 15 minutes
     */
    private Integer presignedUrlExpirationMinutes = 15;
    
    /**
     * Enable server-side encryption.
     * Default: true
     */
    private Boolean enableEncryption = true;
    
    /**
     * Storage class for uploaded objects.
     * Options: STANDARD, INTELLIGENT_TIERING, STANDARD_IA, ONEZONE_IA, GLACIER, DEEP_ARCHIVE
     * Default: STANDARD
     */
    private String storageClass = "STANDARD";
    
    /**
     * Enable versioning for uploaded objects.
     * Default: false (V2 feature)
     */
    private Boolean enableVersioning = false;
    
    /**
     * S3 object key prefix (folder path in bucket).
     * Default: files/
     */
    private String keyPrefix = "files/";
    
    /**
     * Connection timeout (milliseconds).
     * Default: 10 seconds
     */
    private Integer connectionTimeout = 10000;
    
    /**
     * Read timeout (milliseconds).
     * Default: 60 seconds
     */
    private Integer readTimeout = 60000;
}
