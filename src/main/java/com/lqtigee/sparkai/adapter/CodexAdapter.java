package com.lqtigee.sparkai.adapter;

import com.lqtigee.sparkai.codex.CodexFileScanner;
import com.lqtigee.sparkai.codex.CodexJsonlParser;
import com.lqtigee.sparkai.dto.AdapterHealthDto;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.error.ErrorCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CodexAdapter implements AgentAdapter {

    private static final Path CODEX_BIN = Path.of("/home/lqtiger/.npm-global/bin/codex");
    private static final Path CODEX_HOME = Path.of("/home/lqtiger/.codex");
    private static final String DISCOVERED_VERSION = "codex-cli 0.141.0";

    private final CodexFileScanner scanner;
    private final CodexJsonlParser parser;

    public CodexAdapter() {
        this(new CodexFileScanner(), new CodexJsonlParser());
    }

    CodexAdapter(CodexFileScanner scanner, CodexJsonlParser parser) {
        this.scanner = scanner;
        this.parser = parser;
    }

    @Override
    public AgentSource source() {
        return AgentSource.CODEX;
    }

    @Override
    public AdapterHealthDto probe() {
        if (!Files.isExecutable(CODEX_BIN)) {
            return unavailable(ErrorCode.CODEX_BIN_NOT_FOUND, "Codex executable is not available");
        }
        if (!Files.isReadable(CODEX_HOME)) {
            return unavailable(ErrorCode.CODEX_HOME_NOT_FOUND, "Codex home is not readable");
        }
        return new AdapterHealthDto(
                AgentSource.CODEX,
                true,
                "OK",
                DISCOVERED_VERSION,
                null,
                null
        );
    }

    @Override
    public List<RemoteSessionDto> discoverSessions() {
        return scanner.scan(CODEX_HOME).stream()
                .map(path -> parser.parse(path))
                .toList();
    }

    private AdapterHealthDto unavailable(ErrorCode errorCode, String message) {
        return new AdapterHealthDto(
                AgentSource.CODEX,
                false,
                "UNAVAILABLE",
                null,
                errorCode.name(),
                message
        );
    }
}
