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
}
