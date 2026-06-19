package com.lqtigee.sparkai.web;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.service.SessionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
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

    public record SessionsResponse(List<RemoteSessionDto> sessions) {
    }
}
