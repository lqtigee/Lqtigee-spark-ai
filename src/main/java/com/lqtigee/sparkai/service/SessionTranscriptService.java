package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.codex.CodexTranscriptReader;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionTranscriptDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.opencode.OpencodeSqliteTranscriptReader;
import java.nio.file.Path;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SessionTranscriptService {

    private static final int DEFAULT_TRANSCRIPT_LIMIT = 10;
    private static final int MAX_TRANSCRIPT_LIMIT = 100;

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
        return getTranscript(source, id, DEFAULT_TRANSCRIPT_LIMIT, null);
    }

    public SessionTranscriptDto getTranscript(AgentSource source, String id, Integer limit, String before) {
        RemoteSessionDto session = sessionService.getRequiredSession(source, id);
        int requestedLimit = validateLimit(limit);
        return switch (source) {
            case CODEX -> {
                CodexTranscriptReader.CodexTranscriptPage page = codexTranscriptReader.readPage(
                        Path.of(session.rawFile()),
                        requestedLimit,
                        before
                );
                yield new SessionTranscriptDto(session, page.messages(), page.pageInfo());
            }
            case OPENCODE -> {
                OpencodeSqliteTranscriptReader.OpencodeTranscriptPage page = opencodeTranscriptReader.readPage(
                        Path.of(session.rawFile()),
                        session.id(),
                        requestedLimit,
                        before
                );
                yield new SessionTranscriptDto(session, page.messages(), page.pageInfo());
            }
        };
    }

    private int validateLimit(Integer limit) {
        int requestedLimit = limit == null ? DEFAULT_TRANSCRIPT_LIMIT : limit;
        if (requestedLimit < 1) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "Transcript limit must be positive",
                    "limit"
            );
        }
        if (requestedLimit > MAX_TRANSCRIPT_LIMIT) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "Transcript limit is too large",
                    "limit"
            );
        }
        return requestedLimit;
    }
}
