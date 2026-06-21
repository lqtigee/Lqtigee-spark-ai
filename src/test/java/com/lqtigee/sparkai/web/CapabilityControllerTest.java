package com.lqtigee.sparkai.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.SourceCapabilityDto;
import com.lqtigee.sparkai.service.CapabilityService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "lqtigee.security.api-token=test-token")
@AutoConfigureMockMvc
class CapabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CapabilityService capabilityService;

    @Test
    void listCapabilitiesWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/capabilities"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_MISSING"));

        verifyNoInteractions(capabilityService);
    }

    @Test
    void listCapabilitiesWithValidTokenReturnsServiceCapabilities() throws Exception {
        when(capabilityService.listCapabilities()).thenReturn(List.of(
                new SourceCapabilityDto(
                        AgentSource.CODEX,
                        List.of("model"),
                        List.of("image"),
                        List.of(),
                        List.of()
                ),
                new SourceCapabilityDto(
                        AgentSource.OPENCODE,
                        List.of("model", "agent", "fork", "share", "variant", "thinking", "replay", "replayLimit"),
                        List.of("file"),
                        List.of(),
                        List.of("shellDangerouslySkipPermissions")
                )
        ));

        mockMvc.perform(get("/api/capabilities")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capabilities[0].source").value("CODEX"))
                .andExpect(jsonPath("$.capabilities[0].runOptions[0]").value("model"))
                .andExpect(jsonPath("$.capabilities[0].attachments[0]").value("image"))
                .andExpect(jsonPath("$.capabilities[0].sessionActions").isEmpty())
                .andExpect(jsonPath("$.capabilities[0].dangerousOptions").isEmpty())
                .andExpect(jsonPath("$.capabilities[1].source").value("OPENCODE"))
                .andExpect(jsonPath("$.capabilities[1].runOptions[0]").value("model"))
                .andExpect(jsonPath("$.capabilities[1].runOptions[1]").value("agent"))
                .andExpect(jsonPath("$.capabilities[1].runOptions[7]").value("replayLimit"))
                .andExpect(jsonPath("$.capabilities[1].attachments[0]").value("file"))
                .andExpect(jsonPath("$.capabilities[1].sessionActions").isEmpty())
                .andExpect(jsonPath("$.capabilities[1].dangerousOptions[0]").value("shellDangerouslySkipPermissions"));

        verify(capabilityService).listCapabilities();
    }
}
