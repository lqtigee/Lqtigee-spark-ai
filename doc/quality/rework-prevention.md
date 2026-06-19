# Rework Prevention Protocol

This protocol exists to stop future AI agents from repairing symptoms in the wrong layer.

## 1. Layer Ownership

| Failure appears in | First layer to inspect | Layers forbidden until proven necessary |
| --- | --- | --- |
| Empty session list | scanner/parser | frontend UI, command builder |
| Wrong session fields | parser | runtime, frontend |
| Model disabled/missing | model config/service | frontend UI |
| Start run rejected | request validation/model/session service | process launcher |
| Process exits non-zero | command builder/process pump | frontend UI |
| SSE missing event | event bus/process pump | parser/model service |
| Mobile layout broken | frontend component/CSS | backend |
| PWA stale data | service worker | backend parser |

## 2. Two-Failure Rule

If the same ticket fails verification twice:

1. Stop the ticket.
2. Write the exact failing command.
3. Write the exact failing output.
4. Identify the current layer.
5. Add a new discovery or bug micro ticket.
6. Do not keep editing the same files.

## 3. Runtime Static Evidence Failure Rule

If Codex or opencode command builder evidence is incomplete or inconsistent:

- Do not change frontend.
- Do not change parser.
- Do not change API DTOs.
- Add or update a static discovery document.
- Do not run live CLI commands as a gate.
- Update only command builder design and runtime tickets after static evidence is recorded.

## 4. Parser Failure Rule

If parser tests fail:

- Do not use fallback fields.
- Do not read logs as a replacement for a missing session source.
- Do not return an empty successful list for parse errors.
- Add a sanitized fixture showing the missing or changed structure.

## 5. Frontend Failure Rule

If frontend cannot render data:

- First compare frontend types to `doc/contracts/backend-api-contract.md`.
- Do not add temporary arrays.
- Do not hardcode models or sessions.
- Do not create a UI success state for API errors.

## 6. Bug Fix Rule

Every bug fix must be a micro ticket before code changes. The bug ticket must identify:

1. Symptom.
2. Expected behavior.
3. Actual behavior.
4. Failing verification.
5. Allowed files.
6. Smallest responsible layer.

No "cleanup while here" changes are allowed.
