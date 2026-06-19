package com.lqtigee.sparkai.adapter;

import com.lqtigee.sparkai.dto.AdapterHealthDto;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OpencodeAdapter implements AgentAdapter {

    @Override
    public AgentSource source() {
        return AgentSource.OPENCODE;
    }

    @Override
    public AdapterHealthDto probe() {
        throw new UnsupportedOperationException("Opencode adapter probe is not implemented yet");
    }

    @Override
    public List<RemoteSessionDto> discoverSessions() {
        throw new UnsupportedOperationException("Opencode session discovery is not implemented yet");
    }
}
