package com.lqtigee.sparkai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.ModelDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ModelServiceTest {

    @Autowired
    private ModelService modelService;

    @Test
    void listModelsReturnsConfiguredModels() {
        List<ModelDto> models = modelService.listModels();

        assertThat(models)
                .extracting(ModelDto::id)
                .containsExactly("gpt-5.5", "openai/Lqtigee");
    }

    @Test
    void getRequiredModelFailsWhenIdIsMissing() {
        assertThatThrownBy(() -> modelService.getRequiredModel("missing-model"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.MODEL_NOT_FOUND));
    }

    @Test
    void validateModelForSourceFailsWhenSourceIsUnsupported() {
        assertThatThrownBy(() -> modelService.validateModelForSource("gpt-5.5", AgentSource.OPENCODE))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.MODEL_SOURCE_UNSUPPORTED));
    }
}
