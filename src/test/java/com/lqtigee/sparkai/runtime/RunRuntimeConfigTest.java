package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.lqtigee.sparkai.config.DatabaseProperties;
import com.lqtigee.sparkai.config.RemoteProperties;
import com.lqtigee.sparkai.persistence.PostgresConnectionFactory;
import com.lqtigee.sparkai.persistence.RunRecordRepository;
import com.lqtigee.sparkai.service.AttachmentService;
import com.lqtigee.sparkai.service.ModelService;
import com.lqtigee.sparkai.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RunRuntimeConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RunRuntimeConfig.class)
            .withBean(AttachmentService.class, () -> mock(AttachmentService.class))
            .withBean(SessionService.class, () -> mock(SessionService.class))
            .withBean(ModelService.class, () -> mock(ModelService.class))
            .withPropertyValues(
                    "lqtigee.remote.max-prompt-chars=8000",
                    "lqtigee.database.enabled=true",
                    "lqtigee.database.url=jdbc:postgresql://127.0.0.1:5432/lqtigee",
                    "lqtigee.database.username=lqtigee",
                    "lqtigee.database.password=secret"
            );

    @Test
    void contextResolvesRunRecordRepositoryWithoutOpeningPostgres() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DatabaseProperties.class);
            assertThat(context).hasSingleBean(PostgresConnectionFactory.class);
            assertThat(context).hasSingleBean(RunRecordRepository.class);
            assertThat(context).hasSingleBean(RemoteProperties.class);

            DatabaseProperties databaseProperties = context.getBean(DatabaseProperties.class);
            assertThat(databaseProperties.isEnabled()).isTrue();
            assertThat(databaseProperties.getUrl()).isEqualTo("jdbc:postgresql://127.0.0.1:5432/lqtigee");
            assertThat(databaseProperties.getUsername()).isEqualTo("lqtigee");
            assertThat(databaseProperties.getPassword()).isEqualTo("secret");
        });
    }
}
