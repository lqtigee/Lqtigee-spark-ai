package com.lqtigee.sparkai.web;

import com.lqtigee.sparkai.dto.ModelDto;
import com.lqtigee.sparkai.service.ModelService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping("/api/models")
    public ModelsResponse listModels() {
        return new ModelsResponse(modelService.listModels());
    }

    public record ModelsResponse(List<ModelDto> models) {
    }
}
