package com.lqtigee.sparkai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.SourceCapabilityDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class CapabilityServiceTest {

    private final CapabilityService capabilityService = new CapabilityService();

    @Test
    void listCapabilitiesReturnsImplementedSourceCapabilitiesOnly() {
        List<SourceCapabilityDto> capabilities = capabilityService.listCapabilities();

        assertThat(capabilities)
                .extracting(SourceCapabilityDto::source)
                .containsExactly(AgentSource.CODEX, AgentSource.OPENCODE);

        SourceCapabilityDto codex = capabilities.getFirst();
        assertThat(codex.runOptions()).containsExactly("model");
        assertThat(codex.attachments()).containsExactly("image");
        assertThat(codex.sessionActions()).containsExactly("archive", "delete", "unarchive", "fork");
        assertThat(codex.dangerousOptions()).isEmpty();

        SourceCapabilityDto opencode = capabilities.get(1);
        assertThat(opencode.runOptions())
                .containsExactly("model", "agent", "fork", "share", "variant", "thinking", "replay", "replayLimit");
        assertThat(opencode.attachments()).containsExactly("file");
        assertThat(opencode.sessionActions()).containsExactly("delete", "export");
        assertThat(opencode.dangerousOptions()).containsExactly("shellDangerouslySkipPermissions");
    }
}
