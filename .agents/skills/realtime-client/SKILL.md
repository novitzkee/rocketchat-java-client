---
name: realtime-client
description: Explains core concepts and patterns for the Rocket.Chat realtime client implementation. Use when extending or modifying the Realtime API client.
author: Jarosław Nowicki
version: 1.0
---

## Source Code Layout

Source root: `src/main/java/org/novitzkee/rocketchatclient/realtime/`

Core implementation class: `RocketChatRealtimeClient`

- `common/` — Shared DDP protocol types
- `exception/` — Client-specific exceptions
- `json/` — Moshi JSON adapters for DDP types
- `message/` — Concrete DDP message implementations
- `method/` — Rocket.Chat method call implementations, organized by domain:
  - `authentication/` — Authentication-related method calls
  - `channels/` — Channel-related method calls
- `util/` — Internal utilities

## Architecture

`RocketChatRealtimeClient` communicates over a single WebSocket connection using the DDP protocol:

1. **Connection lifecycle:** `connect()` establishes the WebSocket and sends a DDP `connect` message. `close()` performs a graceful shutdown.
2. **Synchronous call pattern:** Each outgoing call gets a unique `CallId`. Its `CompletableFuture` is stored in a Caffeine cache with a configurable TTL. The server response completes the matching future.
3. **Method dispatch:** Call `performMethodCall()` with a `MethodCall<R>` instance.
4. **JSON:** Moshi with custom type adapters in the `json/` package.

## Adding a New Realtime Method

1. Add a `MethodName` value if it doesn't exist yet.
2. Create a class extending `MethodCall<R>` in the appropriate `method/` sub-package.
3. Implement `resultClass()` to return the response type.
4. Add unit tests in `src/test/java/.../realtime/` using mocked WebSocket responses.
5. Add JSON fixtures in `src/test/resources/json/realtime/` as needed.

## Conventions

- `MethodCall` subclasses use static factory methods or builders — no public constructors.
- Response DTOs are Java records nested inside their `MethodCall` class.

## Test Patterns

- **Unit tests** (`*Test.java`): Mock the `WebSocket` and `HttpClient`. Simulate server responses by invoking the `WebSocketListener` directly. Use `RocketChatRealtimeMessages` helper to load JSON fixtures.
- **Integration tests** (`*IT.java`): Extend `RocketChatIntegrationTest` which provides live Rocket.Chat + MongoDB via Testcontainers.
- **Custom matchers:** `util/MessageMatchers` provides Mockito argument matchers for DDP JSON messages.
- Use AssertJ for assertions, Awaitility for async waiting.

## Integration Test Orchestration

`RocketChatRealtimeClientIT` uses a two-phase gatekeeper pattern driven by annotations from the `orchestration` package:

- **Phase 1 (`@AuthTestPhase`):** A single `@Nested` `AuthenticationTest` class runs first, sequentially. It connects and authenticates the shared `rocketChatRealtimeClient`. If any auth test fails, all Phase 2 classes are skipped automatically.
- **Phase 2 (`@DomainTestPhase`):** Each domain (channels, rooms, messages, users, etc.) is a separate `@Nested` class annotated with `@DomainTestPhase`. These classes run **concurrently** after auth succeeds.

When adding a new method, add its integration test to the existing `@DomainTestPhase` class for that domain,
or create a new `@Nested @DomainTestPhase` class if the domain doesn't exist yet. 
The outer class is `@TestInstance(PER_CLASS)` so all nested classes share the same authenticated client instance — no additional setup is needed.
Each nested domain test should create resources to verify CRUD operations on that domain and use method order for the nested test class if it's necessary.

