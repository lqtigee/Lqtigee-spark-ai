package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.config.ModelProperties;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.ModelDto;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
        throw new UnsupportedOperationException("Model lookup is not implemented yet");
    }

    public void validateModelForSource(String id, AgentSource source) {
        throw new UnsupportedOperationException("Model source validation is not implemented yet");
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
