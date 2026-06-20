package com.lqtigee.sparkai.web;

import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.dto.StartRunResponse;
import com.lqtigee.sparkai.dto.StopRunResponse;
import com.lqtigee.sparkai.service.RunService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @PostMapping("/api/runs")
    public StartRunResponse start(@RequestBody StartRunRequest request) {
        return runService.start(request);
    }

    @GetMapping(value = "/api/runs/{runId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String runId) {
        return runService.events(runId);
    }

    @PostMapping("/api/runs/{runId}/stop")
    public StopRunResponse stop(@PathVariable String runId) {
        return runService.stop(runId);
    }
}
