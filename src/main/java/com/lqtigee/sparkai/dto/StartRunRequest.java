package com.lqtigee.sparkai.dto;

public record StartRunRequest(
        String sessionId,
        AgentSource source,
        String modelId,
        CommandMode mode,
        String prompt,
        boolean confirmDangerous,
        CodexRunOptionsDto codexOptions
) {
    public StartRunRequest(
            String sessionId,
            AgentSource source,
            String modelId,
            CommandMode mode,
            String prompt,
            boolean confirmDangerous
    ) {
        this(sessionId, source, modelId, mode, prompt, confirmDangerous, null);
    }
}
