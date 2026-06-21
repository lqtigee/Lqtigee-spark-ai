package com.lqtigee.sparkai.web;

import com.lqtigee.sparkai.dto.SourceCapabilityDto;
import com.lqtigee.sparkai.service.CapabilityService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CapabilityController {

    private final CapabilityService capabilityService;

    public CapabilityController(CapabilityService capabilityService) {
        this.capabilityService = capabilityService;
    }

    @GetMapping("/api/capabilities")
    public CapabilitiesResponse listCapabilities() {
        return new CapabilitiesResponse(capabilityService.listCapabilities());
    }

    public record CapabilitiesResponse(List<SourceCapabilityDto> capabilities) {
    }
}
