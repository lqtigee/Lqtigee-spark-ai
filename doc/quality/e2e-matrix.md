# End-to-End Verification Matrix

This matrix defines the project-level checks required before calling the project deliverable.

## 1. Environment

| ID | Check | Command / Action | Pass Criteria |
| --- | --- | --- | --- |
| ENV-001 | Port free | `ss -ltnp | rg ':20261'` | No listener before app start |
| ENV-002 | Java | `java -version` | Java 21 |
| ENV-003 | Maven | `mvn -version` | Maven available, Java 21 |
| ENV-004 | Node | `node --version && npm --version` | Node/npm available |

## 2. Backend Startup

| ID | Check | Command / Action | Pass Criteria |
| --- | --- | --- | --- |
| BE-001 | Build | `mvn test` | Pass |
| BE-002 | Start | `mvn spring-boot:run` | Listens on 20261 |
| BE-003 | Health | `curl http://127.0.0.1:20261/api/health` | 200 JSON |

## 3. Security

| ID | Check | Command / Action | Pass Criteria |
| --- | --- | --- | --- |
| SEC-001 | Missing token | `curl -i /api/models` | 401 `AUTH_TOKEN_MISSING` |
| SEC-002 | Wrong token | `curl -i -H 'Authorization: Bearer wrong' /api/models` | 401 `AUTH_TOKEN_INVALID` |
| SEC-003 | Correct token | `curl -i -H 'Authorization: Bearer <token>' /api/models` | 200 or valid typed error |

## 4. Sessions

| ID | Check | Command / Action | Pass Criteria |
| --- | --- | --- | --- |
| SES-001 | Codex source | `GET /api/codex/sessions` | Real JSONL-derived sessions or typed error |
| SES-002 | opencode source | `GET /api/opencode/sessions` | Real SQLite-derived sessions or typed error |
| SES-003 | Unified | `GET /api/sessions` | Both sources succeed or endpoint returns 424 |
| SES-004 | No prompt leak | inspect response | No prompt/transcript content in v1 |

## 5. Models

| ID | Check | Command / Action | Pass Criteria |
| --- | --- | --- | --- |
| MOD-001 | List models | `GET /api/models` | Config-derived models |
| MOD-002 | Source support | command invalid source/model | `MODEL_SOURCE_UNSUPPORTED` |

## 6. Runtime

| ID | Check | Command / Action | Pass Criteria |
| --- | --- | --- | --- |
| RUN-001 | Start safe opencode run | `POST /api/runs` | Returns run id |
| RUN-002 | SSE connects | `GET /api/runs/{id}/events` | Receives real events |
| RUN-003 | Terminal event | wait process exit | Exactly one done/error/stopped |
| RUN-004 | Stop run | `POST /api/runs/{id}/stop` | Process stops or returns already finished |
| RUN-005 | Non-zero exit | forced failing command test | Run becomes FAILED |

## 7. Frontend

| ID | Check | Command / Action | Pass Criteria |
| --- | --- | --- | --- |
| FE-001 | Build | `cd frontend && npm run build` | Pass |
| FE-002 | No mock audit | `rg 'mock|fake|demo|sample' frontend/src` | No business-data match |
| FE-003 | Settings | open page | URL/token saved locally |
| FE-004 | Sessions | open page with backend | Shows real sessions or real error |
| FE-005 | Control | select session/model | Submit only when valid |
| FE-006 | Runs | start run | Real SSE timeline |

## 8. Android / PWA

| ID | Check | Command / Action | Pass Criteria |
| --- | --- | --- | --- |
| PWA-001 | Manifest | inspect built manifest | name `Lqtigee`, standalone |
| PWA-002 | API cache exclusion | inspect service worker | `/api/**` bypassed |
| PWA-003 | Mobile width | 360px viewport | No horizontal scroll |
| PWA-004 | Secure context | Android Chrome final URL | `window.isSecureContext === true` |
| PWA-005 | Service worker | Android Chrome final URL | Registered on secure context |
| PWA-006 | Android install | Android Chrome final URL | Install option available |

## 9. Release Gate

Release is blocked if any of these are true:

- Any endpoint uses mock data.
- Any scanner/parser failure returns success.
- Any command uses shell string concatenation.
- Any run can remain running after process exit.
- Service worker caches `/api/**`.
- Frontend contains hardcoded business sessions/models.
- Android 360px layout has horizontal scroll.
- Final Android URL is plain HTTP server IP.
- Final Android URL is not a secure context.
