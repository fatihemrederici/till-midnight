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

## The design in four rules

1. **Death comes from traps.** A choice can secretly set a trap. A later choice can trigger it. The player never sees traps; the death screen reveals them with the time of each choice.
2. **Two meters: Mind and Body.** Every chapter has the same two. A meter at 0 kills the player, and the ending depends on where the current card is: the street, home or work.
3. **No free choice.** Every choice lowers a meter, sets a flag or can kill. Usually the comfortable side sets a trap and the hard side costs a meter.
4. **Every death has a reason.** A death's condition, or its card's condition, needs at least one flag. The death screen builds the chain from those flags, so the explanation is always the real one.

## Add a chapter

1. Create the folder with the four files.
2. Add its id to `chapters.json`.
3. Generate its sample paths:

   ```sh
   cd api && ./mvnw test -Dtest=UpdateStoryPaths -DupdateStoryPaths=true
   ```

4. Run the tests.

The server checks every chapter when it starts and refuses to run with a broken one. `POST /internal/validate` runs the same checks on a draft and returns the analysis, including how often a random player survives.

## Rules: `story.json`

| Field | Meaning |
| --- | --- |
| `formatVersion` | `2` |
| `id` | Chapter name, the same as its folder |
| `start` | Starting value of `mind` and `body` |
| `maxMeter` | Highest value a meter can reach |
| `meters` | `mind` and `body`, in that order, each with a death for every place |
| `traps` | Flags that are traps. Setting one plays a faint sound, nothing more |
| `cards` | The day, in order |
| `endings` | Every way the day can end |

A meter:

```json
{ "id": "mind", "emoji": "🧠", "endings": { "street": "panic-street", "home": "panic-home", "work": "panic-work" } }
```

A card:

```json
{
  "id": "stairs",
  "time": "12:30",
  "place": "home",
  "emoji": "🛗",
  "image": "stairs.jpg",
  "when": { "noKeys": false },
  "variants": [{ "when": { "work": true } }],
  "left": {
    "effects": { "body": -1 },
    "deaths": [{ "when": { "wet": true, "pill": true }, "ending": "stairs" }]
  },
  "right": { "effects": { "mind": -1 }, "set": { "lift": true } }
}
```

- `place`: `street`, `home` or `work`. A meter that runs out here uses this place's death.
- `image`: Optional. A photo in the chapter's `images/` folder, like `stairs.jpg`. The game shows it instead of the emoji. The API serves it at `/stories/{chapterId}/images/{file}`, and the loader stops if the file is missing.
- `shownTime`: Optional. The time the screen shows when it differs from the real one, like a clock that runs fast.
- `when`: The card only appears if the flags match.
- `variants`: The first variant whose `when` matches replaces the card's text. This is how an earlier choice comes back later.
- `effects`: Meter changes. Values stay between 0 and `maxMeter`.
- `set`: Flags to remember.
- `deaths`: Checked against the flags from before this choice. The first one whose `when` matches ends the game. A death with no `when` always kills, so its card needs a `when`.

An ending:

```json
{ "id": "together", "won": true, "when": { "together": true } }
```

- `won`: `true` if the player reached midnight.
- `when`: For wins, which flags this win needs. The first match is used. One win must have no `when`.

## Text: `en.json`, `tr.json`, `pl.json`

All three files have the same shape. A text belongs to the card or ending with the same id.

```json
{
  "title": "The First Day",
  "vision": ["The bus turns onto the bridge."],
  "wake": "You open your eyes.",
  "meters": { "mind": "Mind", "body": "Body" },
  "cards": {
    "stairs": {
      "who": "Your building",
      "text": "Twelve floors. The stairs have just been mopped.",
      "variants": ["The same text, after work."],
      "left": { "label": "Stairs", "after": "You made it.", "past": "You climbed the freshly mopped stairs" },
      "right": { "label": "Lift", "after": "Nothing happens.", "past": "You took the lift" }
    }
  },
  "endings": {
    "stairs": {
      "title": "Wet Step",
      "text": "Your foot slipped on the seventh floor.",
      "hint": "Stairs are good for you, they say.",
      "omen": "The steps are wet."
    }
  }
}
```

- `after`: Shown above the next card. Not needed on a choice that always kills.
- `past`: The choice told in the past tense. The death screen uses it, with the card's time, to build the chain.
- `omen`: Every death needs one. When Mind is low, a card's text turns into an omen for a moment, and sometimes it points at a death the player has really set up.
- `variants` match the list in `story.json` by position.

A missing translation or a text that nothing uses, such as a misspelled card id, stops the server with a clear message.

## Order of one swipe

1. If one of the choice's `deaths` matches, the game ends.
2. The choice's `set` flags are added.
3. The `effects` are applied.
4. The first meter at 0, in file order, ends the game with the death for this card's place.
5. The next card whose `when` matches comes up.
6. With no card left, the first winning ending whose `when` matches is the result.

A flag that was never set counts as `false`.

## What the checks reject

- Meters other than `mind` and `body`, or a meter with no death for a place.
- A choice with no cost, or a death with no flag behind it.
- A condition that uses a flag no choice ever sets, usually a typo.
- A trap that is never set, or never matters.
- A card time that goes backwards.
- A card or ending that no path reaches, or a chapter nobody can survive.

A meter's place deaths are a safety net: if Mind can never run out at work, `panic-work` stays unreachable without an error, and it doesn't count toward the deaths a player can find.
