package com.lqtigee.sparkai.adapter;

import com.lqtigee.sparkai.dto.AdapterHealthDto;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CodexAdapter implements AgentAdapter {

    @Override
    public AgentSource source() {
        return AgentSource.CODEX;
    }

    @Override
    public AdapterHealthDto probe() {
        throw new UnsupportedOperationException("Codex adapter probe is not implemented yet");
    }

    @Override
    public List<RemoteSessionDto> discoverSessions() {
        throw new UnsupportedOperationException("Codex session discovery is not implemented yet");
    }
}
