package com.lqtigee.sparkai.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.RunRecordDto;
import com.lqtigee.sparkai.dto.RunStatus;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.dto.StartRunResponse;
import com.lqtigee.sparkai.service.RunService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "lqtigee.security.api-token=test-token")
@AutoConfigureMockMvc
class RunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RunService runService;

    @Test
    void startWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-1",
                                  "source": "CODEX",
                                  "modelId": "codex-default",
                                  "mode": "ASK",
                                  "prompt": "hello",
                                  "confirmDangerous": false
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_MISSING"));

        verifyNoInteractions(runService);
    }

    @Test
    void eventsWithWrongTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/runs/run-1/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));

        verifyNoInteractions(runService);
    }

    @Test
    void stopWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/runs/run-1/stop"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_MISSING"));

        verifyNoInteractions(runService);
    }

    @Test
    void startWithValidTokenReachesService() throws Exception {
        when(runService.start(any(StartRunRequest.class)))
                .thenReturn(new StartRunResponse(
                        "run-1",
                        "session-1",
                        AgentSource.CODEX,
                        RunStatus.RUNNING,
                        Instant.parse("2026-06-20T00:00:00Z")));

        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-1",
                                  "source": "CODEX",
                                  "modelId": "codex-default",
                                  "mode": "ASK",
                                  "prompt": "hello",
                                  "confirmDangerous": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("run-1"))
                .andExpect(jsonPath("$.status").value("RUNNING"));

        verify(runService).start(any(StartRunRequest.class));
    }

    @Test
    void listRunsWithValidTokenReturnsCurrentRuns() throws Exception {
        when(runService.listRuns()).thenReturn(List.of(new RunRecordDto(
                "run-1",
                "session-1",
                AgentSource.CODEX,
                "gpt-5.5",
                CommandMode.ASK,
                RunStatus.RUNNING,
                null,
                null,
                Instant.parse("2026-06-20T00:00:00Z"),
                false
        )));

        mockMvc.perform(get("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runId").value("run-1"))
                .andExpect(jsonPath("$[0].source").value("CODEX"))
                .andExpect(jsonPath("$[0].status").value("RUNNING"));

        verify(runService).listRuns();
    }
}
