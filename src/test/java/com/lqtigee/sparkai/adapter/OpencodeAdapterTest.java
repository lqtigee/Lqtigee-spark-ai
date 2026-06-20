package com.lqtigee.sparkai.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.opencode.OpencodeSqliteSessionReader;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class OpencodeAdapterTest {

    @Test
    void discoverSessionsPropagatesTypedReaderFailure() {
        ApiException failure = new ApiException(
                ErrorCode.OPENCODE_SESSION_SCAN_FAILED,
                HttpStatus.FAILED_DEPENDENCY,
                "Opencode SQLite database open failed",
                "read failed"
        );
        OpencodeAdapter adapter = new OpencodeAdapter(new FailingReader(failure));

        assertThatThrownBy(adapter::discoverSessions)
                .isSameAs(failure)
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(ErrorCode.OPENCODE_SESSION_SCAN_FAILED);
                    assertThat(exception.status()).isEqualTo(HttpStatus.FAILED_DEPENDENCY);
                });
    }

    private static class FailingReader extends OpencodeSqliteSessionReader {

        private final ApiException failure;

        private FailingReader(ApiException failure) {
            this.failure = failure;
        }

        @Override
        public List<RemoteSessionDto> readSessions(Path databasePath) {
            throw failure;
        }
    }
}
