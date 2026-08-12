package com.ziboto.backend.search.document;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Elasticsearch document for file search.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Document(indexName = "files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDocument {
    
    @Id
    private String id; // UUID as string
    
    @Field(type = FieldType.Long)
    private Long userId;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String fileName;
    
    @Field(type = FieldType.Text)
    private String originalFileName;
    
    @Field(type = FieldType.Keyword)
    private String fileExtension;
    
    @Field(type = FieldType.Keyword)
    private String mimeType;
    
    @Field(type = FieldType.Long)
    private Long fileSize;
    
    @Field(type = FieldType.Keyword)
    private String folderId; // UUID as string
    
    @Field(type = FieldType.Text)
    private String folderPath;
    
    @Field(type = FieldType.Text)
    private String tags;
    
    @Field(type = FieldType.Date)
    private LocalDateTime uploadedAt;
    
    @Field(type = FieldType.Date)
    private LocalDateTime lastModified;
    
    @Field(type = FieldType.Keyword)
    private String sha256Hash;
    
    // Helper methods
    
    public UUID getFileId() {
        return id != null ? UUID.fromString(id) : null;
    }
    
    public UUID getFolderIdAsUUID() {
        return folderId != null ? UUID.fromString(folderId) : null;
    }
    
    public String getFormattedFileSize() {
        if (fileSize == null || fileSize == 0) {
            return "0 B";
        }
        
        if (fileSize < 1024) {
            return fileSize + " B";
        }
        
        int exp = (int) (Math.log(fileSize) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", fileSize / Math.pow(1024, exp), pre);
    }
}
