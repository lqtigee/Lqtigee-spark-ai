package com.lqtigee.sparkai.config;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;

@ConfigurationProperties(prefix = "lqtigee.remote")
public class RemoteProperties {

    private static final Path SERVICE_OWNED_ROOT = Path.of("/home/lqtiger/.lqtigee-spark-ai").toAbsolutePath().normalize();

    private int maxPromptChars;
    private String attachmentRoot = SERVICE_OWNED_ROOT.resolve("attachments").toString();
    private long maxUploadBytes = 10_485_760L;
    private List<String> allowedContentTypes = new ArrayList<>(List.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "text/plain",
            "application/json"
    ));

    public int getMaxPromptChars() {
        return maxPromptChars;
    }

    public void setMaxPromptChars(int maxPromptChars) {
        this.maxPromptChars = maxPromptChars;
    }

    public String getAttachmentRoot() {
        return attachmentRoot;
    }

    public void setAttachmentRoot(String attachmentRoot) {
        this.attachmentRoot = attachmentRoot;
    }

    public long getMaxUploadBytes() {
        return maxUploadBytes;
    }

    public void setMaxUploadBytes(long maxUploadBytes) {
        this.maxUploadBytes = maxUploadBytes;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes == null ? new ArrayList<>() : allowedContentTypes;
    }

    public void validate() {
        if (maxPromptChars <= 0) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "Remote prompt limit must be positive",
                    "lqtigee.remote.max-prompt-chars"
            );
        }
        if (attachmentRoot == null || attachmentRoot.isBlank()) {
            throw validationFailed("Attachment root must be configured", "lqtigee.remote.attachment-root");
        }
        Path normalizedAttachmentRoot = Path.of(attachmentRoot).toAbsolutePath().normalize();
        if (!normalizedAttachmentRoot.startsWith(SERVICE_OWNED_ROOT)) {
            throw validationFailed("Attachment root must stay inside the service-owned directory", "lqtigee.remote.attachment-root");
        }
        if (maxUploadBytes <= 0) {
            throw validationFailed("Attachment max upload size must be positive", "lqtigee.remote.max-upload-bytes");
        }
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            throw validationFailed("Attachment content type allowlist must not be empty", "lqtigee.remote.allowed-content-types");
        }
        for (String contentType : allowedContentTypes) {
            if (contentType == null || contentType.isBlank()) {
                throw validationFailed("Attachment content type allowlist must not contain blank values", "lqtigee.remote.allowed-content-types");
            }
        }
    }

    private ApiException validationFailed(String message, String detail) {
        return new ApiException(
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST,
                message,
                detail
        );
    }
}
