# Until Midnight

You should have died this morning. A vision showed you the bus falling off the bridge, and you stepped back. Now you have to reach midnight, one swiped card at a time, while death comes back through your own small choices. Skip lunch at noon and you can faint on the stairs at night.

Playable in English, Turkish and Polish. The language changes instantly at any point in the game.

## How it works

```
stories/first-day.json     The whole game as data: cards, rules, endings, three languages
        |
Java server                Loads and checks the story, replays each finished run, stores results
        |
PostgreSQL                 The runs table
        |
React client               Runs the same rules for instant swipes, plays sounds and animations
```

The client decides nothing that counts. It sends only the list of swipes, and the server replays them to get the real result.

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

Both test suites play all 485 possible paths of the story from `stories/first-day.paths.json` and must reach the same endings.

## API

| Request | Purpose |
| --- | --- |
| `GET /story` | The story |
| `POST /runs` | Saves a finished run: `{ storyId, runId, playerId, choices: ["LEFT", "RIGHT"] }`. Returns the ending and statistics |
| `POST /internal/validate` | Checks a story file without loading it |

Writing a new story: [docs/story-format.md](docs/story-format.md)
