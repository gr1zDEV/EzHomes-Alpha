# EzHome

EzHome is a simple Paper/Folia homes plugin with a configurable GUI.

## Features
- `/home` opens a homes GUI.
- `/home create <name>` creates a home at your current location.
- `/home delete <name>` deletes an existing home.
- Configurable GUI layout/items/messages in `config.yml`.
- Home slot limits based on permissions.
- Optional LuckPerms API integration for permission checks.

## Installation
1. Build the plugin:
   ```bash
   ./gradlew build
   ```
2. Place the built jar from `build/libs/` into your server `plugins/` folder.
3. Start the server to generate `plugins/EzHome/config.yml`.
4. Edit config values as needed and restart or reload.

## Permissions
- `ezhome.use` - use the `/home` command (default: `true`).
- `ezhomes.homes.<number>` - max unlocked home slots (1-54).
  - Example: `ezhomes.homes.5` allows up to 5 homes.
- `ezhomes.homes.*` - all home slots (default: op).

### LuckPerms support
EzHome now supports LuckPerms directly (if installed). If LuckPerms is present, EzHome reads the player's effective permissions through LuckPerms contexts. If LuckPerms is not installed, EzHome falls back to Bukkit's `player.hasPermission(...)`.

Example LuckPerms commands:
```bash
/lp user Steve permission set ezhomes.homes.5 true
/lp group vip permission set ezhomes.homes.10 true
```

## Commands
- `/home` - open the GUI.
- `/home create <name>` - create a home.
- `/home delete <name>` - delete a home.

## Configuration notes
- `default-home-limit` sets the fallback slot limit when no higher permission is granted.
- `gui.team-slots` reserves slots for team items.
- Messages support `&` color codes and MiniMessage tags.
