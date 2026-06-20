# Frontend No Mock Audit

Ticket: AUDIT-M002

Scope:

- `frontend/src`
- `frontend/public`

Search commands:

```bash
rg -n -i "mock|sample|demo|fake" frontend/src frontend/public
rg -n -i "gpt-|claude|gemini|llama|openai/|anthropic/|model-[a-z0-9]|run_\d|ses_" frontend/src frontend/public
rg -n "const .*models|models: \[|sessions: \[|events: \[|setModels\(\[\{|setSessions\(\[\{|setEvents\(\[\{" frontend/src frontend/public
```

Result:

- PASS
- No `mock`, `sample`, `demo`, or `fake` keyword matches in frontend code/assets.
- No hardcoded model id, session id, or run id matches.
- Structural search only found empty array state initialization and variable names, not business-data literals.

Harmless structural matches:

- `frontend/src/state/useModelsState.ts`: `useState<ModelDto[]>([])` initial empty state.
- `frontend/src/components/ModelSelect.tsx`: filters real `models` prop.
- `frontend/src/pages/ControlPage.tsx`: uses real `useModelsState`.

Blocked items:

- None.
