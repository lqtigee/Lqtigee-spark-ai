package com.lqtigee.sparkai.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StartRunRequestTest {

    @Test
    void storesCodexOptionsOnRequest() {
        CodexRunOptionsDto codexOptions = new CodexRunOptionsDto(
                List.of("att_image_01"),
                "work",
                "workspace-write",
                "on-request",
                true,
                List.of("att_dir_01"),
                List.of(new CodexRunOptionsDto.ConfigOverrideDto("model_reasoning_effort", "high")),
                "att_schema_01"
        );

        StartRunRequest request = new StartRunRequest(
                "session-1",
                AgentSource.CODEX,
                "gpt-5",
                CommandMode.ASK,
                "continue",
                false,
                codexOptions
        );

        assertThat(request.codexOptions()).isSameAs(codexOptions);
        assertThat(request.codexOptions().imageAttachmentIds()).containsExactly("att_image_01");
        assertThat(request.codexOptions().profile()).isEqualTo("work");
        assertThat(request.codexOptions().sandbox()).isEqualTo("workspace-write");
        assertThat(request.codexOptions().approvalPolicy()).isEqualTo("on-request");
        assertThat(request.codexOptions().searchEnabled()).isTrue();
        assertThat(request.codexOptions().addDirAttachmentIds()).containsExactly("att_dir_01");
        assertThat(request.codexOptions().configOverrides())
                .containsExactly(new CodexRunOptionsDto.ConfigOverrideDto("model_reasoning_effort", "high"));
        assertThat(request.codexOptions().outputSchemaAttachmentId()).isEqualTo("att_schema_01");
    }

    @Test
    void keepsLegacyConstructorWithoutCodexOptions() {
        StartRunRequest request = new StartRunRequest(
                "session-1",
                AgentSource.OPENCODE,
                "openai/Lqtigee",
                CommandMode.ASK,
                "continue",
                false
        );

        assertThat(request.codexOptions()).isNull();
    }
}
