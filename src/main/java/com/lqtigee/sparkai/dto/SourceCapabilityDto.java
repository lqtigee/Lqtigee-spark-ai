package com.lqtigee.sparkai.dto;

import java.util.List;

public record SourceCapabilityDto(
        AgentSource source,
        List<String> runOptions,
        List<String> attachments,
        List<String> sessionActions,
        List<String> dangerousOptions
) {
    public SourceCapabilityDto {
        runOptions = List.copyOf(runOptions);
        attachments = List.copyOf(attachments);
        sessionActions = List.copyOf(sessionActions);
        dangerousOptions = List.copyOf(dangerousOptions);
    }
}
