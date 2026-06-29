# ProxyBanSystem

Source-Code for Ultimate Ban System FREE.

ProxyBanSystem ist ein plattformübergreifendes Ban- und Punishment-System für Minecraft-Netzwerke. Die gleiche gebaute JAR kann auf Proxy- und Server-Plattformen genutzt werden und verwendet eine gemeinsame MySQL-Datenbank, damit Bans, Lookup-Daten und registrierte Spielernamen zentral gespeichert werden.

## Kompatibilität

Eine JAR ist für diese Plattformen gedacht:

- Velocity
- BungeeCord
- Waterfall
- Spigot
- Paper
- Folia
- Leaf
- Sponge

Waterfall nutzt den BungeeCord-Loader. Spigot, Paper, Folia und Leaf nutzen den Bukkit/Spigot-Loader. Folia wird über `folia-supported: true` in der `plugin.yml` markiert.

## Funktionen

- Spieler per Name oder UUID bannen
- Spieler per Name oder UUID entbannen
- Automatischer Login-Check: gebannte Spieler werden beim Join abgelehnt
- Automatischer Kick eines online gebannten Spielers
- Permanente und zeitlich begrenzte Bans
- Frei konfigurierbare Ban-Gründe mit eigener Dauer
- Fallback-Ban-Grund für nicht vordefinierte Gründe
- Ban-Screen und Nachrichten vollständig über `messages.yml` anpassbar
- Ban- und Unban-Broadcasts für Teammitglieder mit Berechtigung
- Lookup-System für Ban-Status, UUID, Ban-Anzahl, Entban-Anzahl, Gründe und Verlauf
- Einzelne Ban-Gründe aus dem Lookup-Verlauf entfernen
- Kompletten Lookup-Verlauf eines Spielers entfernen
- Aktive Bans auflisten
- Whitelist für Spieler, die nicht gebannt werden können
- Spieler-Registry in MySQL für Offline-Lookups und Tab-Complete
- Lokale `bannedplayers.json` als zusätzliche gespeicherte Verlauf-/Anzeige-Datei
- Reload-Befehle für Config und Messages
- Tab-Complete für Spieler, Ban-Gründe und Verwaltungsbefehle
- Eine gemeinsame Core-Logik für alle unterstützten Plattformen

## Installation

1. Projekt bauen:

```powershell
mvn clean package
```

2. Die fertige JAR aus `target/proxybansystem-4.2.jar` in den passenden Plugin-Ordner legen:

- Velocity: Proxy-`plugins`-Ordner
- BungeeCord/Waterfall: Proxy-`plugins`-Ordner
- Spigot/Paper/Folia/Leaf: Server-`plugins`-Ordner
- Sponge: Sponge-`plugins`-Ordner

3. Server oder Proxy einmal starten, damit die Dateien erstellt werden.

4. In `config.yml` die MySQL-Zugangsdaten eintragen.

5. Server oder Proxy neu starten oder `/adminpunish reload` nutzen.

## Voraussetzungen

- Java 21
- Maven zum Bauen
- MySQL-Datenbank
- Ein Server/Proxy auf einer der unterstützten Plattformen

Die Tabellen werden automatisch erstellt, wenn die MySQL-Verbindung funktioniert:

- `bans`
- `punishment_history`
- `player_registry`

## Dateien

### `config.yml`

Enthält technische Einstellungen:

- Prefix und Titel
- Ban-/Unban-Broadcast-Prefix
- MySQL-Host, Port, Datenbank, Benutzer, Passwort und JDBC-Parameter

### `messages.yml`

Enthält alles, was im Spiel angezeigt oder genutzt wird:

- Command-Namen
- Unterbefehle
- Nachrichten
- Lookup-Formate
- Ban-Screen
- Broadcast-Formate
- Zeitformatierung
- Ban-Gründe
- Whitelist

### `bannedplayers.json`

Speichert zusätzliche Ban-Verlaufsdaten lokal im Plugin-Datenordner. Die aktiven Bans liegen weiterhin in MySQL.

## Commands

Die Command-Namen sind in `messages.yml` konfigurierbar. Standardmäßig heißen sie:

| Command | Nutzung | Beschreibung |
| --- | --- | --- |
| `/punish <Spieler/UUID> <Grund>` | Bannt einen Spieler | Nutzt einen konfigurierten Ban-Grund oder den Fallback-Grund |
| `/unpunish <Spieler/UUID>` | Entbannt einen Spieler | Entfernt den aktiven Ban aus MySQL |
| `/lookup <Spieler/UUID>` | Zeigt Ban-Infos | Status, Ban-Anzahl, Gründe, Banner, Entbanns und Zeiten |
| `/lookupremove <Spieler/UUID>` | Listet gespeicherte Ban-Gründe | Zeigt nummerierte Gründe zum Entfernen |
| `/lookupremove <Spieler/UUID> <Nummer/Grund>` | Entfernt einen einzelnen Ban-Grund | Aktualisiert Verlauf und aktiven Grund, falls nötig |
| `/lookupfullremove <Spieler/UUID>` | Löscht den kompletten Lookup-Verlauf | Entfernt alle History-Einträge des Spielers |
| `/punishment listallbans` | Listet aktive Bans | Zeigt alle aktuell gebannten Spieler aus MySQL |
| `/punishment reload` | Lädt Config neu | Lädt `config.yml` und `messages.yml` neu |
| `/punishment whitelist add <Spieler>` | Fügt Whitelist-Eintrag hinzu | Spieler kann danach nicht mehr gebannt werden |
| `/punishment whitelist remove <Spieler>` | Entfernt Whitelist-Eintrag | Spieler kann danach wieder gebannt werden |
| `/adminpunish help` | Zeigt Hilfe | Zeigt Admin-Hilfe aus `messages.yml` |
| `/adminpunish reload` | Lädt Config neu | Lädt `config.yml` und `messages.yml` neu |

## Berechtigungen

| Permission | Zweck |
| --- | --- |
| `punishsystem.admin.use` | Erlaubt Nutzung der Admin-, Ban-, Unban-, Lookup- und Verwaltungsbefehle |
| `punishsystem.admin.seebans` | Empfängt Ban- und Unban-Broadcasts |

Konsole darf die Verwaltungsbefehle ohne Spieler-Permission ausführen.

## Ban-Gründe und Zeiten

Ban-Gründe werden in `messages.yml` unter `ban-reasons` konfiguriert:

```yaml
ban-reasons:
  hacking:
    display: "&cHacking"
    duration: "7d"
  hausverbot:
    display: "&4HAUSVERBOT"
    duration: "permanent"
```

Unterstützte Zeitwerte:

- `ms` für Millisekunden
- `s` für Sekunden
- `m` für Minuten
- `h` für Stunden
- `d` für Tage
- `permanent`, `perm`, `dauerhaft` oder `-1` für permanente Bans

Beispiele:

- `15m`
- `2h`
- `7d`
- `permanent`

Wenn ein Grund nicht in `ban-reasons` existiert, wird `default-ban-reason` genutzt.

## Whitelist

Spieler auf der Whitelist können nicht gebannt werden. Einträge können Namen oder UUIDs sein.

In `messages.yml`:

```yaml
whitelist:
  - Julian1657
```

Per Command:

```text
/punishment whitelist add <Spieler>
/punishment whitelist remove <Spieler>
```

## Plattform-Hinweise

- Auf Velocity, BungeeCord und Waterfall arbeitet das Plugin als Proxy-Plugin.
- Auf Spigot, Paper, Folia und Leaf arbeitet es als Server-Plugin.
- Auf Sponge arbeitet es als Sponge-Plugin mit eigener Sponge-Metadata.
- Für netzwerkweite Bans sollte dieselbe MySQL-Datenbank auf allen Instanzen genutzt werden.
- Wenn das Plugin auf mehreren Servern/Proxys gleichzeitig läuft, greifen alle auf dieselben MySQL-Bans zu.

## Build

```powershell
mvn clean package
```

Ausgabe:

```text
target/proxybansystem-4.2.jar
```

Die JAR enthält die Loader-Dateien für alle unterstützten Plattformen:

- `velocity-plugin.json`
- `bungee.yml`
- `plugin.yml`
- `META-INF/sponge_plugins.json`
