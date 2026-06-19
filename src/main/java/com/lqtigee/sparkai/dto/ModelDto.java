package com.lqtigee.sparkai.dto;

import java.util.List;

public record ModelDto(
        String id,
        String label,
        String commandModelName,
        List<AgentSource> sources,
        boolean enabled
) {
}
