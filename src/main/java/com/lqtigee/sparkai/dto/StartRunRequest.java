package com.lqtigee.sparkai.dto;

public record StartRunRequest(
        String sessionId,
        AgentSource source,
        String modelId,
        CommandMode mode,
        String prompt,
        boolean confirmDangerous,
        CodexRunOptionsDto codexOptions,
        OpencodeRunOptionsDto opencodeOptions
) {
    public StartRunRequest(
            String sessionId,
            AgentSource source,
            String modelId,
            CommandMode mode,
            String prompt,
            boolean confirmDangerous,
            CodexRunOptionsDto codexOptions
    ) {
        this(sessionId, source, modelId, mode, prompt, confirmDangerous, codexOptions, null);
    }

    public StartRunRequest(
            String sessionId,
            AgentSource source,
            String modelId,
            CommandMode mode,
            String prompt,
            boolean confirmDangerous
    ) {
        this(sessionId, source, modelId, mode, prompt, confirmDangerous, null, null);
    }
}
