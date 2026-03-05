# EzHome

EzHome is a lightweight Paper/Folia plugin for creating, deleting, and teleporting to homes with an in-game GUI.

## Features

- `/home` GUI to view and use saved homes.
- `/home create <name>` and `/home delete <name>` commands.
- Async home data saving per player.
- Paper + Folia compatible scheduling.
- LuckPerms-aware permission checks.

## Requirements

- Java 21+
- Paper 1.21.1+ (or compatible)
- LuckPerms (recommended; used for permission checks when present)

## Installation

1. Build or download `EzHome-<version>.jar`.
2. Place the jar in your server `plugins/` folder.
3. Install LuckPerms in `plugins/`.
4. Start/restart the server.

## Commands

- `/home` — Open the home GUI.
- `/home create <name>` — Create a home at your current location.
- `/home delete <name>` — Delete a saved home.

## Permissions (LuckPerms)

Assign these with LuckPerms, for example:

```bash
/lp user <player> permission set ezhome.use true
/lp user <player> permission set ezhome.homes.3 true
```

### Nodes

- `ezhome.use` — Allows using `/home`.
- `ezhome.homes.<number>` — Sets max homes based on highest granted number (1–54).
  - Example: `ezhome.homes.5` allows up to 5 homes.

> Backward compatibility: `ezhomes.homes.<number>` is still read if you used the old node prefix.

## Configuration

`plugins/EzHome/config.yml`

- `gui-title` — GUI inventory title.
- `default-home-limit` — Fallback limit if no permission node applies.
- `team-slots` — Reserved GUI slots (0–2 effectively used).
- `messages.*` — User-facing messages.

## Data Storage

Homes are stored per player in YAML files under:

- `plugins/EzHome/playerdata/<uuid>.yml`

## Notes

- Shift + left click a home bed in GUI to delete.
- Left click a home bed in GUI to teleport.
