package com.lqtigee.sparkai.opencode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpencodeAgentServiceTest {

    @Test
    void listAgentsRunsRealCliCommandAsArgumentArray() {
        CapturingRunner runner = new CapturingRunner(0, "build (primary)\n  []\n", "");
        OpencodeAgentService service = new OpencodeAgentService(runner);

        service.listAgents();

        assertThat(runner.commands()).containsExactly(List.of("opencode", "agent", "list"));
    }

    @Test
    void listAgentsParsesRealOpencodeAgentHeaders() {
        OpencodeAgentService service = new OpencodeAgentService(new CapturingRunner(0, """
                build (primary)
                  [
                    {
                      "permission": "*",
                      "action": "allow",
                      "pattern": "*"
                    }
                  ]
                explore (subagent)
                  []
                """, ""));

        assertThat(service.listAgents())
                .extracting("id", "name", "source")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("build", "build", "primary"),
                        org.assertj.core.groups.Tuple.tuple("explore", "explore", "subagent")
                );
    }

    @Test
    void listAgentsAllowsEmptySuccessOnlyWhenCommandOutputIsEmpty() {
        OpencodeAgentService service = new OpencodeAgentService(new CapturingRunner(0, "", ""));

        assertThat(service.listAgents()).isEmpty();
    }

    @Test
    void listAgentsFailsWhenCommandExitsNonZero() {
        OpencodeAgentService service = new OpencodeAgentService(new CapturingRunner(2, "", "failure"));

        assertThatThrownBy(service::listAgents)
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.OPENCODE_AGENT_LIST_FAILED);
    }

    @Test
    void listAgentsFailsWhenOutputHasNoAgentHeaders() {
        OpencodeAgentService service = new OpencodeAgentService(new CapturingRunner(0, "unexpected output\n", ""));

        assertThatThrownBy(service::listAgents)
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.OPENCODE_AGENT_OUTPUT_INVALID);
    }

    @Test
    void listAgentsFailsWhenCommandCannotStart() {
        OpencodeAgentService service = new OpencodeAgentService(command -> {
            throw new IOException("opencode missing");
        });

        assertThatThrownBy(service::listAgents)
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.OPENCODE_AGENT_LIST_FAILED);
    }

    private static class CapturingRunner implements OpencodeAgentService.CommandRunner {

        private final List<List<String>> commands = new ArrayList<>();
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        private CapturingRunner(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        @Override
        public OpencodeAgentService.CommandResult run(List<String> command) {
            commands.add(List.copyOf(command));
            return new OpencodeAgentService.CommandResult(exitCode, stdout, stderr);
        }

        private List<List<String>> commands() {
            return commands;
        }
    }
}
