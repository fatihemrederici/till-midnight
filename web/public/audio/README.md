# Background audio

The game plays these files in a loop and fades between them. A missing file stays silent.

| File                     | When it plays                                    |
| ------------------------ | ------------------------------------------------ |
| `music/day.mp3`          | On the street and at work, until you get home    |
| `music/evening.mp3`      | At home until 22:00                              |
| `music/night.mp3`        | From 22:00 to midnight                           |
| `ambience/street.mp3`    | Cards on the street                              |
| `ambience/work.mp3`      | Cards at the office                              |
| `ambience/home.mp3`      | Cards at home until 19:00                        |
| `ambience/home-rain.mp3` | Cards at home after 19:00, when the storm starts |

Use MP3 files without vocals. Before adding a track, level it and make its end blend into its start so the loop has no seam (the tracks here were made with ffmpeg `loudnorm` and a 3 second `acrossfade` from the end into the start).
