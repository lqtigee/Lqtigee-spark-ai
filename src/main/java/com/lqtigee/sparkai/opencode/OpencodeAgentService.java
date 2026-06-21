package com.lqtigee.sparkai.opencode;

import com.lqtigee.sparkai.dto.OpencodeAgentDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OpencodeAgentService {

    private static final List<String> AGENT_LIST_COMMAND = List.of("opencode", "agent", "list");
    private static final Pattern AGENT_HEADER_PATTERN = Pattern.compile("^(.+?) \\(([^()]+)\\)$");

    private final CommandRunner commandRunner;

    public OpencodeAgentService() {
        this(new ProcessBuilderCommandRunner());
    }

    OpencodeAgentService(CommandRunner commandRunner) {
        this.commandRunner = commandRunner;
    }

    public List<OpencodeAgentDto> listAgents() {
        CommandResult result;
        try {
            result = commandRunner.run(AGENT_LIST_COMMAND);
        } catch (IOException exception) {
            throw agentListFailed(exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw agentListFailed("opencode agent list was interrupted");
        }

        if (result.exitCode() != 0) {
            throw agentListFailed("exitCode=" + result.exitCode());
        }

        return parseAgents(result.stdout());
    }

    private List<OpencodeAgentDto> parseAgents(String stdout) {
        List<OpencodeAgentDto> agents = new ArrayList<>();
        boolean sawOutput = false;
        for (String line : stdout.lines().toList()) {
            if (!line.isBlank()) {
                sawOutput = true;
            }
            Matcher matcher = AGENT_HEADER_PATTERN.matcher(line);
            if (matcher.matches()) {
                String name = matcher.group(1).trim();
                String source = matcher.group(2).trim();
                if (name.isBlank() || source.isBlank()) {
                    throw agentOutputInvalid();
                }
                agents.add(new OpencodeAgentDto(name, name, source));
            }
        }

        if (sawOutput && agents.isEmpty()) {
            throw agentOutputInvalid();
        }
        return List.copyOf(agents);
    }

    private ApiException agentListFailed(String detail) {
        return new ApiException(
                ErrorCode.OPENCODE_AGENT_LIST_FAILED,
                HttpStatus.FAILED_DEPENDENCY,
                "opencode agent list failed",
                detail
        );
    }

    private ApiException agentOutputInvalid() {
        return new ApiException(
                ErrorCode.OPENCODE_AGENT_OUTPUT_INVALID,
                HttpStatus.FAILED_DEPENDENCY,
                "opencode agent list output is invalid",
                "expected agent header lines like '<name> (<source>)'"
        );
    }

    interface CommandRunner {
        CommandResult run(List<String> command) throws IOException, InterruptedException;
    }

    record CommandResult(int exitCode, String stdout, String stderr) {
    }

    private static class ProcessBuilderCommandRunner implements CommandRunner {

        @Override
        public CommandResult run(List<String> command) throws IOException, InterruptedException {
            Process process = new ProcessBuilder(command).start();
            CompletableFuture<String> stdout = readAsync(process.getInputStream());
            CompletableFuture<String> stderr = readAsync(process.getErrorStream());
            int exitCode = process.waitFor();
            return new CommandResult(exitCode, join(stdout), join(stderr));
        }

        private static CompletableFuture<String> readAsync(InputStream stream) {
            return CompletableFuture.supplyAsync(() -> {
                try (InputStream input = stream) {
                    return new String(input.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException exception) {
                    throw new CompletionException(exception);
                }
            });
        }

        private static String join(CompletableFuture<String> output) throws IOException {
            try {
                return output.join();
            } catch (CompletionException exception) {
                if (exception.getCause() instanceof IOException ioException) {
                    throw ioException;
                }
                throw exception;
            }
        }
    }
}
