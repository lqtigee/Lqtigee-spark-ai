package com.lqtigee.sparkai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelPropertiesTest {

    @Test
    void validateFailsWhenModelListIsEmpty() {
        ModelProperties properties = new ModelProperties();

        assertThatThrownBy(properties::validate)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void validateFailsWhenModelIdsAreDuplicated() {
        ModelProperties properties = new ModelProperties();
        properties.setEntries(List.of(
                modelEntry("same-model"),
                modelEntry("same-model")
        ));

        assertThatThrownBy(properties::validate)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void validatePassesWhenOneModelIsPresent() {
        ModelProperties properties = new ModelProperties();
        properties.setEntries(List.of(modelEntry("test-model")));

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    private ModelProperties.Entry modelEntry(String id) {
        ModelProperties.Entry entry = new ModelProperties.Entry();
        entry.setId(id);
        entry.setLabel("Test Model");
        entry.setCommandModelName(id);
        entry.setSources(List.of(AgentSource.CODEX));
        entry.setEnabled(true);
        return entry;
    }
}
