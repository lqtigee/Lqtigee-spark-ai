package com.lqtigee.sparkai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.SessionActionRequest;
import com.lqtigee.sparkai.dto.SessionActionResponse;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.service.SessionActionService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "lqtigee.security.api-token=test-token")
@AutoConfigureMockMvc
class SessionActionControllerTest {

    private static final String CODEX_SESSION_ID = "019ee090-24e8-7ac1-bd1c-8e4d6788fbf1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionActionService sessionActionService;

    @Test
    void startActionWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/sessions/CODEX/{id}/actions", CODEX_SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "archive",
                                  "confirmDestructive": false
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_MISSING"));

        verifyNoInteractions(sessionActionService);
    }

    @Test
    void startActionWithValidTokenReturnsStartedResponse() throws Exception {
        when(sessionActionService.startAction(eq(AgentSource.CODEX), eq(CODEX_SESSION_ID), any(SessionActionRequest.class)))
                .thenReturn(new SessionActionResponse(
                        "act_01",
                        AgentSource.CODEX,
                        CODEX_SESSION_ID,
                        "archive",
                        "STARTED",
                        Instant.parse("2026-06-20T00:00:00Z")
                ));

        mockMvc.perform(post("/api/sessions/CODEX/{id}/actions", CODEX_SESSION_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "archive",
                                  "confirmDestructive": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionId").value("act_01"))
                .andExpect(jsonPath("$.source").value("CODEX"))
                .andExpect(jsonPath("$.sessionId").value(CODEX_SESSION_ID))
                .andExpect(jsonPath("$.action").value("archive"))
                .andExpect(jsonPath("$.status").value("STARTED"))
                .andExpect(jsonPath("$.startedAt").value("2026-06-20T00:00:00Z"));

        ArgumentCaptor<SessionActionRequest> requestCaptor = ArgumentCaptor.forClass(SessionActionRequest.class);
        verify(sessionActionService).startAction(eq(AgentSource.CODEX), eq(CODEX_SESSION_ID), requestCaptor.capture());
        assertThat(requestCaptor.getValue().action()).isEqualTo("archive");
        assertThat(requestCaptor.getValue().confirmDestructive()).isFalse();
    }

    @Test
    void missingDestructiveConfirmationReturnsTypedError() throws Exception {
        when(sessionActionService.startAction(eq(AgentSource.CODEX), eq(CODEX_SESSION_ID), any(SessionActionRequest.class)))
                .thenThrow(new ApiException(
                        ErrorCode.DANGER_CONFIRM_REQUIRED,
                        HttpStatus.BAD_REQUEST,
                        "Codex session delete requires explicit confirmation",
                        "confirmDestructive"
                ));

        mockMvc.perform(post("/api/sessions/CODEX/{id}/actions", CODEX_SESSION_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "delete",
                                  "confirmDestructive": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DANGER_CONFIRM_REQUIRED"))
                .andExpect(jsonPath("$.detail").value("confirmDestructive"));

        verify(sessionActionService).startAction(eq(AgentSource.CODEX), eq(CODEX_SESSION_ID), any(SessionActionRequest.class));
    }
}
