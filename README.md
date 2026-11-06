# Till Midnight

A vision shows you how you die. Now you have to reach midnight, one swiped card at a time, while death comes back through your own small choices. Leave the café with coffee in your shoe at 08:50, take a sedative at 11:30, and the freshly mopped stairs kill you at 12:30. You only find out how when the death screen shows the chain.

Playable in English, Turkish and Polish. The language changes instantly at any point in the game.

## How it works

```
stories/   The chapters as data: rules and three languages
api/       Java server: checks chapters, replays each finished run, stores results
web/       React client: runs the same rules for instant swipes, plays sounds and animations
```

The client decides nothing that counts. It sends only the list of swipes, and the server replays them to get the real result.

Read more in [docs/architecture.md](docs/architecture.md).

## Run it

You need Java 21, Node and Docker.

```sh
docker compose up -d             # database
cd api && ./mvnw spring-boot:run # server on port 8080
cd web && npm install && npm run dev   # game on port 5173
```

## Test it

```sh
cd api && ./mvnw test   # the database must be running
cd web && npm test
```

Both test suites replay the same sample of paths and must reach the same endings. GitHub Actions runs both on every push.

## API

| Request | Purpose |
| --- | --- |
| `GET /stories` | The chapters |
| `GET /stories/{id}` | One chapter |
| `POST /runs` | Saves a finished run: `{ storyId, storyVersion, runId, playerId, choices: ["LEFT", "RIGHT"] }`. Returns the ending and statistics |
| `POST /internal/validate` | Checks a chapter before it joins the game: `{ rules, texts: { en: {...} } }` |

Writing a new chapter: [docs/story-format.md](docs/story-format.md)
