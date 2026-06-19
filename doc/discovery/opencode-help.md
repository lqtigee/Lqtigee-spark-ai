# opencode Help Discovery

Ticket: `P0-M013`

Commands:

```bash
/home/lqtiger/.opencode/bin/opencode --help
/home/lqtiger/.opencode/bin/opencode run --help
```

Observed top-level commands:

```text
opencode run [message..]
opencode export [sessionID]
opencode session
opencode db
opencode serve
opencode web
```

Observed `run` options:

```text
opencode run [message..]
--session <session id>
--continue
--fork
--model <provider/model>
--format default|json
--dir <directory>
--agent <agent>
--file <file>
--dangerously-skip-permissions
```

Confirmed command-builder shape for future ticket:

```text
opencode run --format json --model <provider/model> --dir <workspace> --session <sessionId> <message>
```

Dangerous mode note:

- `--dangerously-skip-permissions` exists and must require `confirmDangerous`.

Risk:

- Prompt/message must be passed as an argument array entry, not shell-concatenated.

