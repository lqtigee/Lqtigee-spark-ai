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
                codexOptions,
                null
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
    void storesOpencodeOptionsOnRequest() {
        OpencodeRunOptionsDto opencodeOptions = new OpencodeRunOptionsDto(
                "build",
                true,
                false,
                "high",
                true,
                true,
                10,
                List.of("att_file_01")
        );

        StartRunRequest request = new StartRunRequest(
                "session-1",
                AgentSource.OPENCODE,
                "openai/Lqtigee",
                CommandMode.ASK,
                "continue",
                false,
                null,
                opencodeOptions
        );

        assertThat(request.opencodeOptions()).isSameAs(opencodeOptions);
        assertThat(request.opencodeOptions().agent()).isEqualTo("build");
        assertThat(request.opencodeOptions().fork()).isTrue();
        assertThat(request.opencodeOptions().share()).isFalse();
        assertThat(request.opencodeOptions().variant()).isEqualTo("high");
        assertThat(request.opencodeOptions().thinking()).isTrue();
        assertThat(request.opencodeOptions().replay()).isTrue();
        assertThat(request.opencodeOptions().replayLimit()).isEqualTo(10);
        assertThat(request.opencodeOptions().fileAttachmentIds()).containsExactly("att_file_01");
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
        assertThat(request.opencodeOptions()).isNull();
    }
}
