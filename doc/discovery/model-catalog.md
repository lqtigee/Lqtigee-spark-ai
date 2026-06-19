# Model Catalog Discovery

Ticket: `P0-M021`

Observed model evidence:

## Codex

Latest Codex session `turn_context`:

```text
payload.model = gpt-5.5
```

Codex config also contains local model catalog files, but this discovery did not copy config contents to avoid leaking tokens or provider configuration.

## opencode

Latest opencode session rows:

```text
session.model = {"id":"Lqtigee","providerID":"openai"}
session.model = {"id":"Lqtigee","providerID":"openai","variant":"default"}
```

opencode log also shows:

```text
providerID=openai
modelID=Lqtigee
agent=build
```

## Confirmed Initial Models

```text
id: gpt-5.5
label: GPT-5.5
source: CODEX
commandModelName: gpt-5.5
enabled: true
```

```text
id: openai/Lqtigee
label: Lqtigee
source: OPENCODE
commandModelName: openai/Lqtigee
enabled: true
```

## Caveat

The exact Codex model routing is local-router dependent. Do not run live commands as a gate. Command builder tests may assert that configured model ids are passed correctly; actual model/provider failure must surface as a typed runtime failure, not a fallback.
