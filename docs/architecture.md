# Architecture

Till Midnight has four parts, each with one job.

| Part | Folder | Job |
| --- | --- | --- |
| Content | `stories/` | The chapters: rules and text, no code |
| Server | `api/` | Loads and checks chapters, replays finished runs, stores results |
| Database | `docker-compose.yml` | Stores the runs |
| Client | `web/` | Shows the game, reacts instantly to swipes |

## One run

1. The player picks a chapter from `GET /stories`, and the client loads it with `GET /stories/{id}`. The server has already merged the rules and languages into one object, so language changes need no request.
2. Each swipe runs through `web/src/game/engine.ts`, so the game reacts with no network delay.
3. When the run ends, the client sends only the list of swipes to `POST /runs`.
4. The server replays them in `StoryEngine.java` and stores the result it calculated itself.

Each chapter has a version: a fingerprint of its rules, calculated when it loads. Runs store it, and a run from an older version is rejected instead of being replayed under different rules. Fixing a translation doesn't change the version.

## Two engines, one behavior

The rules exist in Java and TypeScript. Java is what counts, because a client can't be trusted. TypeScript exists for instant response.

To keep them identical, both are tested against `api/src/test/resources/story-paths/`, which lists every possible path of a chapter and its ending. If one engine changes alone, its test fails.

## Server: `api/`

| Package | Contains |
| --- | --- |
| `story` | Pure game logic and content: `StoryEngine`, `StoryLoader`, `StoryMerger`, `StoryValidator`, `StoryAnalyzer`, `StoryChecker`, `StoryCatalog` |
| `run` | Saving runs and reading statistics |
| `web` | HTTP only: controllers and request and response types |

## Client: `web/src/`

| Folder | Contains |
| --- | --- |
| `game` | Rules and clock calculations, no React |
| `data` | Server calls and browser storage |
| `i18n` | Languages and interface text |
| `audio` | Sound engine, sound effects, ambience, vibration |
| `hooks` | Reusable behavior: swiping, game feedback, found endings |
| `components` | Pieces used inside screens |
| `screens` | One file per screen |
| `styles` | One file per area of the interface |

`App.tsx` decides which screen shows and holds the current run.

## Known limits

- `GET /stories/{id}` sends the rules to the browser, so a curious player can read every ending. Results are still calculated on the server.
- There are no accounts. Player ids are generated in the browser, so statistics can be inflated.
- `POST /internal/validate` is open and must be protected before going live.
