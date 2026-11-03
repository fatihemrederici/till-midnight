# Scene format (version 1)

A scene is one JSON file. The structure is enforced by [`scenes/scene.schema.json`](../scenes/scene.schema.json);
this page defines what it means. [`scenes/cafe-candle.json`](../scenes/cafe-candle.json) is the reference example.

## Parts of a scene

| Field | Purpose | Sent to the client? |
| --- | --- | --- |
| `id`, `title`, `intro`, `grid`, `durationSeconds`, `maxTouches` | Scene setup | Yes |
| `objects` | Things on screen: position and a finite set of states, each with an emoji | Only the initial state's emoji |
| `touches` | What a player's touch does | **Never** |
| `triggers` | What happens by itself over time | **Never** |
| `disasters` | Which state combinations end the loop | **Never** |

The client learns about later states only from the timeline a played attempt returns. Sending the last three
fields would put the solution in the browser's network tab.

## Conditions and effects

Both use the same shape: `{ "object": "candle", "state": "lit" }`.

- In `when`, it is a condition. All conditions must hold, and a missing `when` always holds.
- In `then`, it is an effect that moves the object to that state.

Setting an object to the state it is already in does nothing: no event, and its entry time does not reset.

## Triggers

A trigger has exactly one of these:

- `"at": 2` fires at second 2.
- `"after": { "object": "cat", "state": "prowling", "delay": 2 }` fires 2 seconds after the object *entered* that
  state, and only if it is still there and has not re-entered it since.

The second form is what makes chains: interrupt a state and everything hanging off it never happens.

## Touches

`POST /attempt` will receive the full list of touches for the loop:

```json
{ "sceneId": "cafe-candle", "touches": [{ "object": "machine", "second": 4 }, { "object": "curtain", "second": 7 }] }
```

- The player adds one touch per loop. Earlier touches replay automatically, and the client can clear them to start over.
- The server stays stateless about this: it validates and simulates whatever list it receives.
- Valid `second` values are 0 to `durationSeconds - 1`. A list may hold at most `maxTouches` touches, with no duplicate
  object and second pair.
- The first rule in `touches` for that object whose `when` holds is applied. If none holds, the touch has no effect
  and the timeline records that.

## Simulation order

The engine is deterministic: no randomness, no clock, same input gives the same timeline. Starting from each object's
`initialState` (entered at second 0), for each second `t` from 0 to `durationSeconds`:

1. **Triggers.** Work out which triggers are due from the state at the start of `t`. Fire them in file order, checking
   each one's `when` at the moment it fires.
2. **Touches.** Apply the touches for second `t`, sorted by object id.
3. **Disasters.** Check in file order. The first match ends the loop as a loss.

If no disaster has happened after second `durationSeconds`, the player wins.

Triggers run before touches because of what the player sees: during second 3 the screen already shows what happened
at second 3, so a touch in that second must act on it.

Calculating due triggers up front means two triggers waiting on the same state both fire, even if the first one
changes that state. Ordering inside a second still matters where one trigger's effect feeds another's `when`, as with
`steam-blows-candle`, which sits above `cat-knocks-candle`.

## Reference results for `cafe-candle`

Use these as engine tests from day 4 on. They come from a throwaway simulation of the rules above.

| Touches | Result |
| --- | --- |
| none | Loss: `explosion` at second 8 |
| `machine` at 3 | **Win** with one touch: steam blows the candle out at second 4 |
| `machine` at 5 | Loss: `fire` at second 9 |
| `candle` at 1 | Loss: `explosion` at second 8 |
| `cat` at 3 | Loss: `explosion` at second 4 |
| `pitcher` at 5 | Loss: `explosion` at second 6 |
| `candle` at 1, `machine` at 5 | Win |
| `machine` at 4, `pitcher` at 4 | Win |

Solver figures to expect, with 5 objects × 10 seconds = 50 possible touches:

- 1 winning single touch out of 50.
- 92 winning pairs out of 1,225. 47 of those only contain the single-touch win plus an extra touch, leaving 45 real
  two-touch solutions.

The no-touch timeline, in order:

| Second | Trigger | Change |
| --- | --- | --- |
| 1 | `machine-heats` | machine: idle → heating |
| 2 | `cat-wakes` | cat: asleep → prowling |
| 3 | `machine-pressure` | machine: heating → pressure |
| 4 | `cat-knocks-candle` | candle: lit → fallen; cat: prowling → hiding |
| 6 | `flame-reaches-curtain` | curtain: hanging → smoking |
| 8 | `machine-blows` | machine: pressure → exploded, then the `explosion` disaster |

The explosion at second 8 comes before the fire at second 9, so the player only discovers the fire after fixing the
machine. Hiding one disaster behind another is what gives a scene a second layer.

## Checks the schema cannot do

The solver endpoint on day 6 has to reject these with a reason the LLM can act on:

- A reference to an object or state that does not exist.
- An object position outside the grid, or two objects on the same cell.
- `at` or `after` timings that can never fall within the scene.
- A scene with no winning solution, or one that needs no touch at all.
