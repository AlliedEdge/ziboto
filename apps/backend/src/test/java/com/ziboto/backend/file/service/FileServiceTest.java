package com.ziboto.backend.file.service;

import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.file.dto.FileUploadResponse;
import com.ziboto.backend.file.entity.FileMetadata;
import com.ziboto.backend.file.entity.Folder;
import com.ziboto.backend.file.repository.FileMetadataRepository;
import com.ziboto.backend.file.repository.FolderRepository;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import com.ziboto.backend.user.service.StorageUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private StorageUsageService storageUsageService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileService fileService;

    private User testUser;
    private UUID testFolderId;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setStorageUsed(0L);
        testUser.setStorageQuota(5368709120L); // 5GB

        testFolderId = UUID.randomUUID();

        ReflectionTestUtils.setField(fileService, "maxFileSize", 524288000L); // 500MB
        ReflectionTestUtils.setField(fileService, "allowedMimeTypes", "");
    }

    @Test
    void testUploadFile_Success() throws IOException {
        // Arrange
        Long userId = 1L;
        String username = "testuser";
        String originalFilename = "test-file.txt";
        long fileSize = 1024L;
        String contentType = "text/plain";
        UUID fileId = UUID.randomUUID();
        String storageKey = "files/user-1/" + fileId + "/test-file.txt";

        when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
        when(multipartFile.getSize()).thenReturn(fileSize);
        when(multipartFile.getContentType()).thenReturn(contentType);
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("test content".getBytes()))
                .thenReturn(new ByteArrayInputStream("test content".getBytes()));

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(fileMetadataRepository.existsByUserIdAndFolderIdAndFileName(userId, null, originalFilename))
                .thenReturn(false);
        when(storageService.uploadFile(eq(userId), any(UUID.class), eq(multipartFile)))
                .thenReturn(storageKey);

        FileMetadata savedFile = FileMetadata.builder()
                .id(fileId)
                .userId(userId)
                .fileName(originalFilename)
                .fileSize(fileSize)
                .mimeType(contentType)
                .storageKey(storageKey)
                .build();

        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(savedFile);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        FileUploadResponse response = fileService.uploadFile(multipartFile, userId, null, username);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getFileId()).isEqualTo(fileId);
        assertThat(response.getFileName()).isEqualTo(originalFilename);
        assertThat(response.getFileSize()).isEqualTo(fileSize);
        assertThat(response.getStorageKey()).isEqualTo(storageKey);
        assertThat(response.getIsDuplicate()).isFalse();

        verify(fileMetadataRepository).save(any(FileMetadata.class));
        verify(userRepository).save(any(User.class));
        verify(storageUsageService).invalidateCache(userId);
    }

    @Test
    void testUploadFile_ExceedsQuota() throws IOException {
        // Arrange
        Long userId = 1L;
        String username = "testuser";
        long fileSize = 50000001L; // 50MB + 1 byte - will exceed 5GB quota
        
        // User already has 4.96GB used, adding 50MB+1 will exceed 5GB quota
        testUser.setStorageUsed(5318709120L); // 4.96GB used
        testUser.setStorageQuota(5368709120L); // 5GB quota
        // Total: 5318709120 + 50000001 = 5368709121 which is > 5368709120

        // Mock what's needed for validation before quota check
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(fileSize);
        // getContentType() is NOT needed - MIME validation is skipped when allowedMimeTypes is empty
        when(multipartFile.getOriginalFilename()).thenReturn("large-file.bin");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(multipartFile, userId, null, username))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("Storage quota exceeded");

        verify(fileMetadataRepository, never()).save(any());
        verify(storageService, never()).uploadFile(any(), any(), any());
    }

    @Test
    void testUploadFile_EmptyFile() throws IOException {
        // Arrange
        Long userId = 1L;
        String username = "testuser";

        when(multipartFile.isEmpty()).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(multipartFile, userId, null, username))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("File is empty");

        verify(fileMetadataRepository, never()).save(any());
    }

    @Test
    void testUploadFile_FileTooLarge() throws IOException {
        // Arrange
        Long userId = 1L;
        String username = "testuser";
        long fileSize = 150000000L; // 150MB - exceeds 100MB single upload limit

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(fileSize);

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(multipartFile, userId, null, username))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("File too large");

        verify(fileMetadataRepository, never()).save(any());
    }

    @Test
    void testUploadFile_InvalidMimeType() throws IOException {
        // Arrange
        ReflectionTestUtils.setField(fileService, "allowedMimeTypes", "image/jpeg,image/png");

        Long userId = 1L;
        String username = "testuser";

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("application/pdf");

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(multipartFile, userId, null, username))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("File type not allowed");

        verify(fileMetadataRepository, never()).save(any());
    }

    @Test
    void testUploadFile_DuplicateFilename() throws IOException {
        // Arrange
        Long userId = 1L;
        String username = "testuser";
        String originalFilename = "test-file.txt";
        long fileSize = 1024L;
        UUID fileId = UUID.randomUUID();

        when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
        when(multipartFile.getSize()).thenReturn(fileSize);
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("test content".getBytes()))
                .thenReturn(new ByteArrayInputStream("test content".getBytes()));

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(fileMetadataRepository.existsByUserIdAndFolderIdAndFileName(userId, null, originalFilename))
                .thenReturn(true); // Duplicate
        when(fileMetadataRepository.existsByUserIdAndFolderIdAndFileName(userId, null, "test-file (1).txt"))
                .thenReturn(false); // Unique

        when(storageService.uploadFile(eq(userId), any(UUID.class), eq(multipartFile)))
                .thenReturn("storage-key");

        FileMetadata savedFile = FileMetadata.builder()
                .id(fileId)
                .fileName("test-file (1).txt") // Auto-renamed
                .fileSize(fileSize)
                .storageKey("storage-key")
                .build();

        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(savedFile);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        FileUploadResponse response = fileService.uploadFile(multipartFile, userId, null, username);

        // Assert
        assertThat(response.getFileName()).isEqualTo("test-file (1).txt");
    }

    @Test
    void testUploadFile_WithFolderId() throws IOException {
        // Arrange
        Long userId = 1L;
        String username = "testuser";
        UUID folderId = testFolderId;

        Folder folder = new Folder();
        folder.setId(folderId);
        folder.setUserId(userId);
        folder.setFolderName("Test Folder");

        when(multipartFile.getOriginalFilename()).thenReturn("test-file.txt");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("test content".getBytes()))
                .thenReturn(new ByteArrayInputStream("test content".getBytes()));

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(folderRepository.findByIdAndUserId(folderId, userId)).thenReturn(Optional.of(folder));
        when(fileMetadataRepository.existsByUserIdAndFolderIdAndFileName(userId, folderId, "test-file.txt"))
                .thenReturn(false);
        when(storageService.uploadFile(eq(userId), any(UUID.class), eq(multipartFile)))
                .thenReturn("storage-key");

        FileMetadata savedFile = FileMetadata.builder()
                .id(UUID.randomUUID())
                .folderId(folderId)
                .fileName("test-file.txt")
                .fileSize(1024L)
                .storageKey("storage-key")
                .build();

        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(savedFile);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        FileUploadResponse response = fileService.uploadFile(multipartFile, userId, folderId, username);

        // Assert
        assertThat(response.getFolderId()).isEqualTo(folderId);
        verify(folderRepository).findByIdAndUserId(folderId, userId);
    }

    @Test
    void testUploadFile_FolderNotFound() throws IOException {
        // Arrange
        Long userId = 1L;
        String username = "testuser";
        UUID folderId = UUID.randomUUID();

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getOriginalFilename()).thenReturn("test-file.txt");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(folderRepository.findByIdAndUserId(folderId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(multipartFile, userId, folderId, username))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("don't have permission");

        verify(storageService, never()).uploadFile(any(), any(), any());
    }

    @Test
    void testDeleteFile_Success() {
        // Arrange
        Long userId = 1L;
        UUID fileId = UUID.randomUUID();
        String storageKey = "files/user-1/uuid/test-file.txt";
        long fileSize = 1024L;

        FileMetadata fileMetadata = FileMetadata.builder()
                .id(fileId)
                .userId(userId)
                .fileName("test-file.txt")
                .fileSize(fileSize)
                .storageKey(storageKey)
                .build();

        when(fileMetadataRepository.findByIdAndUserId(fileId, userId))
                .thenReturn(Optional.of(fileMetadata));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(storageService).deleteFile(storageKey);
        doNothing().when(fileMetadataRepository).delete(fileMetadata);

        // Act
        fileService.deleteFile(fileId, userId);

        // Assert
        verify(storageService).deleteFile(storageKey);
        verify(fileMetadataRepository).delete(fileMetadata);
        verify(userRepository).save(any(User.class));
        verify(storageUsageService).invalidateCache(userId);
    }

    @Test
    void testDeleteFile_NotFound() {
        // Arrange
        Long userId = 1L;
        UUID fileId = UUID.randomUUID();

        when(fileMetadataRepository.findByIdAndUserId(fileId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> fileService.deleteFile(fileId, userId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("File not found");

        verify(storageService, never()).deleteFile(any());
        verify(fileMetadataRepository, never()).delete(any());
    }

    @Test
    void testDeleteFile_UnauthorizedAccess() {
        // Arrange
        Long ownerUserId = 1L;
        Long otherUserId = 2L;
        UUID fileId = UUID.randomUUID();

        when(fileMetadataRepository.findByIdAndUserId(fileId, otherUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> fileService.deleteFile(fileId, otherUserId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("File not found");

        verify(storageService, never()).deleteFile(any());
    }
}
