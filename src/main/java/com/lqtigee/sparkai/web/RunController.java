package com.lqtigee.sparkai.web;

import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.dto.StartRunResponse;
import com.lqtigee.sparkai.service.RunService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
