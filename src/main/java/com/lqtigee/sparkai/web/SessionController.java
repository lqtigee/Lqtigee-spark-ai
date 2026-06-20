package com.lqtigee.sparkai.web;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionTranscriptDto;
import com.lqtigee.sparkai.service.SessionService;
import com.lqtigee.sparkai.service.SessionTranscriptService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    private final SessionService sessionService;
    private final SessionTranscriptService sessionTranscriptService;

    public SessionController(SessionService sessionService, SessionTranscriptService sessionTranscriptService) {
        this.sessionService = sessionService;
        this.sessionTranscriptService = sessionTranscriptService;
    }

    @GetMapping("/api/sessions")
    public SessionsResponse listSessions() {
        return new SessionsResponse(sessionService.listAllSessions());
    }

    @GetMapping("/api/codex/sessions")
    public SessionsResponse listCodexSessions() {
        return new SessionsResponse(sessionService.listBySource(AgentSource.CODEX));
    }

    @GetMapping("/api/opencode/sessions")
    public SessionsResponse listOpencodeSessions() {
        return new SessionsResponse(sessionService.listBySource(AgentSource.OPENCODE));
    }

    @GetMapping("/api/sessions/{source}/{id}/transcript")
    public SessionTranscriptDto getTranscript(
            @PathVariable AgentSource source,
            @PathVariable String id,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String before
    ) {
        return sessionTranscriptService.getTranscript(source, id, limit, before);
    }

    public record SessionsResponse(List<RemoteSessionDto> sessions) {
    }
}
