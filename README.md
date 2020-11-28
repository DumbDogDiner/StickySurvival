# StickySurvival

Survival Games (commonly known as Hunger Games) plugin for the DDD server.

## Leaderboard

Placeholders are available to get any top 10 player and their win count: `%stickysurvival_lb_player_<PLACE>` and
`%stickysurvival_lb_score_<PLACE>`.

## Specifying locations in configuration files

A location in a configuration file should be a map, e.g. `{x: 10, y: 20, z: 30}`. Depending on the property, coordinates
may be floating-point or integral. In some cases, pitch and yaw can be specified.

## Adding arenas/worlds

Worlds should each be in their own world folder, and the folder name should only contain the characters in the range
`[A-Za-z0-9_]`. Each folder should have a file named `survivalgames.yml` in it. Its structure is as follows:

- **friendly name**:
    The world name players will see. Can contain any characters. The /sg join command uses the folder name, NOT the
    friendly name.
- **icon**:
    The icon for this world. Currently, this is only displayed in the hologram.
- **min players**:
    The minimum number of players that can play a game in this world.
- **max players**:
    The maximum number of players that can play a game in this world.
- **no damage time** *(optional)*:
    Overrides the default in `config.yml`.
- **chest refill** *(optional)*:
    How many seconds must pass before chests will be refilled. If set to 0 or omitted, this feature will be disabled.
- **time**:
    How many seconds a game can go on for at maximum, not accounting for no damage time.
- **hologram**:
    Where the hologram (replacement for lobby signs) will appear in the world.
- **bounds**:
    The bounding box where players will be allowed, where random chests will spawn, and where the world border starts.
    - **min**: The minimum bound for the world.
    - **max**: The maximum bound for the world.
- **spawn points**:
    The list of available spawn points where players will spawn.
- **chest percentage** *(optional)*:
    The percent of chests that will appear each game, e.g. a value of 100 means all chests will appear, and a value of 0
    means no chests will appear. Defaults to 100.
- **cornucopia** *(optional)*:
    An area in the map where all chests will appear, regardless of the value in chest percentage.
    - **min**: The lower coordinates.
    - **max**: The upper coordinates.

**Additionally**, each world must be listed in `config.yml`.