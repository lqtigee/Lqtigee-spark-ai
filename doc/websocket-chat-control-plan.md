# WebSocket Chat Control Plan

## 目标

把手机端会话控制从“HTTP 命令 + SSE 输出 + 局部轮询”逐步迁移为“HTTP 初始数据 + WebSocket 双向会话通道”。

WebSocket 必须用于真实 Codex CLI / opencode 会话控制，不允许 mock 会话、不允许假消息、不允许本地假成功状态、不允许在 CLI 状态不可确认时伪造 running/idle。

## 现状判断

当前实现里：

- `GET /api/sessions`：加载全部真实 Codex / opencode session。
- `POST /api/sessions/refresh`：只刷新前端传入的 `{ source, id }`，用于自动刷新 running session。
- `GET /api/sessions/{source}/{id}/transcript`：分页读取真实历史消息。
- `POST /api/runs`：发起一次真实 CLI run。
- `GET /api/runs/{runId}/events`：SSE 推送 run 输出。
- `POST /api/runs/{runId}/stop`：停止 run。

SSE 只能服务端到客户端，适合展示输出流，但不适合作为完整 chat 控制通道。Chat 需要同一条连接里完成订阅、发送 prompt、停止、刷新当前 session、接收 session 状态、接收 transcript 增量、接收 run 输出和终止事件，所以 WebSocket 是正确方向。

## 协议边界

WebSocket 第一阶段只接管“打开的单个 chat 会话”的实时控制。

仍保留 HTTP：

- 初次打开 APP 的健康检查。
- 初次加载 session 列表。
- 手动全量刷新。
- 初次加载模型、能力、skills。
- 上传附件。
- 获取 WebSocket 一次性 ticket。

WebSocket 接管：

- 订阅当前 chat session。
- 推送当前 session snapshot。
- 推送最近 10 条 transcript page。
- 滚动到顶部时请求更早消息。
- 发送 prompt。
- 停止当前 run。
- 推送 run stdout/stderr/状态事件。
- 推送 run terminal 事件。
- 推送 running session 状态变化。

## 鉴权设计

浏览器原生 `WebSocket` 构造函数不能设置自定义 `Authorization` header。不能把长期 bearer token 直接放进 WebSocket URL 作为固定查询参数，因为 URL 容易进入日志、浏览器历史、代理日志。

必须采用一次性 ticket：

1. 前端用现有 bearer token 调用 `POST /api/ws-tickets`。
2. 后端验证 bearer token。
3. 后端生成一次性 ticket，保存到 PostgreSQL 或内存 ticket store。
4. ticket 字段：`ticketId`、`tokenHash`、`expiresAt`、`usedAt`、`clientNonce`。
5. ticket 有效期 30 秒。
6. ticket 只能使用一次。
7. 前端连接 `ws://host/ws/chat?ticket=...`。
8. WebSocket 握手时校验 ticket 未过期、未使用、hash 匹配。
9. 校验通过后立即标记 `usedAt`。
10. 校验失败直接拒绝握手，不进入业务 handler。

禁止：

- 禁止长期 token 直接作为 WebSocket query 参数。
- 禁止无 token WebSocket。
- 禁止 ticket 过期后自动放行。
- 禁止鉴权失败后返回假 session。

## 消息 Envelope

所有 WebSocket 消息都是 JSON object。

字段：

- `type`: 必填，字符串。
- `requestId`: 客户端发起命令时必填，服务端响应同一个 requestId。
- `sessionRef`: 当前会话引用，可空；格式 `{ "source": "CODEX|OPENCODE", "id": "..." }`。
- `runId`: run 相关消息使用，可空。
- `payload`: object，可空但必须存在。
- `timestamp`: 服务端事件必填 ISO-8601；客户端命令可选。

客户端命令类型：

- `session.subscribe`
- `session.unsubscribe`
- `session.refresh`
- `transcript.loadNewest`
- `transcript.loadOlder`
- `chat.sendPrompt`
- `run.stop`
- `ping`

服务端事件类型：

- `ack`
- `session.snapshot`
- `transcript.page`
- `run.started`
- `run.output`
- `run.terminal`
- `error`
- `pong`

## 客户端命令定义

### session.subscribe

用途：手机进入某个 chat 页面后订阅真实 session。

Payload：

```json
{
  "source": "CODEX",
  "id": "session-id",
  "messageLimit": 10
}
```

服务端必须：

1. 验证 `source` 是 `CODEX` 或 `OPENCODE`。
2. 验证 `id` 非空。
3. 调用真实 `SessionService.getRequiredSession(source, id)`。
4. 调用真实 `SessionTranscriptService.getTranscript(source, id, 10, null)`。
5. 记录当前 WebSocket connection 订阅了这个 session。
6. 发送 `ack`。
7. 发送 `session.snapshot`。
8. 发送 `transcript.page`。

找不到 session 时：

- 发送 `error`，`code=SESSION_NOT_FOUND`。
- 不允许构造假 session。

### session.unsubscribe

用途：手机返回列表或切换 session。

Payload：

```json
{
  "source": "CODEX",
  "id": "session-id"
}
```

服务端必须：

1. 只移除当前连接上对应 session 订阅。
2. 不停止 run。
3. 发送 `ack`。

### session.refresh

用途：客户端主动要求刷新当前打开的真实 session snapshot。

Payload：

```json
{
  "source": "CODEX",
  "id": "session-id"
}
```

服务端必须：

1. 只刷新请求里的 session。
2. 不扫描并推送全部 session。
3. 找不到时发送 `error`。
4. 找到时发送 `session.snapshot`。

### transcript.loadNewest

用途：打开 chat 或运行结束后拉最近消息。

Payload：

```json
{
  "source": "CODEX",
  "id": "session-id",
  "limit": 10
}
```

服务端必须调用真实 transcript 服务，返回 `transcript.page`。

### transcript.loadOlder

用途：手机滚动到顶部继续加载更早 10 条。

Payload：

```json
{
  "source": "CODEX",
  "id": "session-id",
  "limit": 10,
  "before": "oldest-cursor"
}
```

服务端必须：

1. 校验 `before` 非空。
2. 调用真实 transcript 分页。
3. 返回 `transcript.page`，其中 `payload.direction="older"`。
4. 不允许一次性返回全量历史。

### chat.sendPrompt

用途：手机输入框发送继续对话 prompt。

Payload：

```json
{
  "source": "CODEX",
  "sessionId": "session-id",
  "modelId": "gpt-5.5",
  "mode": "ASK",
  "prompt": "用户输入",
  "confirmDangerous": false,
  "codexOptions": {
    "profile": null,
    "sandbox": null,
    "approvalPolicy": null,
    "searchEnabled": null,
    "reasoningEffort": "medium",
    "skillIds": []
  },
  "opencodeOptions": null,
  "attachmentIds": []
}
```

服务端必须：

1. 验证当前 connection 已订阅此 session。
2. 验证没有同一个 session 的未终止 run。
3. 验证模型支持 source。
4. 验证 skill、附件、reasoning effort 等选项真实存在或可被 CLI 支持。
5. 调用现有真实 run 启动服务。
6. 发送 `run.started`。
7. 把 `RunEventBus` 里这个 run 的真实事件桥接到当前 WebSocket connection。

禁止：

- 禁止发送 prompt 后只在 UI 加假用户消息。
- 禁止 run 没启动成功却显示 running。
- 禁止因 run 失败自动换模型或换 source。

### run.stop

用途：停止当前 run。

Payload：

```json
{
  "runId": "run-id"
}
```

服务端必须：

1. 验证 runId 属于当前 connection 可访问的 session。
2. 调用真实 stop service。
3. 发送 `ack`。
4. 后续 terminal 状态只能来自真实 run event 或真实进程终止确认。

### ping

用途：心跳。

Payload：

```json
{}
```

服务端返回 `pong`。

## 服务端事件定义

### ack

Payload：

```json
{
  "ok": true
}
```

### session.snapshot

Payload 必须是现有 `RemoteSessionDto` 或同字段结构。

来源：

- Codex：真实 `.codex` session 文件解析。
- opencode：真实 opencode SQLite 读取。

### transcript.page

Payload：

```json
{
  "direction": "newest",
  "session": {},
  "messages": [],
  "pageInfo": {
    "oldestCursor": null,
    "newestCursor": null,
    "hasMoreBefore": false
  }
}
```

规则：

- 最近消息默认 10 条。
- 更早消息每次 10 条。
- 禁止 WebSocket 连接建立后一次推送全部历史。

### run.started

Payload 必须来自真实 `StartRunResponse`。

### run.output

Payload 必须来自真实 `RunEventDto`，包括 stdout/stderr/message/data。

### run.terminal

Payload 必须来自真实 terminal run event。

Terminal 类型：

- `done`
- `error`
- `stopped`

收到 terminal 后服务端必须：

1. 停止给该 connection 推送此 run 的输出。
2. 刷新该 session snapshot。
3. 推送 `session.snapshot`。
4. 推送最新 `transcript.page`，limit 10。

### error

Payload：

```json
{
  "code": "SESSION_NOT_FOUND",
  "message": "Session not found",
  "detail": "session-id"
}
```

错误必须真实表达后端状态。禁止把错误转换成成功空数据。

## 后端实现组件

### WsTicketController

文件建议：

- `src/main/java/com/lqtigee/sparkai/web/WsTicketController.java`

方法：

- `createTicket()`

职责：

1. 复用现有 bearer token filter。
2. 生成一次性 ticket。
3. 返回 `{ ticket, expiresAt }`。

### WsTicketService

文件建议：

- `src/main/java/com/lqtigee/sparkai/service/WsTicketService.java`

方法：

- `WsTicketDto issueTicket()`
- `void consumeTicket(String ticket)`
- `void pruneExpiredTickets()`

职责：

1. 生成不可预测 ticket。
2. hash 后保存，不保存明文。
3. 校验一次性使用。
4. 过期直接拒绝。

第一阶段可用内存 store，条件：

- 单实例部署。
- ticket TTL 30 秒。
- 不用于恢复业务状态。

如果后续多实例部署，必须迁移到 PostgreSQL。

### ChatWebSocketConfig

文件建议：

- `src/main/java/com/lqtigee/sparkai/websocket/ChatWebSocketConfig.java`

职责：

1. 注册 `/ws/chat`。
2. 绑定 handshake interceptor。
3. 拒绝无 ticket 或 ticket 无效连接。

### ChatWebSocketHandler

文件建议：

- `src/main/java/com/lqtigee/sparkai/websocket/ChatWebSocketHandler.java`

方法：

- `afterConnectionEstablished(WebSocketSession session)`
- `handleTextMessage(WebSocketSession session, TextMessage message)`
- `afterConnectionClosed(WebSocketSession session, CloseStatus status)`
- `handleTransportError(WebSocketSession session, Throwable exception)`

职责：

1. JSON parse。
2. envelope validate。
3. dispatch 到 command handler。
4. 序列化 server event。
5. connection close 时取消订阅。

### ChatCommandDispatcher

文件建议：

- `src/main/java/com/lqtigee/sparkai/websocket/ChatCommandDispatcher.java`

方法：

- `dispatch(ChatConnection connection, ChatClientMessage message)`
- `handleSubscribe(...)`
- `handleUnsubscribe(...)`
- `handleRefresh(...)`
- `handleLoadNewest(...)`
- `handleLoadOlder(...)`
- `handleSendPrompt(...)`
- `handleStopRun(...)`
- `handlePing(...)`

职责：

1. 每个 command 独立方法。
2. 每个方法只做一个业务动作。
3. 不在 dispatcher 里直接拼 CLI command。
4. 复用现有 service。

### ChatConnectionRegistry

文件建议：

- `src/main/java/com/lqtigee/sparkai/websocket/ChatConnectionRegistry.java`

方法：

- `register(connection)`
- `remove(connectionId)`
- `subscribe(connectionId, sessionRef)`
- `unsubscribe(connectionId, sessionRef)`
- `connectionsForRun(runId)`
- `connectionsForSession(sessionRef)`

职责：

1. 保存当前连接。
2. 保存连接订阅的 session。
3. 保存 runId 到 connection 的映射。
4. close 时清理映射。

### RunEventBusBridge

文件建议：

- `src/main/java/com/lqtigee/sparkai/websocket/RunEventBusBridge.java`

方法：

- `attachRun(connectionId, runId, sessionRef)`
- `detachRun(connectionId, runId)`
- `onRunEvent(RunEventDto event)`

职责：

1. 监听真实 `RunEventBus`。
2. 只向订阅该 run 的连接发送事件。
3. terminal 后刷新真实 session 和 transcript。

## 前端实现组件

### wsTicketApi.ts

方法：

- `createWsTicket(): Promise<WsTicketResponse>`

职责：

1. 使用现有 bearer token。
2. 调 `POST /api/ws-tickets`。
3. 不把长期 token 放进 ws URL。

### chatSocketClient.ts

方法：

- `connect(ticket)`
- `disconnect()`
- `send(message)`
- `subscribeSession(sessionRef)`
- `loadOlder(sessionRef, before)`
- `sendPrompt(request)`
- `stopRun(runId)`
- `ping()`

职责：

1. 管理单条 WebSocket。
2. 维护 requestId。
3. 分发 server event 到 state。
4. 不直接操作 React component state。

### useChatSocketState.ts

职责：

1. 打开 selected session 时创建 ticket 并连接。
2. 订阅 session。
3. 接收 snapshot 更新 selected session。
4. 接收 transcript page 更新 messages。
5. 接收 run output 更新流式输出。
6. 接收 terminal 后结束 running UI。
7. 断线时进入明确错误状态。

禁止：

- 禁止断线后显示假在线。
- 禁止 reconnect 时重复发送 prompt。
- 禁止没有 ack 就认为命令成功。

### SessionDetail 集成

改动原则：

1. 输入框发送走 `chatSocketClient.sendPrompt`。
2. 停止按钮走 `chatSocketClient.stopRun`。
3. 顶部加载更多走 `chatSocketClient.loadOlder`。
4. 运行输出直接来自 `run.output`。
5. 会话状态直接来自 `session.snapshot`。

## Reconnect 规则

断线后：

1. 前端把 connection state 设为 `DISCONNECTED`。
2. 不自动重发未 ack 的 `chat.sendPrompt`。
3. 重新用 bearer token 换新 ticket。
4. 新连接建立后只重发 `session.subscribe`。
5. 服务端返回最新 `session.snapshot` 和最近 10 条 transcript。
6. 如果 run 仍真实 running，服务端可继续桥接后续真实 output。
7. 如果 run 已结束，服务端返回真实 session 状态和最新 transcript。

## 幂等规则

- `session.subscribe` 可重复，重复时更新同一订阅，不创建多个订阅。
- `session.unsubscribe` 可重复，重复时返回 `ack`。
- `transcript.loadOlder` 由 `before` cursor 控制，重复请求返回同一真实分页结果。
- `chat.sendPrompt` 不自动重试。
- `run.stop` 可重复，但最终状态必须来自真实 stop/run terminal。

## 微任务拆分

### WS-M001 添加 WebSocket 依赖

文件：

- `pom.xml`

实现：

1. 添加 `spring-boot-starter-websocket`。
2. 不改任何业务代码。

验证：

```bash
mvn test -Dskip.frontend.copy=true
rg "spring-boot-starter-websocket" pom.xml
```

### WS-M002 创建 WebSocket Ticket DTO

文件：

- `src/main/java/com/lqtigee/sparkai/dto/WsTicketDto.java`

实现：

1. 定义 `ticket`。
2. 定义 `expiresAt`。

验证：

```bash
mvn test -Dskip.frontend.copy=true
rg "record WsTicketDto" src/main/java
```

### WS-M003 实现 WsTicketService 内存 store

文件：

- `src/main/java/com/lqtigee/sparkai/service/WsTicketService.java`
- `src/test/java/com/lqtigee/sparkai/service/WsTicketServiceTest.java`

实现：

1. `issueTicket()` 生成 32 字节随机 ticket。
2. 明文 ticket 只返回给调用方。
3. 内部只保存 SHA-256 hash。
4. TTL 30 秒。
5. `consumeTicket(ticket)` 校验存在、未过期、未使用。
6. consume 成功后标记已使用。
7. 第二次 consume 同一 ticket 抛认证错误。

验证：

```bash
mvn test -Dtest=WsTicketServiceTest -Dskip.frontend.copy=true
```

### WS-M004 暴露 POST /api/ws-tickets

文件：

- `src/main/java/com/lqtigee/sparkai/web/WsTicketController.java`
- `src/test/java/com/lqtigee/sparkai/web/WsTicketControllerTest.java`

实现：

1. endpoint 必须受现有 bearer token 保护。
2. 有效 token 返回 ticket。
3. 无 token 返回 `AUTH_TOKEN_MISSING`。
4. 错 token 返回 `AUTH_TOKEN_INVALID`。

验证：

```bash
mvn test -Dtest=WsTicketControllerTest -Dskip.frontend.copy=true
```

### WS-M005 注册 /ws/chat 握手

文件：

- `src/main/java/com/lqtigee/sparkai/websocket/ChatWebSocketConfig.java`
- `src/main/java/com/lqtigee/sparkai/websocket/WsTicketHandshakeInterceptor.java`
- `src/test/java/com/lqtigee/sparkai/websocket/WsTicketHandshakeInterceptorTest.java`

实现：

1. 注册 `/ws/chat`。
2. handshake 读取 query 参数 `ticket`。
3. 调用 `WsTicketService.consumeTicket(ticket)`。
4. 成功则把 authenticated flag 放进 session attributes。
5. 失败则拒绝握手。

验证：

```bash
mvn test -Dtest=WsTicketHandshakeInterceptorTest -Dskip.frontend.copy=true
```

### WS-M006 定义 WebSocket 消息 DTO

文件：

- `src/main/java/com/lqtigee/sparkai/websocket/dto/ChatClientMessage.java`
- `src/main/java/com/lqtigee/sparkai/websocket/dto/ChatServerMessage.java`
- `src/main/java/com/lqtigee/sparkai/websocket/dto/ChatSessionRef.java`

实现：

1. envelope 字段和本文一致。
2. 使用 Jackson 解析。
3. 不使用 Map 到处传业务对象，只有 `payload` 可为 `JsonNode`。

验证：

```bash
mvn test -Dskip.frontend.copy=true
rg "ChatClientMessage|ChatServerMessage|ChatSessionRef" src/main/java
```

### WS-M007 实现 ChatConnectionRegistry

文件：

- `src/main/java/com/lqtigee/sparkai/websocket/ChatConnectionRegistry.java`
- `src/test/java/com/lqtigee/sparkai/websocket/ChatConnectionRegistryTest.java`

实现：

1. 注册 connection。
2. 移除 connection。
3. subscribe/unsubscribe session。
4. attach/detach run。
5. close 清理所有索引。

验证：

```bash
mvn test -Dtest=ChatConnectionRegistryTest -Dskip.frontend.copy=true
```

### WS-M008 实现 ping/pong

文件：

- `src/main/java/com/lqtigee/sparkai/websocket/ChatWebSocketHandler.java`
- `src/main/java/com/lqtigee/sparkai/websocket/ChatCommandDispatcher.java`
- `src/test/java/com/lqtigee/sparkai/websocket/ChatCommandDispatcherTest.java`

实现：

1. 收到 `ping` 返回 `pong`。
2. pong 带同一个 requestId。
3. 不触碰 session service。

验证：

```bash
mvn test -Dtest=ChatCommandDispatcherTest -Dskip.frontend.copy=true
```

### WS-M009 实现 session.subscribe

文件：

- `ChatCommandDispatcher.java`
- `ChatConnectionRegistry.java`
- `ChatCommandDispatcherTest.java`

实现：

1. 调真实 `SessionService.getRequiredSession`。
2. 调真实 `SessionTranscriptService.getTranscript(..., 10, null)`。
3. 注册订阅。
4. 发 `ack`、`session.snapshot`、`transcript.page`。

验证：

```bash
mvn test -Dtest=ChatCommandDispatcherTest -Dskip.frontend.copy=true
```

### WS-M010 实现 transcript.loadOlder

实现：

1. 校验 source/id/before/limit。
2. 调真实 transcript service。
3. 发 `transcript.page`，direction 为 `older`。
4. 不返回全量历史。

验证：

```bash
mvn test -Dtest=ChatCommandDispatcherTest -Dskip.frontend.copy=true
```

### WS-M011 实现 chat.sendPrompt

实现：

1. 复用现有 StartRunRequest 校验。
2. 验证 connection 已订阅 session。
3. 调真实 run service。
4. 发 `run.started`。
5. 建立 runId 到 connection 映射。

验证：

```bash
mvn test -Dtest=ChatCommandDispatcherTest,RunServiceTest -Dskip.frontend.copy=true
```

### WS-M012 桥接 RunEventBus

实现：

1. 监听真实 run event。
2. 非 terminal 发送 `run.output`。
3. terminal 发送 `run.terminal`。
4. terminal 后刷新真实 session snapshot。
5. terminal 后刷新最新 transcript page。

验证：

```bash
mvn test -Dtest=RunEventBusBridgeTest -Dskip.frontend.copy=true
```

### WS-M013 实现 run.stop

实现：

1. 验证 runId 属于当前连接。
2. 调真实 stop service。
3. 返回 ack。
4. 不手动伪造 terminal。

验证：

```bash
mvn test -Dtest=ChatCommandDispatcherTest,RunServiceTest -Dskip.frontend.copy=true
```

### WS-M014 前端 ticket API

文件：

- `frontend/src/api/wsTicketApi.ts`

实现：

1. 调 `POST /api/ws-tickets`。
2. 使用现有 bearer token。
3. 返回 ticket/expiresAt。

验证：

```bash
npm --prefix frontend run build
rg "createWsTicket" frontend/src
```

### WS-M015 前端 chatSocketClient

文件：

- `frontend/src/api/chatSocketClient.ts`

实现：

1. 建立 WebSocket。
2. 维护 requestId。
3. 发送 envelope。
4. 分发 server events。
5. close/error 明确通知调用方。

验证：

```bash
npm --prefix frontend run build
rg "chatSocketClient|session.subscribe|chat.sendPrompt|run.stop" frontend/src
```

### WS-M016 前端 useChatSocketState

文件：

- `frontend/src/state/useChatSocketState.ts`

实现：

1. selected session 改变时断开旧连接。
2. 创建新 ticket。
3. 连接 WebSocket。
4. 发送 `session.subscribe`。
5. 维护 messages/session/run 状态。
6. reconnect 只重发 subscribe，不重发 prompt。

验证：

```bash
npm --prefix frontend run build
rg "useChatSocketState|DISCONNECTED|session.subscribe" frontend/src
```

### WS-M017 SessionDetail 接入 WebSocket

文件：

- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/components/SessionChatComposer.tsx`

实现：

1. 发送按钮调用 WebSocket `chat.sendPrompt`。
2. 停止按钮调用 WebSocket `run.stop`。
3. 顶部加载更多调用 WebSocket `transcript.loadOlder`。
4. 流式输出来自 WebSocket `run.output`。
5. 状态来自 WebSocket `session.snapshot`。

验证：

```bash
npm --prefix frontend run build
rg "useChatSocketState|chat.sendPrompt|run.output|transcript.loadOlder" frontend/src
```

### WS-M018 停用 chat 内 SSE 运行流

实现：

1. chat 页面不再调用 `openRunEvents`。
2. `/runs` 独立页面可暂时保留 SSE。
3. Chat 的实时输出只来自 WebSocket。

验证：

```bash
rg "openRunEvents" frontend/src/pages frontend/src/state
npm --prefix frontend run build
```

### WS-M019 真机验证

验证：

1. 手机打开 public URL。
2. 打开一个真实 Codex session。
3. 最近只显示 10 条消息。
4. 滚动顶部加载更早消息。
5. 发送 prompt。
6. 看到 WebSocket 流式输出。
7. running 状态显示为运行中。
8. stop 能停止真实 run。
9. terminal 后状态刷新为真实非 running。
10. 断网重连后只重新 subscribe，不重复发送 prompt。

禁止使用 smoke、mock 或假 session。

## 风险点

高风险：

- WebSocket 鉴权如果直接使用长期 token query，会泄漏 token。
- reconnect 如果自动重发 prompt，会重复执行真实 CLI。
- terminal 状态如果由 UI 猜测，会显示假完成。
- RunEventBus bridge 如果不清理订阅，会导致重复输出和手机卡顿。

中风险：

- 多标签页同时订阅同一 session，输出会到多个连接；这是允许的，但每个连接必须独立清理。
- 手机后台后 WebSocket 被系统断开，前端必须显示断线状态并重新 ticket。
- opencode 与 Codex transcript 更新时机不同，terminal 后必须重新读真实 transcript。

低风险：

- ping/pong 心跳间隔不准只影响断线发现速度，不影响真实数据。

## 交付判定

WebSocket chat 控制完成时必须满足：

1. 手机 chat 打开后只有当前 session 建立实时订阅。
2. 未 running 的列表 session 不参与自动刷新。
3. 输入 prompt 后能看到真实 run started。
4. 输出是 WebSocket 逐条推送，不再依赖 chat 内 SSE。
5. terminal 后 session 状态来自真实刷新。
6. 历史消息每次 10 条分页。
7. 断线不重复发送 prompt。
8. 无 mock、无 fake、无 fallback success。
