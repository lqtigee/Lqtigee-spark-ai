package com.lqtigee.sparkai.adapter;

import com.lqtigee.sparkai.dto.AdapterHealthDto;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.error.ErrorCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OpencodeAdapter implements AgentAdapter {

    private static final Path OPENCODE_BIN = Path.of("/home/lqtiger/.opencode/bin/opencode");
    private static final Path OPENCODE_DB = Path.of("/home/lqtiger/.local/share/opencode/opencode.db");
    private static final String DISCOVERED_VERSION = "1.17.8";

    @Override
    public AgentSource source() {
        return AgentSource.OPENCODE;
    }

    @Override
    public AdapterHealthDto probe() {
        if (!Files.isExecutable(OPENCODE_BIN)) {
            return unavailable(ErrorCode.OPENCODE_BIN_NOT_FOUND, "opencode executable is not available");
        }
        if (!Files.isReadable(OPENCODE_DB)) {
            return unavailable(ErrorCode.OPENCODE_CONFIG_NOT_FOUND, "opencode SQLite database is not readable");
        }
        return new AdapterHealthDto(
                AgentSource.OPENCODE,
                true,
                "OK",
                DISCOVERED_VERSION,
                null,
                null
        );
    }

    @Override
    public List<RemoteSessionDto> discoverSessions() {
        throw new UnsupportedOperationException("Opencode session discovery is not implemented yet");
    }

    private AdapterHealthDto unavailable(ErrorCode errorCode, String message) {
        return new AdapterHealthDto(
                AgentSource.OPENCODE,
                false,
                "UNAVAILABLE",
                null,
                errorCode.name(),
                message
        );
    }
}
