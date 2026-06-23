package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.adapter.CodexAdapter;
import com.lqtigee.sparkai.adapter.AgentAdapter;
import com.lqtigee.sparkai.adapter.OpencodeAdapter;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionRefreshRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

    public List<RemoteSessionDto> refreshSessions(SessionRefreshRequest request) {
        validateRefreshRequest(request);
        Map<AgentSource, Set<String>> idsBySource = request.refs().stream()
                .collect(Collectors.groupingBy(
                        SessionRefreshRequest.SessionRefDto::source,
                        LinkedHashMap::new,
                        Collectors.mapping(SessionRefreshRequest.SessionRefDto::id, Collectors.toSet())
                ));

        List<RemoteSessionDto> refreshedSessions = new ArrayList<>();
        for (Map.Entry<AgentSource, Set<String>> entry : idsBySource.entrySet()) {
            Set<String> requestedIds = entry.getValue();
            listBySource(entry.getKey()).stream()
                    .filter(session -> requestedIds.contains(session.id()))
                    .forEach(refreshedSessions::add);
        }
        return List.copyOf(refreshedSessions);
    }

    public RemoteSessionDto getRequiredSession(AgentSource source, String id) {
        return listBySource(source).stream()
                .filter(session -> session.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        ErrorCode.SESSION_NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Session not found",
                        id
                ));
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

    private void validateRefreshRequest(SessionRefreshRequest request) {
        if (request == null || request.refs() == null) {
            throw validationFailed("refs");
        }
        for (SessionRefreshRequest.SessionRefDto ref : request.refs()) {
            if (ref == null || ref.source() == null || ref.id() == null || ref.id().isBlank()) {
                throw validationFailed("refs");
            }
        }
    }

    private ApiException validationFailed(String detail) {
        return new ApiException(
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                detail
        );
    }
}
