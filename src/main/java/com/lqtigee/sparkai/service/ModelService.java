package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.ModelDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ModelService {

    public List<ModelDto> listModels() {
        throw new UnsupportedOperationException("Model listing is not implemented yet");
    }

    public ModelDto getRequiredModel(String id) {
        throw new UnsupportedOperationException("Model lookup is not implemented yet");
    }

    public void validateModelForSource(String id, AgentSource source) {
        throw new UnsupportedOperationException("Model source validation is not implemented yet");
    }
}
