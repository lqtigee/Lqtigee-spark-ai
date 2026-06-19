package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.dto.AgentSource;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record CommandSpec(
        List<String> command,
        Path workdir,
        Map<String, String> environment,
        AgentSource source,
        String sessionId,
        String modelId
) {
}
