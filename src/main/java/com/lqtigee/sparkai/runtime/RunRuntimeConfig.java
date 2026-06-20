package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.config.RemoteProperties;
import com.lqtigee.sparkai.service.ModelService;
import com.lqtigee.sparkai.service.RunService;
import com.lqtigee.sparkai.service.SessionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RemoteProperties.class)
public class RunRuntimeConfig {

    @Bean
    public CodexCommandBuilder codexCommandBuilder() {
        return new CodexCommandBuilder();
    }

    @Bean
    public OpencodeCommandBuilder opencodeCommandBuilder() {
        return new OpencodeCommandBuilder();
    }

    @Bean
    public ProcessLauncher processLauncher() {
        return new ProcessLauncher();
    }

    @Bean
    public RunEventBus runEventBus() {
        return new RunEventBus();
    }

    @Bean
    public RunRegistry runRegistry() {
        return new RunRegistry();
    }

    @Bean
    public ProcessOutputPump processOutputPump(RunEventBus runEventBus, RunRegistry runRegistry) {
        return new ProcessOutputPump(runEventBus, runRegistry);
    }

    @Bean
    public RunService runService(
            SessionService sessionService,
            ModelService modelService,
            CodexCommandBuilder codexCommandBuilder,
            OpencodeCommandBuilder opencodeCommandBuilder,
            ProcessLauncher processLauncher,
            ProcessOutputPump processOutputPump,
            RunEventBus runEventBus,
            RunRegistry runRegistry,
            RemoteProperties remoteProperties
    ) {
        return new RunService(
                sessionService,
                modelService,
                codexCommandBuilder,
                opencodeCommandBuilder,
                processLauncher,
                processOutputPump,
                runEventBus,
                runRegistry,
                remoteProperties
        );
    }
}
