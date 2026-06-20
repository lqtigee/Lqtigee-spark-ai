package com.lqtigee.sparkai.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionMessageDto;
import com.lqtigee.sparkai.dto.SessionStatus;
import com.lqtigee.sparkai.dto.SessionTranscriptDto;
import com.lqtigee.sparkai.service.SessionService;
import com.lqtigee.sparkai.service.SessionTranscriptService;
import java.time.Instant;
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
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionService sessionService;

    @MockitoBean
    private SessionTranscriptService sessionTranscriptService;

    @Test
    void listSessionsWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_MISSING"));
    }

    @Test
    void listSessionsWithValidTokenReachesService() throws Exception {
        when(sessionService.listAllSessions()).thenReturn(List.of());

        mockMvc.perform(get("/api/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions").isArray())
                .andExpect(jsonPath("$.sessions").isEmpty());

        verify(sessionService).listAllSessions();
    }

    @Test
    void getTranscriptWithValidTokenReachesService() throws Exception {
        RemoteSessionDto session = new RemoteSessionDto(
                "session-id",
                AgentSource.CODEX,
                "Session title",
                "/workspace",
                "gpt-5.5",
                SessionStatus.UNKNOWN,
                Instant.parse("2026-06-20T00:00:00Z"),
                "Last message",
                "/sessions/session.jsonl"
        );
        SessionMessageDto message = new SessionMessageDto(
                "msg-1",
                "user",
                "Open this session as chat",
                Instant.parse("2026-06-20T00:01:00Z")
        );
        when(sessionTranscriptService.getTranscript(AgentSource.CODEX, "session-id", null, null))
                .thenReturn(new SessionTranscriptDto(session, List.of(message)));

        mockMvc.perform(get("/api/sessions/CODEX/session-id/transcript")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.id").value("session-id"))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].text").value("Open this session as chat"))
                .andExpect(jsonPath("$.pageInfo.oldestCursor").value("msg-1"))
                .andExpect(jsonPath("$.pageInfo.newestCursor").value("msg-1"))
                .andExpect(jsonPath("$.pageInfo.hasMoreBefore").value(false));

        verify(sessionTranscriptService).getTranscript(AgentSource.CODEX, "session-id", null, null);
    }

    @Test
    void getTranscriptPassesLimitAndBeforeQueryParams() throws Exception {
        RemoteSessionDto session = new RemoteSessionDto(
                "session-id",
                AgentSource.CODEX,
                "Session title",
                "/workspace",
                "gpt-5.5",
                SessionStatus.UNKNOWN,
                Instant.parse("2026-06-20T00:00:00Z"),
                "Last message",
                "/sessions/session.jsonl"
        );
        SessionMessageDto message = new SessionMessageDto(
                "msg-2",
                "assistant",
                "Older page",
                Instant.parse("2026-06-20T00:02:00Z")
        );
        when(sessionTranscriptService.getTranscript(AgentSource.CODEX, "session-id", 10, "msg-9"))
                .thenReturn(new SessionTranscriptDto(
                        session,
                        List.of(message),
                        new SessionTranscriptDto.TranscriptPageInfoDto("msg-2", "msg-2", true)
                ));

        mockMvc.perform(get("/api/sessions/CODEX/session-id/transcript")
                        .queryParam("limit", "10")
                        .queryParam("before", "msg-9")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].id").value("msg-2"))
                .andExpect(jsonPath("$.pageInfo.hasMoreBefore").value(true));

        verify(sessionTranscriptService).getTranscript(AgentSource.CODEX, "session-id", 10, "msg-9");
    }
}
