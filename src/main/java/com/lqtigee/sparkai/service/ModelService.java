package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.config.ModelProperties;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.ModelDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(ModelProperties.class)
public class ModelService {

    private final ModelProperties modelProperties;

    public ModelService(ModelProperties modelProperties) {
        this.modelProperties = modelProperties;
        this.modelProperties.validate();
    }

    public List<ModelDto> listModels() {
        return modelProperties.getEntries().stream()
                .map(this::toDto)
                .toList();
    }

    public ModelDto getRequiredModel(String id) {
        return listModels().stream()
                .filter(model -> model.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        ErrorCode.MODEL_NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Model not found",
                        id
                ));
    }

    public void validateModelForSource(String id, AgentSource source) {
        ModelDto model = getRequiredModel(id);
        if (!model.enabled()) {
            throw new ApiException(
                    ErrorCode.MODEL_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "Model not found",
                    id
            );
        }
        if (!model.sources().contains(source)) {
            throw new ApiException(
                    ErrorCode.MODEL_SOURCE_UNSUPPORTED,
                    HttpStatus.BAD_REQUEST,
                    "Model does not support requested source",
                    source == null ? null : source.name()
            );
        }
    }

    private ModelDto toDto(ModelProperties.Entry entry) {
        return new ModelDto(
                entry.getId(),
                entry.getLabel(),
                entry.getCommandModelName(),
                List.copyOf(entry.getSources()),
                entry.isEnabled()
        );
    }
}
