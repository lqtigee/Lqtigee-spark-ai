package com.lqtigee.sparkai.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorCodeTest {

    @Test
    void containsOpencodeAgentListCodes() {
        assertThat(ErrorCode.valueOf("OPENCODE_AGENT_LIST_FAILED")).isEqualTo(ErrorCode.OPENCODE_AGENT_LIST_FAILED);
        assertThat(ErrorCode.valueOf("OPENCODE_AGENT_OUTPUT_INVALID")).isEqualTo(ErrorCode.OPENCODE_AGENT_OUTPUT_INVALID);
        assertThat(ErrorCode.valueOf("OPENCODE_AGENT_SOURCE_UNAVAILABLE")).isEqualTo(ErrorCode.OPENCODE_AGENT_SOURCE_UNAVAILABLE);
    }

    @Test
    void containsAttachmentCodes() {
        assertThat(ErrorCode.valueOf("ATTACHMENT_MISSING")).isEqualTo(ErrorCode.ATTACHMENT_MISSING);
        assertThat(ErrorCode.valueOf("ATTACHMENT_TOO_LARGE")).isEqualTo(ErrorCode.ATTACHMENT_TOO_LARGE);
        assertThat(ErrorCode.valueOf("ATTACHMENT_CONTENT_TYPE_FORBIDDEN")).isEqualTo(ErrorCode.ATTACHMENT_CONTENT_TYPE_FORBIDDEN);
        assertThat(ErrorCode.valueOf("ATTACHMENT_STORAGE_FAILED")).isEqualTo(ErrorCode.ATTACHMENT_STORAGE_FAILED);
        assertThat(ErrorCode.valueOf("ATTACHMENT_NOT_FOUND")).isEqualTo(ErrorCode.ATTACHMENT_NOT_FOUND);
        assertThat(ErrorCode.valueOf("ATTACHMENT_DELETE_FAILED")).isEqualTo(ErrorCode.ATTACHMENT_DELETE_FAILED);
        assertThat(ErrorCode.valueOf("ATTACHMENT_PATH_INVALID")).isEqualTo(ErrorCode.ATTACHMENT_PATH_INVALID);
    }
}
