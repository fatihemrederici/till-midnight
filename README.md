# 10sec

A daily puzzle game. Every day there is a new scene, and in ten seconds something goes badly wrong.
You get one new touch per loop. Time rewinds, your earlier touches replay, and you work out the chain
of events until the disaster never happens.

**Status:** day 1 of 30. Scene format and API skeleton.

## Layout

| Path | What it is | Stack |
| --- | --- | --- |
| `api/` | Game engine, solver, REST API | Java 21, Spring Boot |
| `web/` | Browser client | React, TypeScript |
| `ai-worker/` | Generates and repairs scenes | Python, FastAPI |
| `scenes/` | Scene JSON and its schema | |
| `docs/` | Design notes | |

The engine runs only on the server. The client sends touches and plays back the timeline it receives,
so scores cannot be faked and the puzzle's rules never reach the browser.

## Run the tests

Requires Java 21.

```sh
cd api
./mvnw test
```

## Scene format

See [docs/scene-format.md](docs/scene-format.md).
