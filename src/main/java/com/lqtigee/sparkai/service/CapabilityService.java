package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.SourceCapabilityDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CapabilityService {

    public List<SourceCapabilityDto> listCapabilities() {
        return List.of(
                new SourceCapabilityDto(
                        AgentSource.CODEX,
                        List.of("model"),
                        List.of("image"),
                        List.of("archive", "delete", "unarchive", "fork"),
                        List.of()
                ),
                new SourceCapabilityDto(
                        AgentSource.OPENCODE,
                        List.of("model", "agent", "fork", "share", "variant", "thinking", "replay", "replayLimit"),
                        List.of("file"),
                        List.of("delete", "export"),
                        List.of("shellDangerouslySkipPermissions")
                )
        );
    }
}
