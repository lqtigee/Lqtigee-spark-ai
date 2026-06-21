package com.lqtigee.sparkai.web;

import com.lqtigee.sparkai.adapter.CodexAdapter;
import com.lqtigee.sparkai.adapter.OpencodeAdapter;
import com.lqtigee.sparkai.dto.AdapterHealthDto;
import com.lqtigee.sparkai.dto.HealthDto;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final CodexAdapter codexAdapter;
    private final OpencodeAdapter opencodeAdapter;

    public HealthController(CodexAdapter codexAdapter, OpencodeAdapter opencodeAdapter) {
        this.codexAdapter = codexAdapter;
        this.opencodeAdapter = opencodeAdapter;
    }

    @GetMapping("/api/health")
    public HealthDto health() {
        List<AdapterHealthDto> adapters = List.of(codexAdapter.probe(), opencodeAdapter.probe());
        return new HealthDto(
                "Lqtigee-spark-ai",
                "Lqtigee",
                20261,
                aggregateStatus(adapters),
                Instant.now(),
                adapters
        );
    }

    private String aggregateStatus(List<AdapterHealthDto> adapters) {
        long availableCount = adapters.stream()
                .filter(AdapterHealthDto::available)
                .count();
        if (availableCount == adapters.size()) {
            return "OK";
        }
        if (availableCount > 0) {
            return "DEGRADED";
        }
        return "FAILED";
    }
}
