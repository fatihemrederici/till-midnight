# Story format

A story is one JSON file in `stories/`. The rules and all the writing live here, not in code.

Every text the player reads has all three languages:

```json
{ "en": "Lock the door", "tr": "Kapıyı kilitle", "pl": "Zamknij drzwi" }
```

## Top level

| Field | Meaning |
| --- | --- |
| `id` | Unique story name |
| `title` | Shown on the opening screen |
| `vision` | The short lines of the disaster the player sees first |
| `wake` | The line after the vision |
| `start` | Starting value of each meter |
| `maxMeter` | Highest value a meter can reach |
| `meters` | The meters. A meter that reaches 0 ends the game with its `ending` |
| `cards` | The day, in order |
| `endings` | Every way the day can end |

## Card

```json
{
  "id": "elevator",
  "time": "12:30",
  "emoji": "🛗",
  "who": { "en": "...", "tr": "...", "pl": "..." },
  "text": { "en": "...", "tr": "...", "pl": "..." },
  "variants": [{ "when": { "warned": true }, "text": { "en": "...", "tr": "...", "pl": "..." } }],
  "when": { "elif": true },
  "left": { },
  "right": { }
}
```

- `when`: The card only appears if the flags match.
- `variants`: The first variant whose `when` matches replaces `text`. This is how an earlier choice comes back later.

## Choice (`left` and `right`)

| Field | Meaning |
| --- | --- |
| `label` | What the player is choosing |
| `effects` | Meter changes, such as `{ "mind": -1 }`. Values stay between 0 and `maxMeter` |
| `set` | Flags to remember, such as `{ "elif": true }` |
| `death` | `{ "when": {...}, "ending": "stairs" }`. If `when` matches the flags from before this choice, the player dies here |
| `after` | The line shown above the next card |

## Ending

| Field | Meaning |
| --- | --- |
| `won` | `true` if the player reached midnight |
| `when` | For wins: which flags this win needs. The first match is used |
| `title`, `text` | Shown on the ending screen |
| `chain` | The steps that led here. Each step can have its own `when` |
| `hint` | What to try next time |

## Order of one swipe

1. If the choice has a `death` and its `when` matches, the game ends.
2. The choice's `set` flags are added.
3. The `effects` are applied.
4. The first meter at 0, in file order, ends the game.
5. The next card whose `when` matches comes up.
6. With no card left, the first winning ending whose `when` matches is the result.

A flag that was never set counts as `false`.

## Checks

`POST /internal/validate` takes a story file and reports broken references and missing translations. It also walks every possible path and returns the analysis: how many paths exist and which cards and endings can be reached.

`StoryContentTest` runs the same checks on `first-day.json`, so a broken story fails the build.

`stories/first-day.paths.json` lists every path and its ending. The Java engine and the TypeScript engine are both tested against it, which is what keeps them identical. If you change the story, regenerate that file and the tests will show what moved.
