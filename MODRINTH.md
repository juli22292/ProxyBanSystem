# ProxyBanSystem

ProxyBanSystem is a cross-platform MySQL ban and punishment system for Minecraft servers and proxy networks. One JAR supports proxy platforms and non-proxy server platforms, while all bans, lookup data, and registered player names are stored in one shared MySQL database.

## Compatibility

One JAR supports:

- Velocity
- BungeeCord
- Waterfall
- Spigot
- Paper
- Purpur
- Folia
- Leaf
- Bukkit-compatible forks
- Sponge

Non-proxy Bukkit-based server support:

- Minecraft Java 1.20.x
- Minecraft Java 1.21.x
- Minecraft Java 26.1.x

The Bukkit loader uses `api-version: 1.20` and is built against Spigot API 1.20.1 so the same JAR can run on 1.20.x and newer Bukkit-compatible platforms, including Paper, Purpur, Folia, and Leaf. Sponge support uses SpongeAPI 14.0.0.

## Features

- Ban and unban players by name or UUID
- Temporary and permanent bans
- Automatic login checks for banned players
- Automatic kick when an online player is banned
- Configurable ban reasons and durations
- Custom ban screen, messages, and broadcast formats
- English and German language presets, with English as the default
- Staff broadcasts for bans and unbans
- Lookup command with ban status, UUID, ban count, unban count, reasons, executors, and history
- Remove one stored lookup reason or delete the full lookup history
- List all active bans
- Whitelist players so they cannot be banned
- MySQL-backed player registry for offline lookups and tab completion
- Shared core logic for all supported platforms

## Requirements

- Java 17 or newer bytecode support
- MySQL database
- A supported server or proxy platform

Use the Java runtime required by your server software. The plugin itself is compiled for Java 17 bytecode, so it can also load on older 1.20.x server lines that still run on Java 17. Minecraft Java 26.1.x servers require Java 25; the plugin can run on that newer runtime because Java runtimes are backward-compatible with Java 17 bytecode.

## Installation

1. Download or build the JAR.
2. Put the JAR into the correct `plugins` folder.
3. Start the server or proxy once.
4. Edit `config.yml` and enter your MySQL connection data.
5. Restart the server/proxy or run `/adminpunish reload`.

Plugin folders:

- Velocity: proxy `plugins` folder
- BungeeCord/Waterfall: proxy `plugins` folder
- Spigot/Paper/Purpur/Folia/Leaf/Bukkit forks: server `plugins` folder
- Sponge: Sponge `plugins` folder

## Commands

- `/punish <player/uuid> <reason>` - ban a player
- `/unpunish <player/uuid>` - unban a player
- `/lookup <player/uuid>` - show ban and history information
- `/lookupremove <player/uuid>` - list stored ban reasons
- `/lookupremove <player/uuid> <number/reason>` - remove one stored reason
- `/lookupfullremove <player/uuid>` - delete full lookup history
- `/punishment listallbans` - list all active bans
- `/punishment reload` - reload config and messages
- `/punishment whitelist add <player>` - add whitelist entry
- `/punishment whitelist remove <player>` - remove whitelist entry
- `/adminpunish help` - show admin help
- `/adminpunish reload` - reload config and messages

Command names can be changed in `messages.yml`.

## Permissions

- `punishsystem.admin.use` - allows admin, ban, unban, lookup, and management commands
- `punishsystem.admin.seebans` - receives ban and unban broadcasts

## Language

English is the default language. You can switch to German in `messages.yml`:

```yaml
settings:
  language: "de"
```

Use `en` for English or `de` for German.

## Network Usage

For network-wide bans, install the plugin on every proxy/server that should enforce bans and point all instances to the same MySQL database.
