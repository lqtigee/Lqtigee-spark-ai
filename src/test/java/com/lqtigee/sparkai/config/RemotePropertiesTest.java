package com.lqtigee.sparkai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;

class RemotePropertiesTest {

    @Test
    void validatePassesWhenAttachmentConfigIsServiceOwned() {
        RemoteProperties properties = validProperties();

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void validatePassesWithServiceOwnedDefaults() {
        RemoteProperties properties = new RemoteProperties();
        properties.setMaxPromptChars(8000);

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void validateFailsWhenAttachmentRootIsBlank() {
        RemoteProperties properties = validProperties();
        properties.setAttachmentRoot(" ");

        assertValidationFailure(properties, "lqtigee.remote.attachment-root");
    }

    @Test
    void validateFailsWhenAttachmentRootEscapesServiceOwnedDirectory() {
        RemoteProperties properties = validProperties();
        properties.setAttachmentRoot("/tmp/lqtigee-attachments");

        assertValidationFailure(properties, "lqtigee.remote.attachment-root");
    }

    @Test
    void validateFailsWhenMaxUploadBytesIsNotPositive() {
        RemoteProperties properties = validProperties();
        properties.setMaxUploadBytes(0);

        assertValidationFailure(properties, "lqtigee.remote.max-upload-bytes");
    }

    @Test
    void validateFailsWhenAllowedContentTypesAreEmpty() {
        RemoteProperties properties = validProperties();
        properties.setAllowedContentTypes(List.of());

        assertValidationFailure(properties, "lqtigee.remote.allowed-content-types");
    }

    @Test
    void validateFailsWhenAllowedContentTypesContainBlankValue() {
        RemoteProperties properties = validProperties();
        properties.setAllowedContentTypes(List.of("image/png", " "));

        assertValidationFailure(properties, "lqtigee.remote.allowed-content-types");
    }

    private RemoteProperties validProperties() {
        RemoteProperties properties = new RemoteProperties();
        properties.setMaxPromptChars(8000);
        properties.setAttachmentRoot("/home/lqtiger/.lqtigee-spark-ai/attachments");
        properties.setMaxUploadBytes(10_485_760);
        properties.setAllowedContentTypes(List.of("image/png", "image/jpeg", "text/plain"));
        return properties;
    }

    private void assertValidationFailure(RemoteProperties properties, String detail) {
        assertThatThrownBy(properties::validate)
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(exception.detail()).isEqualTo(detail);
                });
    }
}
