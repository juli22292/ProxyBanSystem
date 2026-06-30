# ProxyBanSystem

Cross-platform MySQL ban and punishment system for Minecraft servers and networks.

ProxyBanSystem is a cross-platform ban and punishment plugin for Minecraft networks. The same built JAR can run on proxy platforms and server platforms, while all active bans, lookup data, and registered player names are stored in one shared MySQL database.

## Compatibility

One JAR is designed to run on:

- Velocity
- BungeeCord
- Waterfall
- Spigot
- Paper
- Purpur
- Folia
- Leaf
- Sponge

Waterfall uses the BungeeCord loader. Spigot, Paper, Purpur, Folia, Leaf, and Bukkit-compatible forks use the Bukkit/Spigot loader. Folia support is marked with `folia-supported: true` in `plugin.yml`.

### Supported Minecraft Versions

For non-proxy Bukkit-based servers, the plugin is built for one shared JAR across:

- Minecraft Java `1.20.x`
- Minecraft Java `1.21.x`
- Minecraft Java `26.1.x`

The Bukkit loader declares `api-version: '1.20'` and the project compiles against Spigot API `1.20.1-R0.1-SNAPSHOT`. This keeps the Bukkit-side API baseline low while still allowing newer Bukkit-compatible platforms such as Paper, Purpur, Folia, and Leaf to load the same JAR.

Sponge support is provided through SpongeAPI `14.0.0`; use a Sponge server version that matches that API line.

## Features

- Ban players by name or UUID
- Unban players by name or UUID
- Automatic login check for banned players
- Automatic kick when an online player gets banned
- Temporary and permanent bans
- Fully configurable ban reasons with custom durations
- Fallback ban reason for unknown/custom reasons
- Customizable ban screen through `messages.yml`
- Customizable plugin messages and formats
- Built-in language switch with English as the default and German as an optional preset
- Ban and unban broadcasts for staff members
- Lookup system for ban status, UUID, ban count, unban count, reasons, executors, and history
- Remove single ban reasons from a player's lookup history
- Remove the full lookup history of a player
- List all active bans
- Whitelist players so they cannot be banned
- MySQL player registry for offline lookups and tab completion
- Local `bannedplayers.json` for additional stored history/display data
- Reload commands for config and messages
- Tab completion for players, ban reasons, and admin subcommands
- Shared core logic for all supported platforms

## Installation

1. Build the project:

```powershell
mvn clean package
```

2. Put `target/proxybansystem-4.3.jar` into the correct plugin folder:

- Velocity: proxy `plugins` folder
- BungeeCord/Waterfall: proxy `plugins` folder
- Spigot/Paper/Purpur/Folia/Leaf/Bukkit-compatible forks: server `plugins` folder
- Sponge: Sponge `plugins` folder

3. Start the server or proxy once to generate the files.

4. Open `config.yml` and enter your MySQL connection data.

5. Restart the server/proxy or run `/adminpunish reload`.

## Requirements

- Java 17 or newer bytecode support
- Maven for building from source
- MySQL database
- A server or proxy running one of the supported platforms

Use the Java runtime required by your server software. The plugin itself is compiled for Java 17 bytecode so it can also load on older 1.20.x server lines that still run on Java 17, while newer server versions can run it on their newer Java runtime. Minecraft Java 26.1.x servers require Java 25; the plugin can run on that newer runtime because Java runtimes are backward-compatible with Java 17 bytecode.

The plugin automatically creates these MySQL tables when the connection works:

- `bans`
- `punishment_history`
- `player_registry`

## Files

### `config.yml`

Contains technical settings:

- Prefix and ban title
- Ban/unban broadcast prefixes
- MySQL host, port, database, user, password, and JDBC parameters

### `messages.yml`

Contains all user-facing and gameplay settings:

- Active language selection
- Command names
- Subcommand names
- Messages
- Lookup formats
- Ban screen format
- Broadcast formats
- Duration labels
- Ban reasons
- Whitelist

Language is selected at the top of `messages.yml`:

```yaml
settings:
  language: "en"
```

Available built-in values:

- `en` for English
- `de` for German

English is the default. The language presets are stored under the `languages:` section, so server owners can edit both English and German text in one file.

### `bannedplayers.json`

Stores additional local ban history/display data in the plugin data folder. Active bans are still stored in MySQL.

## Commands

Command names can be changed in `messages.yml`. These are the default commands:

| Command | Usage | Description |
| --- | --- | --- |
| `/punish <player/uuid> <reason>` | Ban a player | Uses a configured ban reason or the fallback reason |
| `/unpunish <player/uuid>` | Unban a player | Removes the active ban from MySQL |
| `/lookup <player/uuid>` | Show ban information | Shows status, counts, reasons, executors, unbans, and dates |
| `/lookupremove <player/uuid>` | List stored ban reasons | Shows numbered reasons that can be removed |
| `/lookupremove <player/uuid> <number/reason>` | Remove one stored ban reason | Updates history and the active reason if needed |
| `/lookupfullremove <player/uuid>` | Delete full lookup history | Removes all punishment history entries for the player |
| `/punishment listallbans` | List active bans | Shows all currently banned players from MySQL |
| `/punishment reload` | Reload config | Reloads `config.yml` and `messages.yml` |
| `/punishment whitelist add <player>` | Add whitelist entry | The player can no longer be banned |
| `/punishment whitelist remove <player>` | Remove whitelist entry | The player can be banned again |
| `/adminpunish help` | Show help | Shows the admin help from `messages.yml` |
| `/adminpunish reload` | Reload config | Reloads `config.yml` and `messages.yml` |

## Permissions

| Permission | Description |
| --- | --- |
| `punishsystem.admin.use` | Allows use of admin, ban, unban, lookup, and management commands |
| `punishsystem.admin.seebans` | Receives ban and unban broadcasts |

The console can run management commands without player permissions.

## Ban Reasons And Durations

Ban reasons are configured in `messages.yml` under `ban-reasons`:

```yaml
ban-reasons:
  hacking:
    display: "&cHacking"
    duration: "7d"
  hausverbot:
    display: "&4HAUSVERBOT"
    duration: "permanent"
```

Supported duration values:

- `ms` for milliseconds
- `s` for seconds
- `m` for minutes
- `h` for hours
- `d` for days
- `permanent`, `perm`, `dauerhaft`, or `-1` for permanent bans

Examples:

- `15m`
- `2h`
- `7d`
- `permanent`

If a reason does not exist in `ban-reasons`, `default-ban-reason` is used.

## Whitelist

Whitelisted players cannot be banned. Entries can be names or UUIDs.

In `messages.yml`:

```yaml
whitelist:
  - Julian1657
```

With commands:

```text
/punishment whitelist add <player>
/punishment whitelist remove <player>
```

## Platform Notes

- On Velocity, BungeeCord, and Waterfall, the plugin runs as a proxy plugin.
- On Spigot, Paper, Purpur, Folia, Leaf, and Bukkit-compatible forks, the plugin runs as a server plugin.
- For Bukkit-based non-proxy servers, the supported Minecraft target lines are `1.20.x`, `1.21.x`, and `26.1.x`.
- On Sponge, the plugin runs as a Sponge plugin with Sponge metadata.
- For network-wide bans, use the same MySQL database on all instances.
- If the plugin runs on multiple servers/proxies at the same time, all instances read and write the same MySQL bans.

## Build

```powershell
mvn clean package
```

Output:

```text
target/proxybansystem-4.3.jar
```

The JAR contains loader files for all supported platforms:

- `velocity-plugin.json`
- `bungee.yml`
- `plugin.yml`
- `META-INF/sponge_plugins.json`
