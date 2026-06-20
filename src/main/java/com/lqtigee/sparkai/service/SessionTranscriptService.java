package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.codex.CodexTranscriptReader;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionMessageDto;
import com.lqtigee.sparkai.dto.SessionTranscriptDto;
import com.lqtigee.sparkai.opencode.OpencodeSqliteTranscriptReader;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SessionTranscriptService {

    private final SessionService sessionService;
    private final CodexTranscriptReader codexTranscriptReader;
    private final OpencodeSqliteTranscriptReader opencodeTranscriptReader;

    public SessionTranscriptService(
            SessionService sessionService,
            CodexTranscriptReader codexTranscriptReader,
            OpencodeSqliteTranscriptReader opencodeTranscriptReader
    ) {
        this.sessionService = sessionService;
        this.codexTranscriptReader = codexTranscriptReader;
        this.opencodeTranscriptReader = opencodeTranscriptReader;
    }

    public SessionTranscriptDto getTranscript(AgentSource source, String id) {
        RemoteSessionDto session = sessionService.getRequiredSession(source, id);
        List<SessionMessageDto> messages = switch (source) {
            case CODEX -> codexTranscriptReader.readMessages(Path.of(session.rawFile()));
            case OPENCODE -> opencodeTranscriptReader.readMessages(Path.of(session.rawFile()), session.id());
        };

        return new SessionTranscriptDto(session, messages);
    }
}
