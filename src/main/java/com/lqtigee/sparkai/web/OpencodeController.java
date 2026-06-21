package com.lqtigee.sparkai.web;

import com.lqtigee.sparkai.dto.OpencodeAgentDto;
import com.lqtigee.sparkai.opencode.OpencodeAgentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpencodeController {

    private final OpencodeAgentService opencodeAgentService;

    public OpencodeController(OpencodeAgentService opencodeAgentService) {
        this.opencodeAgentService = opencodeAgentService;
    }

    @GetMapping("/api/opencode/agents")
    public OpencodeAgentsResponse listAgents() {
        return new OpencodeAgentsResponse(opencodeAgentService.listAgents());
    }

    public record OpencodeAgentsResponse(List<OpencodeAgentDto> opencodeAgents) {
    }
}
