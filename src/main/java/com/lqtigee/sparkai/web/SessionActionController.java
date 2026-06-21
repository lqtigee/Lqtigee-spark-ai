package com.lqtigee.sparkai.web;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.SessionActionRequest;
import com.lqtigee.sparkai.dto.SessionActionResponse;
import com.lqtigee.sparkai.service.SessionActionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionActionController {

    private final SessionActionService sessionActionService;

    public SessionActionController(SessionActionService sessionActionService) {
        this.sessionActionService = sessionActionService;
    }

    @PostMapping("/api/sessions/{source}/{id}/actions")
    public SessionActionResponse startAction(
            @PathVariable AgentSource source,
            @PathVariable String id,
            @RequestBody SessionActionRequest request
    ) {
        return sessionActionService.startAction(source, id, request);
    }
}
