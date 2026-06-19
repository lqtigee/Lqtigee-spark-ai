# Related Process Discovery

Command:

```bash
ps -eo pid,etimes,cmd | awk 'BEGIN{IGNORECASE=1} /codex|opencode/ && !/awk/ {print}'
```

Observed related processes:

```text
java -jar target/codex-java-gateway-0.0.1-SNAPSHOT.jar
opencode
node /home/lqtiger/.npm-global/bin/codex
codex app-server
java -jar target/codex-java-gateway-0.0.1-SNAPSHOT.jar --server.port=19012
```

Conclusion:

```text
Both Codex-related and opencode-related processes were present during discovery.
```

Implementation note:

- Process inspection may help health display, but session discovery should use persisted Codex JSONL and opencode SQLite first.

