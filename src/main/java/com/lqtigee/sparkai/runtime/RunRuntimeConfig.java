package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.config.DatabaseProperties;
import com.lqtigee.sparkai.config.RemoteProperties;
import com.lqtigee.sparkai.persistence.PostgresConnectionFactory;
import com.lqtigee.sparkai.persistence.RunRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lqtigee.sparkai.service.ModelService;
import com.lqtigee.sparkai.service.AttachmentService;
import com.lqtigee.sparkai.service.CapabilityService;
import com.lqtigee.sparkai.service.RunService;
import com.lqtigee.sparkai.service.SessionActionService;
import com.lqtigee.sparkai.service.SessionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
@EnableConfigurationProperties({RemoteProperties.class, DatabaseProperties.class})
public class RunRuntimeConfig {

    @Bean
    public CodexCommandBuilder codexCommandBuilder(AttachmentService attachmentService) {
        return new CodexCommandBuilder(attachmentService);
    }

    @Bean
    public OpencodeCommandBuilder opencodeCommandBuilder(AttachmentService attachmentService) {
        return new OpencodeCommandBuilder(attachmentService);
    }

    @Bean
    public CodexSessionActionCommandBuilder codexSessionActionCommandBuilder() {
        return new CodexSessionActionCommandBuilder();
    }

    @Bean
    public OpencodeSessionActionCommandBuilder opencodeSessionActionCommandBuilder() {
        return new OpencodeSessionActionCommandBuilder();
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
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .findModulesViaServiceLoader(true)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.build();
    }

    @Bean
    public VscodeIpcClient vscodeIpcClient(ObjectMapper objectMapper) {
        return new VscodeIpcClient(objectMapper);
    }

    @Bean
    public VscodeCodexSessionTracker vscodeCodexSessionTracker(
            ObjectMapper objectMapper,
            VscodeIpcClient vscodeIpcClient
    ) {
        return new VscodeCodexSessionTracker(objectMapper, vscodeIpcClient);
    }

    @Bean
    public VscodeCodexRunBridge vscodeCodexRunBridge(
            ObjectMapper objectMapper,
            VscodeIpcClient vscodeIpcClient,
            VscodeCodexSessionTracker vscodeCodexSessionTracker,
            RunEventBus runEventBus,
            RunRegistry runRegistry
    ) {
        return new VscodeCodexRunBridge(
                objectMapper,
                vscodeIpcClient,
                vscodeCodexSessionTracker,
                runEventBus,
                runRegistry
        );
    }

    @Bean
    public PostgresConnectionFactory postgresConnectionFactory(DatabaseProperties databaseProperties) {
        return new PostgresConnectionFactory(databaseProperties);
    }

    @Bean
    public RunRecordRepository runRecordRepository(PostgresConnectionFactory connectionFactory) {
        return new RunRecordRepository(connectionFactory);
    }

    @Bean
    public ProcessOutputPump processOutputPump(
            RunEventBus runEventBus,
            RunRegistry runRegistry,
            RunRecordRepository runRecordRepository
    ) {
        return new ProcessOutputPump(runEventBus, runRegistry, runRecordRepository);
    }

    @Bean
    public RunService runService(
            SessionService sessionService,
            ModelService modelService,
            OpencodeCommandBuilder opencodeCommandBuilder,
            ProcessLauncher processLauncher,
            ProcessOutputPump processOutputPump,
            RunEventBus runEventBus,
            RunRegistry runRegistry,
            RemoteProperties remoteProperties,
            RunRecordRepository runRecordRepository,
            VscodeCodexRunBridge vscodeCodexRunBridge
    ) {
        return new RunService(
                sessionService,
                modelService,
                opencodeCommandBuilder,
                processLauncher,
                processOutputPump,
                runEventBus,
                runRegistry,
                remoteProperties,
                runRecordRepository,
                vscodeCodexRunBridge
        );
    }

    @Bean
    public SessionActionService sessionActionService(
            SessionService sessionService,
            CapabilityService capabilityService,
            CodexSessionActionCommandBuilder codexSessionActionCommandBuilder,
            OpencodeSessionActionCommandBuilder opencodeSessionActionCommandBuilder,
            ProcessLauncher processLauncher
    ) {
        return new SessionActionService(
                sessionService,
                capabilityService,
                codexSessionActionCommandBuilder,
                opencodeSessionActionCommandBuilder,
                processLauncher
        );
    }
}
