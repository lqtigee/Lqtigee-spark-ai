package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.config.DatabaseProperties;
import com.lqtigee.sparkai.config.RemoteProperties;
import com.lqtigee.sparkai.persistence.PostgresConnectionFactory;
import com.lqtigee.sparkai.persistence.RunRecordRepository;
import com.lqtigee.sparkai.service.ModelService;
import com.lqtigee.sparkai.service.AttachmentService;
import com.lqtigee.sparkai.service.RunService;
import com.lqtigee.sparkai.service.SessionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            CodexCommandBuilder codexCommandBuilder,
            OpencodeCommandBuilder opencodeCommandBuilder,
            ProcessLauncher processLauncher,
            ProcessOutputPump processOutputPump,
            RunEventBus runEventBus,
            RunRegistry runRegistry,
            RemoteProperties remoteProperties,
            RunRecordRepository runRecordRepository
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
                remoteProperties,
                runRecordRepository
        );
    }
}
