package com.lqtigee.sparkai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.config.RemoteProperties;
import com.lqtigee.sparkai.dto.AttachmentDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class AttachmentServiceTest {

    private final Path testRoot = Path.of("/home/lqtiger/.lqtigee-spark-ai/test-attachments-" + UUID.randomUUID())
            .toAbsolutePath()
            .normalize();

    @AfterEach
    void removeTestRoot() throws IOException {
        if (!Files.exists(testRoot)) {
            return;
        }
        try (var paths = Files.walk(testRoot)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException("Failed to delete test attachment path", exception);
                        }
                    });
        }
    }

    @Test
    void uploadStoresFileUnderConfiguredAttachmentRootAndReturnsDto() throws IOException {
        AttachmentService service = new AttachmentService(properties(testRoot.resolve("attachments"), 100));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "context.txt",
                "text/plain",
                "hello".getBytes()
        );

        AttachmentDto dto = service.upload(file);

        assertThat(dto.id()).startsWith("att_");
        assertThat(dto.filename()).isEqualTo("context.txt");
        assertThat(dto.contentType()).isEqualTo("text/plain");
        assertThat(dto.sizeBytes()).isEqualTo(5);
        assertThat(dto.createdAt()).isNotNull();
        assertThat(Files.readString(testRoot.resolve("attachments").resolve(dto.id()))).isEqualTo("hello");
    }

    @Test
    void uploadUsesSafeDisplayFilenameOnly() {
        AttachmentService service = new AttachmentService(properties(testRoot.resolve("attachments"), 100));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../secret.txt",
                "text/plain",
                "hello".getBytes()
        );

        AttachmentDto dto = service.upload(file);

        assertThat(dto.filename()).isEqualTo("secret.txt");
        assertThat(dto.id()).doesNotContain("secret");
    }

    @Test
    void uploadRejectsMissingFile() {
        AttachmentService service = new AttachmentService(properties(testRoot.resolve("attachments"), 100));

        assertApiException(() -> service.upload(null), ErrorCode.ATTACHMENT_MISSING);
    }

    @Test
    void uploadRejectsOversizedFile() {
        AttachmentService service = new AttachmentService(properties(testRoot.resolve("attachments"), 3));
        MockMultipartFile file = new MockMultipartFile("file", "context.txt", "text/plain", "hello".getBytes());

        assertApiException(() -> service.upload(file), ErrorCode.ATTACHMENT_TOO_LARGE);
    }

    @Test
    void uploadRejectsForbiddenContentType() {
        AttachmentService service = new AttachmentService(properties(testRoot.resolve("attachments"), 100));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "context.bin",
                "application/octet-stream",
                "hello".getBytes()
        );

        assertApiException(() -> service.upload(file), ErrorCode.ATTACHMENT_CONTENT_TYPE_FORBIDDEN);
    }

    @Test
    void uploadReturnsStorageFailureWhenInputStreamFails() {
        AttachmentService service = new AttachmentService(properties(testRoot.resolve("attachments"), 100));

        assertApiException(
                () -> service.upload(new FailingMultipartFile()),
                ErrorCode.ATTACHMENT_STORAGE_FAILED
        );
    }

    @Test
    void deleteRemovesExistingAttachment() throws IOException {
        AttachmentService service = new AttachmentService(properties(testRoot.resolve("attachments"), 100));
        AttachmentDto dto = service.upload(new MockMultipartFile(
                "file",
                "context.txt",
                "text/plain",
                "hello".getBytes()
        ));

        service.delete(dto.id());

        assertThat(Files.exists(testRoot.resolve("attachments").resolve(dto.id()))).isFalse();
    }

    @Test
    void deleteRejectsMissingAttachment() {
        AttachmentService service = new AttachmentService(properties(testRoot.resolve("attachments"), 100));

        assertApiException(
                () -> service.delete("att_00000000000000000000000000000000"),
                ErrorCode.ATTACHMENT_NOT_FOUND
        );
    }

    @Test
    void deleteRejectsPathLikeIdWithoutTouchingOutsideFile() throws IOException {
        AttachmentService service = new AttachmentService(properties(testRoot.resolve("attachments"), 100));
        Files.createDirectories(testRoot);
        Path outsideFile = testRoot.resolve("outside.txt");
        Files.writeString(outsideFile, "keep");

        assertApiException(() -> service.delete("../outside.txt"), ErrorCode.ATTACHMENT_NOT_FOUND);

        assertThat(Files.readString(outsideFile)).isEqualTo("keep");
    }

    private RemoteProperties properties(Path attachmentRoot, long maxUploadBytes) {
        RemoteProperties properties = new RemoteProperties();
        properties.setMaxPromptChars(8000);
        properties.setAttachmentRoot(attachmentRoot.toString());
        properties.setMaxUploadBytes(maxUploadBytes);
        properties.setAllowedContentTypes(List.of("text/plain", "image/png"));
        return properties;
    }

    private void assertApiException(ThrowingRunnable action, ErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(code));
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static class FailingMultipartFile implements MultipartFile {

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return "context.txt";
        }

        @Override
        public String getContentType() {
            return "text/plain";
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public long getSize() {
            return 5;
        }

        @Override
        public byte[] getBytes() throws IOException {
            throw new IOException("broken");
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            throw new IOException("broken");
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            throw new IOException("broken");
        }
    }
}
