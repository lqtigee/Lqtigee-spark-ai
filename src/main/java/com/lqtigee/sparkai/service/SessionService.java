package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.adapter.CodexAdapter;
import com.lqtigee.sparkai.adapter.OpencodeAdapter;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private final CodexAdapter codexAdapter;
    private final OpencodeAdapter opencodeAdapter;

    public SessionService(CodexAdapter codexAdapter, OpencodeAdapter opencodeAdapter) {
        this.codexAdapter = codexAdapter;
        this.opencodeAdapter = opencodeAdapter;
    }

    public List<RemoteSessionDto> listAllSessions() {
        throw new UnsupportedOperationException("Session listing is not implemented yet");
    }

    public List<RemoteSessionDto> listBySource(AgentSource source) {
        throw new UnsupportedOperationException("Session source routing is not implemented yet");
    }

    public RemoteSessionDto getRequiredSession(AgentSource source, String id) {
        throw new UnsupportedOperationException("Session lookup is not implemented yet");
    }
}
