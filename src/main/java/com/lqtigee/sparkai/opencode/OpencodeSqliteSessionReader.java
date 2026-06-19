package com.lqtigee.sparkai.opencode;

import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import org.springframework.http.HttpStatus;

public class OpencodeSqliteSessionReader {

    public List<RemoteSessionDto> readSessions(Path databasePath) {
        if (databasePath == null || !Files.exists(databasePath)) {
            throw new ApiException(
                    ErrorCode.OPENCODE_CONFIG_NOT_FOUND,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Opencode SQLite database not found",
                    databasePath == null ? "databasePath=null" : databasePath.toString()
            );
        }
        if (!Files.isReadable(databasePath)) {
            throw new ApiException(
                    ErrorCode.OPENCODE_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Opencode SQLite database is not readable",
                    databasePath.toString()
            );
        }

        try (Connection ignored = openReadOnly(databasePath)) {
            // Row queries are implemented by later parser tickets.
        } catch (SQLException exception) {
            throw new ApiException(
                    ErrorCode.OPENCODE_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Opencode SQLite database open failed",
                    exception.getMessage()
            );
        }

        throw new UnsupportedOperationException("Opencode SQLite session reading is not implemented yet");
    }

    private Connection openReadOnly(Path databasePath) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("open_mode", "1");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath(), properties);
        connection.setReadOnly(true);
        return connection;
    }
}
