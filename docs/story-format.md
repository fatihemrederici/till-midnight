# Story format

Each chapter is a folder in `stories/`. Chapters are independent: their own characters, rules and endings.

```
stories/
  chapters.json        The chapters, in play order
  first-day/
    story.json         Rules only, no player-facing text
    en.json            English text
    tr.json            Turkish text
    pl.json            Polish text
```

## Add a chapter

1. Create the folder with the four files.
2. Add its id to `chapters.json`.
3. Generate its expected paths:

   ```sh
   cd api && ./mvnw test -Dtest=UpdateStoryPaths -DupdateStoryPaths=true
   ```

4. Run the tests.

The server checks every chapter when it starts and refuses to run with a broken one.

## Rules: `story.json`

| Field | Meaning |
| --- | --- |
| `id` | Chapter name, the same as its folder |
| `start` | Starting value of each meter |
| `maxMeter` | Highest value a meter can reach |
| `meters` | A meter that reaches 0 ends the game with its `ending` |
| `cards` | The day, in order |
| `endings` | Every way the day can end |

A card:

```json
{
  "id": "elevator",
  "time": "12:30",
  "emoji": "🛗",
  "when": { "elif": true },
  "variants": [{ "when": { "warned": true } }],
  "left": { "effects": { "mind": -1 }, "set": { "stairs": true } },
  "right": { "death": { "when": { "wetShoes": true }, "ending": "stairs" } }
}
```

- `when`: The card only appears if the flags match.
- `variants`: The first variant whose `when` matches replaces the card's text. This is how an earlier choice comes back later.
- `effects`: Meter changes. Values stay between 0 and `maxMeter`.
- `set`: Flags to remember.
- `death`: If `when` matches the flags from before this choice, the player dies here.

An ending:

```json
{ "id": "fuse", "won": false, "chain": [{ "when": { "elif": true } }, {}, {}] }
```

- `won`: `true` if the player reached midnight.
- `when`: For wins, which flags this win needs. The first match is used.
- `chain`: One entry per step that led here. A step with `when` only shows if the flags match.

## Text: `en.json`, `tr.json`, `pl.json`

All three files have the same shape. A text belongs to the card or ending with the same id.

```json
{
  "title": "Till Midnight",
  "vision": ["The bus turns onto the bridge."],
  "wake": "You open your eyes.",
  "meters": { "mind": "Mind" },
  "cards": {
    "elevator": {
      "who": "The elevator",
      "text": "The door opens.",
      "variants": ["The same text, after a warning."],
      "left": { "label": "Take the stairs", "after": "Twelve floors." },
      "right": { "label": "Get in" }
    }
  },
  "endings": {
    "fuse": {
      "title": "The Bare Wire",
      "text": "In the dark, you touched the wrong place.",
      "chain": ["First step", "Second step", "Third step"],
      "hint": "What to try next time."
    }
  }
}
```

`variants` and `chain` match the lists in `story.json` by position.

A missing translation or a text that nothing uses, such as a misspelled card id, stops the server with a clear message.

## Order of one swipe

1. If the choice has a `death` and its `when` matches, the game ends.
2. The choice's `set` flags are added.
3. The `effects` are applied.
4. The first meter at 0, in file order, ends the game.
5. The next card whose `when` matches comes up.
6. With no card left, the first winning ending whose `when` matches is the result.

A flag that was never set counts as `false`.

## Crisis cards and locked choices

Both are optional. A chapter without them works the same, and an empty meter ends the game.

A meter with a `crisis` card doesn't end the game at 0. Its card comes up first:

```json
{
  "id": "nerve",
  "emoji": "🧠",
  "ending": "chest",
  "crisis": {
    "id": "crisis-nerve",
    "emoji": "🫁",
    "left": { "effects": { "trust": -2 }, "requires": { "trust": 2 } },
    "right": { "effects": { "stamina": -2 }, "requires": { "stamina": 2 } }
  }
}
```

The player pays with another meter, and the empty meter goes back to 1. A meter can use its crisis only once per run. If it runs out again, or the player can't pay either side, the game ends with the meter's `ending`.

The crisis text lives in the language files under `crises`, using the meter's id.

Any choice can have `requires`: The player needs at least that much in those meters to pick it. One side of a normal card must always be free. In a crisis, both sides can have `requires`.

A meter can also set `effect` to `mind`, `body` or `trust` for its screen and sound effect when it runs low.
