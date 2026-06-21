package com.lqtigee.sparkai.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lqtigee.sparkai.dto.OpencodeAgentDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.opencode.OpencodeAgentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "lqtigee.security.api-token=test-token")
@AutoConfigureMockMvc
class OpencodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpencodeAgentService opencodeAgentService;

    @Test
    void listAgentsWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/opencode/agents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_MISSING"));

        verifyNoInteractions(opencodeAgentService);
    }

    @Test
    void listAgentsWithValidTokenReturnsRealServiceAgents() throws Exception {
        when(opencodeAgentService.listAgents()).thenReturn(List.of(
                new OpencodeAgentDto("build", "build", "primary"),
                new OpencodeAgentDto("explore", "explore", "subagent")
        ));

        mockMvc.perform(get("/api/opencode/agents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opencodeAgents[0].id").value("build"))
                .andExpect(jsonPath("$.opencodeAgents[0].name").value("build"))
                .andExpect(jsonPath("$.opencodeAgents[0].source").value("primary"))
                .andExpect(jsonPath("$.opencodeAgents[1].id").value("explore"))
                .andExpect(jsonPath("$.opencodeAgents[1].source").value("subagent"));

        verify(opencodeAgentService).listAgents();
    }

    @Test
    void listAgentsServiceFailureReturnsTypedError() throws Exception {
        when(opencodeAgentService.listAgents()).thenThrow(new ApiException(
                ErrorCode.OPENCODE_AGENT_LIST_FAILED,
                HttpStatus.FAILED_DEPENDENCY,
                "opencode agent list failed",
                "exitCode=2"
        ));

        mockMvc.perform(get("/api/opencode/agents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isFailedDependency())
                .andExpect(jsonPath("$.code").value("OPENCODE_AGENT_LIST_FAILED"))
                .andExpect(jsonPath("$.detail").value("exitCode=2"));

        verify(opencodeAgentService).listAgents();
    }
}
