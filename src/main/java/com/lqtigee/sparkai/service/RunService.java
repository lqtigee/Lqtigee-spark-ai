package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.dto.StartRunResponse;
import com.lqtigee.sparkai.dto.StopRunResponse;
import com.lqtigee.sparkai.runtime.CodexCommandBuilder;
import com.lqtigee.sparkai.runtime.OpencodeCommandBuilder;
import com.lqtigee.sparkai.runtime.ProcessLauncher;
import com.lqtigee.sparkai.runtime.ProcessOutputPump;
import com.lqtigee.sparkai.runtime.RunRegistry;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class RunService {

    private final SessionService sessionService;
    private final ModelService modelService;
    private final CodexCommandBuilder codexCommandBuilder;
    private final OpencodeCommandBuilder opencodeCommandBuilder;
    private final ProcessLauncher processLauncher;
    private final ProcessOutputPump processOutputPump;
    private final RunRegistry runRegistry;

    public RunService(
            SessionService sessionService,
            ModelService modelService,
            CodexCommandBuilder codexCommandBuilder,
            OpencodeCommandBuilder opencodeCommandBuilder,
            ProcessLauncher processLauncher,
            ProcessOutputPump processOutputPump,
            RunRegistry runRegistry
    ) {
        this.sessionService = sessionService;
        this.modelService = modelService;
        this.codexCommandBuilder = codexCommandBuilder;
        this.opencodeCommandBuilder = opencodeCommandBuilder;
        this.processLauncher = processLauncher;
        this.processOutputPump = processOutputPump;
        this.runRegistry = runRegistry;
    }

    public StartRunResponse start(StartRunRequest request) {
        throw new UnsupportedOperationException("Run start is not implemented yet");
    }

    public SseEmitter events(String runId) {
        throw new UnsupportedOperationException("Run events are not implemented yet");
    }

    public StopRunResponse stop(String runId) {
        throw new UnsupportedOperationException("Run stop is not implemented yet");
    }
}
