# 10sec

A daily puzzle game. Every day there is a new scene, and in ten seconds something goes badly wrong.
First you get a vision of everything going wrong. Then it starts for real, and you have ten seconds and a few
touches to stop it. Time rewinds until you make it.

**Status:** in progress. Three levels are playable in the browser, with progress and stars saved to PostgreSQL.

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

## Run it

Requires Java 21, Node 20.19 or newer, and Docker.

```sh
docker compose up -d
cd api && ./mvnw spring-boot:run
cd web && npm install && npm run dev
```

Then open http://localhost:5173.

## Run the tests

The database must be running. Tests use their own schema.

```sh
docker compose up -d
cd api && ./mvnw test
```

## Scene format

See [docs/scene-format.md](docs/scene-format.md).
