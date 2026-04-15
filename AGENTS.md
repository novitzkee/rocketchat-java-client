# AGENTS.md — rocketchat-java-client

## Project Overview

A Java client library for the [Rocket.Chat](https://rocket.chat/) server API. Two core clients:

- **`RocketChatRealtimeClient`** — Rocket.Chat Realtime API over WebSockets (DDP protocol).
- **`RocketChatRestClient`** — Rocket.Chat REST API.

## Tech Stack

- **Language:** Java 17
- **Build tool:** Maven (standard directory layout)
- **Serialization:** Moshi (JSON)
- **JSON querying:** JsonPath (`com.jayway.jsonpath:json-path`)
- **Logging:** SLF4J API (Logback bound in tests)
- **Code generation:** Lombok (`@Slf4j`, `@Builder`, `@Getter`, `@Setter`, etc.)
- **Test frameworks:** JUnit 5, Mockito, AssertJ, Awaitility
- **Integration testing:** Testcontainers (MongoDB + Rocket.Chat containers), Maven Failsafe Plugin

## Build & Test Commands

| Command       | Description                                                                  |
|---------------|------------------------------------------------------------------------------|
| `mvn compile` | Compile the project                                                          |
| `mvn test`    | Run unit tests only (Surefire — `*Test.java`)                                |
| `mvn verify`  | Run full suite: unit tests + integration tests (Failsafe — `*IT.java`)       |
| `mvn package` | Build the JAR artifact                                                       |

### Integration Test Prerequisites

Integration tests require Docker. The Failsafe plugin is configured with `DOCKER_HOST=tcp://localhost:2375`.

## Directory Structure

```
src/main/java/org/novitzkee/rocketchatclient/
├── realtime/        # Realtime API client (see .agents/skills/realtime-client/)
└── rest/            # REST API client (see .agents/skills/rest-client/)

src/test/java/org/novitzkee/rocketchatclient/
├── realtime/        # Realtime client unit tests
├── rest/            # REST client unit tests (not yet implemented)
└── integration/     # Integration tests (Testcontainers — Docker required)
```

## Coding Conventions

- Prefer Java records for DTOs and value objects.
- Lombok `@Accessors(fluent = true)` — `call.id()` not `call.getId()`.
- Lombok `@Slf4j` for logging.
- AssertJ for assertions, Awaitility for async, Mockito for mocking.
- Unit test methods: `should...` naming. Integration tests: `test...` naming.
- JSON fixtures in `src/test/resources/json/`, loaded by per-package helper classes.

## Skills

Detailed guidance for specific areas lives in `.agents/skills/`:

| Skill | Path | Description |
|-------|------|-------------|
| Realtime Client | `.agents/skills/realtime-client/` | Architecture, conventions, and how-to for the DDP/WebSocket client |
| REST Client | `.agents/skills/rest-client/` | (Planned) REST API client guidance |
| DDP Protocol | `.agents/skills/ddp-protocol/` | (Planned) DDP protocol reference |

## Important Notes

- The project is in early development (`0.0.0-SNAPSHOT`).
- Integration tests use `withReuse(true)` on Testcontainers to keep containers alive for debugging;
this does **not** persist across separate Maven runs unless Testcontainers reuse is globally configured.
