package com.lqtigee.sparkai.dto;

import java.util.List;

public record SessionRefreshRequest(
        List<SessionRefDto> refs
) {
    public record SessionRefDto(
            AgentSource source,
            String id
    ) {
    }
}
