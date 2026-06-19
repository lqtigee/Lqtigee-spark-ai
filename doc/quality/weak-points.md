# Weak Point Elimination Plan

Target: raise implementation confidence toward 95% by removing predictable rework causes.

## 1. Current Weak Points

| ID | Weak point | Rework risk | Required elimination |
| --- | --- | --- | --- |
| WP-001 | API contract not formalized | Frontend/backend drift | `doc/contracts/backend-api-contract.md` |
| WP-002 | Definition of Done absent | "Almost done" tickets | `doc/quality/definition-of-done.md` |
| WP-003 | E2E matrix absent | missed integration failures | `doc/quality/e2e-matrix.md` |
| WP-004 | opencode SQLite reader not fully reflected in older docs | wrong parser implementation | update architecture/plan/micro tickets |
| WP-005 | CLI help exists but command builder tests not implemented | command argument drift | add static evidence and command builder tests before enabling runs |
| WP-006 | Frontend/backend contract sync not enforced | type drift | contract-first frontend tickets |
| WP-007 | PWA cache risk | stale API state | service worker API exclusion test |
| WP-008 | Bug fix process was advisory | broad repair risk | `AGENTS.md` bug protocol |
| WP-009 | Codex command shape used plain exec instead of selected-session resume | command starts wrong thread | require static evidence and `CodexCommandBuilderTest` |
| WP-010 | No single immutable project fact source | constants drift | `PROJECT_SPEC.md` |
| WP-011 | Response examples not separated from runtime data | frontend invents shape | `doc/contracts/backend-response-fixtures.md` |

## 2. Remaining Pre-Implementation Gates

These must be completed before claiming 95% confidence:

1. Execute Codex static evidence ticket.
2. Execute opencode static evidence ticket.
3. Add Codex command builder test.
4. Add opencode command builder test.
5. Add backend API contract tests from response fixtures.
6. Add sanitized parser fixtures.
7. Add opencode SQLite schema guard test.
8. Add non-zero process exit test.
9. Execute frontend no-mock audit after frontend exists.
10. Execute service worker API exclusion test after service worker exists.

## 3. 95% Confidence Criteria

The project can be treated as approximately 95% implementation-ready only when:

```text
Discovery complete: yes
Implementation design complete: yes
API contract complete: yes
Definition of Done complete: yes
E2E matrix complete: yes
Micro tickets cover parser/runtime/frontend/PWA: yes
Static gates for CLI command construction: present
No stale wrong parser plan remains: yes
Codex selected-session command uses resume: yes
```

Current status after this document:

```text
Design confidence: high
Implementation confidence: 94-95% after static evidence tickets and command builder tests
Rework risk: low, concentrated in runtime command builder argument order and final Android secure context
```

## 4. Mandatory Next Tickets

Add or execute these next:

```text
P0-M001 Create PROJECT_SPEC.md
CONTRACT-M001 Add backend API contract reference to micro tickets
CONTRACT-M002 Add backend response fixture examples
STATIC-M001 Verify Codex resume command static evidence
STATIC-M002 Verify opencode session command static evidence
QA-M001 Add Definition of Done enforcement note to AGENTS.md
QA-M002 Add E2E matrix release gate note to README.md
QA-M003 Add release checklist
SQLITE-M001 Add opencode SQLite schema guard
SEC-M001 Add secret leak audit
```

## 5. No-Rework Rule

If implementation reveals a contradiction with discovery:

1. Stop current ticket.
2. Add a new discovery ticket.
3. Update `implementation-design.md`.
4. Update `micro-tickets.md`.
5. Only then resume implementation.

Never patch implementation around a contradicted design assumption.
