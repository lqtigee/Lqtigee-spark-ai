package com.lqtigee.sparkai.web;

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

    public record SessionsResponse(List<RemoteSessionDto> sessions) {
    }
}
