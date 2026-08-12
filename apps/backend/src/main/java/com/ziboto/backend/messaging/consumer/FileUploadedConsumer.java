package com.ziboto.backend.messaging.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ziboto.backend.config.RabbitMQConfig;
import com.ziboto.backend.messaging.event.FileUploadedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer for file uploaded events.
 * 
 * <p>Processes async tasks triggered by file uploads:</p>
 * <ul>
 *   <li>Update search index (Elasticsearch - V2)</li>
 *   <li>Generate thumbnails (if image/video - V2)</li>
 *   <li>Extract metadata (EXIF, etc - V2)</li>
 *   <li>Virus scanning (future - V3)</li>
 * </ul>
 * 
 * <p>Failures are retried automatically (configured in RabbitMQConfig).
 * After max retries, messages move to dead letter queue for manual inspection.</p>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadedConsumer {
    
    // Services will be injected as V2 features are added:
    // private final SearchIndexService searchIndexService;
    // private final ThumbnailService thumbnailService;
    // private final MetadataExtractorService metadataService;
    
    /**
     * Process file uploaded event.
     * 
     * @param event file uploaded event
     */
    @RabbitListener(queues = RabbitMQConfig.FILE_UPLOADED_QUEUE)
    public void handleFileUploaded(FileUploadedEvent event) {
        log.info("Processing file uploaded event: fileId={}, filename={}, size={} bytes", 
                 event.getFileId(), event.getFilename(), event.getSizeBytes());
        
        try {
            // TODO V2: Add to search index (Elasticsearch)
            // searchIndexService.indexFile(event);
            log.debug("Search indexing placeholder - will be implemented in V2");
            
            // TODO V2: Generate thumbnail if image/video
            // if (isImageOrVideo(event.getMimeType())) {
            //     thumbnailService.generateThumbnail(event);
            // }
            log.debug("Thumbnail generation placeholder - will be implemented in V2");
            
            // TODO V2: Extract metadata (EXIF, dimensions, etc)
            // metadataService.extractAndStore(event);
            log.debug("Metadata extraction placeholder - will be implemented in V2");
            
            // TODO V3: Virus scanning
            // virusScanService.scanFile(event);
            log.debug("Virus scanning placeholder - will be implemented in V3");
            
            log.info("File uploaded event processed successfully: fileId={}", event.getFileId());
            
        } catch (Exception e) {
            log.error("Failed to process file uploaded event: fileId={}", 
                      event.getFileId(), e);
            // Exception will trigger retry via RabbitMQ retry mechanism
            throw e;
        }
    }
    
    private boolean isImageOrVideo(String mimeType) {
        return mimeType != null && 
               (mimeType.startsWith("image/") || mimeType.startsWith("video/"));
    }
}
