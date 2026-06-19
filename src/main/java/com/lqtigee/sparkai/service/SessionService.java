package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.adapter.CodexAdapter;
import com.lqtigee.sparkai.adapter.AgentAdapter;
import com.lqtigee.sparkai.adapter.OpencodeAdapter;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
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
        List<RemoteSessionDto> codexSessions = discoverSessions(codexAdapter, ErrorCode.CODEX_SESSION_SCAN_FAILED);
        List<RemoteSessionDto> opencodeSessions = discoverSessions(opencodeAdapter, ErrorCode.OPENCODE_SESSION_SCAN_FAILED);

        List<RemoteSessionDto> sessions = new ArrayList<>(codexSessions.size() + opencodeSessions.size());
        sessions.addAll(codexSessions);
        sessions.addAll(opencodeSessions);
        return List.copyOf(sessions);
    }

    public List<RemoteSessionDto> listBySource(AgentSource source) {
        return switch (source) {
            case CODEX -> codexAdapter.discoverSessions();
            case OPENCODE -> opencodeAdapter.discoverSessions();
        };
    }

    public RemoteSessionDto getRequiredSession(AgentSource source, String id) {
        throw new UnsupportedOperationException("Session lookup is not implemented yet");
    }

    private List<RemoteSessionDto> discoverSessions(AgentAdapter adapter, ErrorCode fallbackCode) {
        try {
            return adapter.discoverSessions();
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ApiException(
                    fallbackCode,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Session discovery failed",
                    exception.getMessage()
            );
        }
    }
}
