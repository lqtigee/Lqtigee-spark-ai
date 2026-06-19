package com.lqtigee.sparkai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.adapter.CodexAdapter;
import com.lqtigee.sparkai.adapter.OpencodeAdapter;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionServiceTest {

    @Test
    void listAllSessionsFailsWhenCodexSucceedsAndOpencodeFails() {
        SessionService service = new SessionService(
                new EmptyCodexAdapter(),
                new FailingOpencodeAdapter()
        );

        assertThatThrownBy(service::listAllSessions)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.OPENCODE_SESSION_SCAN_FAILED));
    }

    private static class EmptyCodexAdapter extends CodexAdapter {

        @Override
        public List<RemoteSessionDto> discoverSessions() {
            return List.of();
        }
    }

    private static class FailingOpencodeAdapter extends OpencodeAdapter {

        @Override
        public List<RemoteSessionDto> discoverSessions() {
            throw new IllegalStateException("opencode discovery failed");
        }
    }
}
