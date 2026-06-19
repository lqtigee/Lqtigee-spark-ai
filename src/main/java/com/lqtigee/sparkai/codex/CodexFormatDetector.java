package com.lqtigee.sparkai.codex;

import com.lqtigee.sparkai.dto.SessionFileFormat;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.HttpStatus;

public class CodexFormatDetector {

    public SessionFileFormat detect(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            int nonEmptyLineCount = 0;
            boolean allSampleLinesStartWithJsonObject = true;
            String firstNonEmptyLine = null;
            String line;

            while ((line = reader.readLine()) != null && nonEmptyLineCount < 3) {
                String trimmedLine = line.stripLeading();
                if (!trimmedLine.isEmpty()) {
                    if (firstNonEmptyLine == null) {
                        firstNonEmptyLine = trimmedLine;
                    }
                    nonEmptyLineCount++;
                    allSampleLinesStartWithJsonObject =
                            allSampleLinesStartWithJsonObject && trimmedLine.startsWith("{");
                }
            }

            if (nonEmptyLineCount > 1 && allSampleLinesStartWithJsonObject) {
                return SessionFileFormat.JSONL;
            }
            if (firstNonEmptyLine != null && firstNonEmptyLine.startsWith("{")) {
                return SessionFileFormat.JSON;
            }
            return SessionFileFormat.UNKNOWN;
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.CODEX_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Codex format detection failed",
                    exception.getMessage()
            );
        }
    }
}
