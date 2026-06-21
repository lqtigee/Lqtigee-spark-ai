package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.config.RemoteProperties;
import com.lqtigee.sparkai.dto.AttachmentDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

    private static final Pattern ATTACHMENT_ID_PATTERN = Pattern.compile("att_[a-f0-9]{32}");
    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";

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
            Files.writeString(contentTypePath(id), contentType);
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

    public void delete(String id) {
        Path target = attachmentPath(id);
        try {
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw attachmentNotFound(id);
            }
            Files.delete(target);
            Files.deleteIfExists(contentTypePath(id));
        } catch (NoSuchFileException exception) {
            throw attachmentNotFound(id);
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.ATTACHMENT_DELETE_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Attachment delete failed",
                    exception.getMessage()
            );
        }
    }

    public ResolvedAttachment requireImageAttachment(String id) {
        Path target = attachmentPath(id);
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw attachmentNotFound(id);
        }
        String contentType = readStoredContentType(id);
        if (contentType == null || !contentType.startsWith(IMAGE_CONTENT_TYPE_PREFIX)) {
            throw new ApiException(
                    ErrorCode.ATTACHMENT_CONTENT_TYPE_FORBIDDEN,
                    HttpStatus.BAD_REQUEST,
                    "Attachment content type is forbidden",
                    contentType
            );
        }
        return new ResolvedAttachment(id, target, contentType);
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

    private Path attachmentPath(String id) {
        if (id == null || !ATTACHMENT_ID_PATTERN.matcher(id).matches()) {
            throw attachmentNotFound(id);
        }
        Path attachmentRoot = attachmentRoot();
        Path target = attachmentRoot.resolve(id).normalize();
        if (!target.startsWith(attachmentRoot)) {
            throw attachmentNotFound(id);
        }
        return target;
    }

    private Path contentTypePath(String id) {
        return attachmentRoot().resolve(id + ".content-type").normalize();
    }

    private String readStoredContentType(String id) {
        try {
            Path contentTypePath = contentTypePath(id);
            if (!contentTypePath.startsWith(attachmentRoot()) || !Files.isRegularFile(contentTypePath, LinkOption.NOFOLLOW_LINKS)) {
                throw attachmentNotFound(id);
            }
            return Files.readString(contentTypePath).trim();
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.ATTACHMENT_STORAGE_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Attachment content type read failed",
                    exception.getMessage()
            );
        }
    }

    private ApiException attachmentNotFound(String id) {
        return new ApiException(
                ErrorCode.ATTACHMENT_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Attachment was not found",
                id
        );
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

    public record ResolvedAttachment(
            String id,
            Path path,
            String contentType
    ) {
    }
}
