package com.ziboto.backend.file.service;

import com.ziboto.backend.config.properties.S3Properties;
import com.ziboto.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private MultipartFile multipartFile;

    private S3Properties s3Properties;
    private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        s3Properties = new S3Properties();
        s3Properties.setBucketName("test-bucket");
        s3Properties.setRegion("us-east-1");
        s3Properties.setKeyPrefix("files/");
        s3Properties.setEnableEncryption(true);
        s3Properties.setStorageClass("STANDARD");

        s3StorageService = new S3StorageService(s3Properties);
        // Inject mocked S3Client using reflection
        try {
            java.lang.reflect.Field field = S3StorageService.class.getDeclaredField("s3Client");
            field.setAccessible(true);
            field.set(s3StorageService, s3Client);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocked S3Client", e);
        }
    }

    @Test
    void testUploadFile_Success() throws IOException {
        // Arrange
        Long userId = 1L;
        UUID fileId = UUID.randomUUID();
        String originalFilename = "test-file.txt";
        long fileSize = 1024L;
        String contentType = "text/plain";
        byte[] fileContent = "test content".getBytes();

        when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
        when(multipartFile.getSize()).thenReturn(fileSize);
        when(multipartFile.getContentType()).thenReturn(contentType);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Act
        String storageKey = s3StorageService.uploadFile(userId, fileId, multipartFile);

        // Assert
        assertThat(storageKey).isNotNull();
        assertThat(storageKey).startsWith("files/user-1/");
        assertThat(storageKey).contains(fileId.toString());
        assertThat(storageKey).endsWith("test-file.txt");

        // Verify S3 client was called
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo("test-bucket");
        assertThat(capturedRequest.key()).isEqualTo(storageKey);
        assertThat(capturedRequest.contentType()).isEqualTo(contentType);
        assertThat(capturedRequest.contentLength()).isEqualTo(fileSize);
        assertThat(capturedRequest.serverSideEncryption()).isEqualTo(ServerSideEncryption.AES256);
        assertThat(capturedRequest.storageClass()).isEqualTo(StorageClass.STANDARD);

        // Verify metadata
        assertThat(capturedRequest.metadata()).containsEntry("user-id", "1");
        assertThat(capturedRequest.metadata()).containsEntry("file-id", fileId.toString());
        assertThat(capturedRequest.metadata()).containsEntry("original-filename", originalFilename);
    }

    @Test
    void testUploadFile_SanitizesFilename() throws IOException {
        // Arrange
        Long userId = 1L;
        UUID fileId = UUID.randomUUID();
        String unsafeFilename = "../../../etc/passwd";
        long fileSize = 1024L;

        when(multipartFile.getOriginalFilename()).thenReturn(unsafeFilename);
        when(multipartFile.getSize()).thenReturn(fileSize);
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Act
        String storageKey = s3StorageService.uploadFile(userId, fileId, multipartFile);

        // Assert
        assertThat(storageKey).doesNotContain("../");
        assertThat(storageKey).doesNotContain("/etc/");
        assertThat(storageKey).matches("^files/user-\\d+/[a-f0-9-]+/[a-zA-Z0-9._-]+$");
    }

    @Test
    void testUploadFile_HandlesIOException() throws IOException {
        // Arrange
        Long userId = 1L;
        UUID fileId = UUID.randomUUID();

        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getInputStream()).thenThrow(new IOException("Simulated IO error"));

        // Act & Assert
        assertThatThrownBy(() -> s3StorageService.uploadFile(userId, fileId, multipartFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to read file for upload");
    }

    @Test
    void testUploadFile_HandlesS3Exception() throws IOException {
        // Arrange
        Long userId = 1L;
        UUID fileId = UUID.randomUUID();

        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder()
                        .message("Access Denied")
                        .statusCode(403)
                        .build());

        // Act & Assert
        assertThatThrownBy(() -> s3StorageService.uploadFile(userId, fileId, multipartFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("S3 upload failed");
    }

    @Test
    void testGetFileStream_Success() {
        // Arrange
        String storageKey = "files/user-1/test-uuid/test-file.txt";
        byte[] fileContent = "test content".getBytes();
        InputStream mockInputStream = new ByteArrayInputStream(fileContent);

        when(s3Client.getObject(any(GetObjectRequest.class), any(software.amazon.awssdk.core.sync.ResponseTransformer.class)))
                .thenReturn(mockInputStream);

        // Act
        InputStream result = s3StorageService.getFileStream(storageKey);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockInputStream);

        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(requestCaptor.capture(), any(software.amazon.awssdk.core.sync.ResponseTransformer.class));

        GetObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo("test-bucket");
        assertThat(capturedRequest.key()).isEqualTo(storageKey);
    }

    @Test
    void testGetFileStream_FileNotFound() {
        // Arrange
        String storageKey = "files/user-1/test-uuid/nonexistent-file.txt";

        when(s3Client.getObject(any(GetObjectRequest.class), any(software.amazon.awssdk.core.sync.ResponseTransformer.class)))
                .thenThrow(NoSuchKeyException.builder()
                        .message("The specified key does not exist")
                        .build());

        // Act & Assert
        assertThatThrownBy(() -> s3StorageService.getFileStream(storageKey))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File not found in S3");
    }

    @Test
    void testDeleteFile_Success() {
        // Arrange
        String storageKey = "files/user-1/test-uuid/test-file.txt";

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        // Act
        s3StorageService.deleteFile(storageKey);

        // Assert
        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo("test-bucket");
        assertThat(capturedRequest.key()).isEqualTo(storageKey);
    }

    @Test
    void testDeleteFile_HandlesS3Exception() {
        // Arrange
        String storageKey = "files/user-1/test-uuid/test-file.txt";

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .message("Access Denied")
                        .statusCode(403)
                        .build());

        // Act & Assert
        assertThatThrownBy(() -> s3StorageService.deleteFile(storageKey))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("S3 deletion failed");
    }

    @Test
    void testFileExists_True() {
        // Arrange
        String storageKey = "files/user-1/test-uuid/test-file.txt";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        // Act
        boolean exists = s3StorageService.fileExists(storageKey);

        // Assert
        assertThat(exists).isTrue();

        ArgumentCaptor<HeadObjectRequest> requestCaptor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(s3Client).headObject(requestCaptor.capture());

        HeadObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo("test-bucket");
        assertThat(capturedRequest.key()).isEqualTo(storageKey);
    }

    @Test
    void testFileExists_False() {
        // Arrange
        String storageKey = "files/user-1/test-uuid/nonexistent-file.txt";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        // Act
        boolean exists = s3StorageService.fileExists(storageKey);

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void testFileExists_HandlesS3Exception() {
        // Arrange
        String storageKey = "files/user-1/test-uuid/test-file.txt";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .message("Access Denied")
                        .statusCode(403)
                        .build());

        // Act
        boolean exists = s3StorageService.fileExists(storageKey);

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void testUploadFile_WithEncryptionDisabled() throws IOException {
        // Arrange
        s3Properties.setEnableEncryption(false);
        s3StorageService = new S3StorageService(s3Properties);
        // Inject mocked S3Client
        try {
            java.lang.reflect.Field field = S3StorageService.class.getDeclaredField("s3Client");
            field.setAccessible(true);
            field.set(s3StorageService, s3Client);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Long userId = 1L;
        UUID fileId = UUID.randomUUID();

        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Act
        s3StorageService.uploadFile(userId, fileId, multipartFile);

        // Assert
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.serverSideEncryption()).isNull();
    }

    @Test
    void testUploadFile_TruncatesLongFilename() throws IOException {
        // Arrange
        Long userId = 1L;
        UUID fileId = UUID.randomUUID();
        String longFilename = "a".repeat(150) + ".txt";

        when(multipartFile.getOriginalFilename()).thenReturn(longFilename);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Act
        String storageKey = s3StorageService.uploadFile(userId, fileId, multipartFile);

        // Assert
        String filename = storageKey.substring(storageKey.lastIndexOf('/') + 1);
        assertThat(filename.length()).isLessThanOrEqualTo(100);
        assertThat(filename).endsWith(".txt"); // Extension preserved
    }
}
