package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.config.RemoteProperties;
import com.lqtigee.sparkai.dto.AttachmentDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

    private final RemoteProperties remoteProperties;

    public AttachmentService(RemoteProperties remoteProperties) {
        this.remoteProperties = remoteProperties;
        this.remoteProperties.validate();
    }

    public AttachmentDto upload(MultipartFile file) {
        validateFile(file);

        String id = "att_" + UUID.randomUUID().toString().replace("-", "");
        String contentType = file.getContentType().trim();
        String filename = safeFilename(file.getOriginalFilename());
        Path attachmentRoot = attachmentRoot();
        Path destination = attachmentRoot.resolve(id).normalize();
        if (!destination.startsWith(attachmentRoot)) {
            throw new ApiException(
                    ErrorCode.ATTACHMENT_PATH_INVALID,
                    HttpStatus.BAD_REQUEST,
                    "Attachment path is invalid",
                    id
            );
        }

        try {
            Files.createDirectories(attachmentRoot);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination);
            }
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.ATTACHMENT_STORAGE_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Attachment storage failed",
                    exception.getMessage()
            );
        } catch (RuntimeException exception) {
            if (exception instanceof ApiException apiException) {
                throw apiException;
            }
            throw exception;
        }

        return new AttachmentDto(id, filename, contentType, file.getSize(), Instant.now());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(
                    ErrorCode.ATTACHMENT_MISSING,
                    HttpStatus.BAD_REQUEST,
                    "Attachment file is required",
                    "file"
            );
        }
        if (file.getSize() > remoteProperties.getMaxUploadBytes()) {
            throw new ApiException(
                    ErrorCode.ATTACHMENT_TOO_LARGE,
                    HttpStatus.BAD_REQUEST,
                    "Attachment is too large",
                    "maxUploadBytes=" + remoteProperties.getMaxUploadBytes()
            );
        }
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank() || !allowedContentTypes().contains(contentType.trim())) {
            throw new ApiException(
                    ErrorCode.ATTACHMENT_CONTENT_TYPE_FORBIDDEN,
                    HttpStatus.BAD_REQUEST,
                    "Attachment content type is forbidden",
                    contentType
            );
        }
    }

    private Set<String> allowedContentTypes() {
        return Set.copyOf(remoteProperties.getAllowedContentTypes());
    }

    private Path attachmentRoot() {
        return Path.of(remoteProperties.getAttachmentRoot()).toAbsolutePath().normalize();
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "attachment";
        }
        String filename = Path.of(originalFilename.replace("\\", "/")).getFileName().toString();
        if (filename.isBlank()) {
            return "attachment";
        }
        return filename;
    }
}
