# PanelAutoStarter

A BungeeCord and Velocity plugin that automatically starts and stops your Minecraft servers based on player count, through your panel's API.

Works with both **Pterodactyl** and **Pelican**.

> Coming from PterodactylAutoStarter 2.x? The migration steps are in the [3.0.0 release notes](https://github.com/FarmVivi/panel-auto-starter/releases) — note that the data folder is renamed.

## Features

- Automatically starts a server when a player tries to join it
- Automatically stops idle servers
- Queue system that holds players while the server boots, with an on-screen countdown before they are moved
- Server list ping that reuses each server's own MOTD and favicon, decorated with a status badge when it is offline or starting
- Works on BungeeCord and Velocity

## Compatibility

| Platform | Minimum version |
|---|---|
| Panel | Pterodactyl, Pelican |
| BungeeCord | 1.21+, Java 21 or later |
| Velocity | 4.0+, Java 25 or later (required by Velocity 4 itself) |

## Installation

1. Download the jar matching your proxy:
   - `panel-auto-starter-bungee-<version>.jar` for BungeeCord
   - `panel-auto-starter-velocity-<version>.jar` for Velocity
2. Drop the `.jar` into your `plugins` folder
3. Start the proxy once to generate the configuration
4. Edit the generated `config.yml`, then restart the proxy

The configuration is generated in `plugins/PanelAutoStarter/` on BungeeCord and in `plugins/panelautostarter/` on Velocity.

## Configuration

```yaml
panel:
  # pterodactyl or pelican
  type: pterodactyl
  url: https://panel.example.com
  token: ptlc_xxxxxxxx

queue:
  # Fallback server where players wait while the target server boots
  server: lobby

servers:
  lobby:
    id: server_id_from_your_panel
```

### API token

It must be a **client** API token, created from your own user account — not an application API token.

| Panel | Expected prefix | Total length |
|---|---|---|
| Pterodactyl | `ptlc_` | 48 |
| Pelican | `pacc_` | 48 |

The full token is shown **only once**, right after you create the key. The API keys list only ever shows its identifier, which is the first 16 characters.

> **Known Pelican pitfall:** panels older than `1.0.0-beta15` never display the full token, not even on creation ([issue #768](https://github.com/pelican-dev/panel/issues/768)). A 16-character token results in a `401 Unauthenticated`. Either update your panel, or recover the secret from the database:
>
> ```bash
> php artisan tinker
> ```
> ```php
> $k = \App\Models\ApiKey::latest()->first();
> echo $k->identifier . $k->token . PHP_EOL;
> ```

Each server's `id` is its **short** identifier as displayed by the panel (8 hexadecimal characters), not its full UUID.

**Important:** the server name (e.g. `lobby`) must exactly match the name declared in your proxy's own configuration (BungeeCord or Velocity). That is what links the proxy configuration to the panel servers.

### Velocity proxy configuration

Set `ping-passthrough` to `disabled` in `velocity.toml`:

```toml
ping-passthrough = "disabled"
```

The plugin takes care of forwarding each backend's MOTD on a per-host basis. Velocity itself cannot tell its own address apart from a forced host: set to `"all"`, it serves the MOTD of the first server in the `try` list — usually the limbo — even when a player pings the proxy address.

With `disabled`, everyone gets their own MOTD back: the proxy on its address, each backend on its forced host, and the plugin's "offline" / "starting" screens when the server is unavailable.

Side benefit: Velocity no longer contacts a backend on every client ping, since the plugin serves from its cache.

### When a server stops under players' feet

```yaml
queue:
  catch-kicks: true
```

Without this, what happens to the players depends on *how* the backend closed the connection: an abrupt drop triggers the proxy's own failover, a clean disconnect lets it kick them out. That is why a stopping server sometimes sent players back to the lobby and sometimes disconnected them outright.

The plugin knows which servers it manages and where players wait, so it settles the question.

**A deliberate kick is not caught.** Someone an admin threw out stays out, with their reason — only a server that is going away sends players back. The plugin tells the two apart from three signals, most reliable first:

1. it knows it asked for the shutdown, or the server has stopped answering;
2. the connection was cut without a word, which is what a crash looks like;
3. the disconnect reason matches `queue.shutdown-reasons` — a stop launched from the panel announces `Server closed` before the watchdog has noticed anything, so that list is the only clue available at that moment. Adjust it to your server's language and plugins.

Servers the plugin does not manage are left entirely to the proxy.

On arrival the player gets a title, a chat line naming both servers, and a low tone — so being yanked out of a world is explained rather than merely survived:

```yaml
queue:
  kick-feedback:
    enabled: true
    sound:
      name: block.note_block.bass
      volume: 1.0
      pitch: 0.8
```

Like the queue notice, this is held until the player actually lands: they are mid-transfer when the kick happens, and anything sent then is lost.

### Joining a stopped server

When a player is put in the queue they get a title on screen, and once the
server is ready a countdown runs before anyone is moved.

```yaml
queue:
  server: lobby

  actionbar:                              # while the player waits
    enabled: true
    interval: 1000                        # milliseconds, clamped to 250–3000
    show-position: true

  title:
    enabled: true
    fade-in: 300      # milliseconds
    stay: 2500
    fade-out: 500

    sound:                                # accompanies the title
      name: block.note_block.bell
      volume: 1.0
      pitch: 1.0

  countdown:
    enabled: true
    seconds: 3
    tick-sound:                           # once per second
      name: block.note_block.hat
      volume: 0.5
      pitch: 1.0
    go-sound:                             # when players are moved
      name: entity.experience_orb.pickup
      volume: 0.7
      pitch: 1.2
```

A line sits above the hotbar for as long as the player waits, naming the server, its state and the player's place in the queue:

```
●○○ Survie  ·  démarrage  ·  2/5
```

The indicator turns on each refresh — the one thing static text cannot convey is that the plugin is still working. The line is re-sent rather than sent once, because the client clears the action bar on its own after a few seconds. It steps aside during the countdown, where the title already says the same thing in bigger letters, and it does not tell a lone player they are `1/1`.

Unlike sounds, **the action bar works on both proxies**: it is a variant of the chat packet, which BungeeCord can build.

**A player only ever waits for one server.** Asking for another one removes them from the previous queue — otherwise they stayed announced on queues they had abandoned, skewing the positions shown to everyone else.

Leave a `name` empty to drop that sound. The short form `sound: block.note_block.bell` also works, keeping the default volume and pitch.

Countdown volumes default below `1.0` on purpose: a sound repeated every second gets grating at full blast.

Inherent loudness varies wildly between Minecraft sounds — some, like `block.amethyst_block.chime`, stay inaudible even at full volume. If subtitles show the sound but you hear nothing, pick a different one rather than raising the volume.

The countdown adds to `server-start.wait-before-teleport`: the plugin first
waits that grace period silently, letting the server settle, then counts down on
screen. Set `seconds: 0` or `enabled: false` to move players as soon as the
server is ready.

> **Sounds have two limits.** They are **Velocity-only**: BungeeCord exposes no
> sound API at all — the proxy forwards packets rather than producing them — so
> the sound settings have no effect there, and the plugin says so on startup
> rather than letting you wonder. They also require a **1.19.3+ client**: below
> that Velocity cannot emit the sound and drops it silently. Titles and the
> countdown always show on screen regardless.

Players are then moved one at a time, `server-start.teleport-delay` seconds
apart, so a full queue does not land on a freshly booted server all at once.

### Offline and starting MOTD

Every field of the server list entry is configurable on its own, and separately for the offline and the starting state. An unknown value never stops the plugin from loading: the default applies and a warning is logged.

```yaml
motd:
  offline:
    description: cached      # cached | plugin | proxy
    favicon: cached          # cached | bundled | proxy
    favicon-grayscale: true
    favicon-badge: true
    players: zero            # zero | queue | proxy
    max-players: zero        # zero | cached | proxy
    hover: plugin            # plugin | none | proxy
    version-label: none      # none | text
  starting:
    description: cached
    favicon: cached
    favicon-grayscale: false
    favicon-badge: true
    players: zero
    max-players: zero
    hover: plugin
    version-label: none
```

| Value | Meaning |
|---|---|
| `cached` | what the server itself showed, remembered from its last time online |
| `plugin` | text or icon generated by the plugin, announcing the state |
| `bundled` | the generic icon shipped with the plugin |
| `proxy` | leave the proxy's own response untouched |
| `zero` / `queue` | a player count of 0, or the number of players waiting in the queue |

Whenever a server is seen online, its MOTD, favicon and player cap are cached on disk under `cache/` in the plugin folder — one `<server>.json` and one `<server>.png` per server. Because that cache lives on disk rather than in memory, entries look right immediately after a proxy restart, even with every backend still down. It is written only when the MOTD actually changes; delete a pair of files to make the plugin relearn a server.

The description is stored in Adventure's JSON format, so gradients and hex colours survive the round trip.

**One caveat worth knowing:** `version-label` and the player count occupy the same slot in the client. Setting it to `text` shows a red label there — the 2.x behaviour — which makes `players` and `max-players` invisible.

`favicon-grayscale` and `favicon-badge` only apply to `favicon: cached`; the shipped icons already carry their own indicator.

A server that has never been seen online has nothing cached, so `description: cached` and `favicon: cached` leave the proxy's response alone.

#### Useful combinations

| Goal | Settings |
|---|---|
| Default | the server keeps its identity, state reads from the icon and the hover |
| Like 2.x | `description: plugin`, `favicon: bundled`, `version-label: text` |
| Subtle | `favicon-badge: false`, `favicon-grayscale: true` |
| Change nothing | every field on `proxy`, `version-label: none` |

### Menus

```yaml
menu:
  enabled: true
  renderer: auto          # auto | chest | chat
  command: servers        # opens the server list
  server-command: server  # the proxy command a click runs
```

`/servers` lists the managed servers with their state and player count, one clickable line each; `/pas menu` does the same for administration, with start, stop and whitelist buttons.

**Every menu action is a command**, never a callback. That is what lets a chat renderer and an inventory renderer coexist — a chat click can only carry a command — and it means a menu can do nothing its user could not have typed, so every action goes through the same checks.

**The selection menu is deliberately dumb.** It lists servers and forwards the request exactly as a hand-typed `/server` would, and does **not** filter by permission — the connection event decides, and it alone. Filtering there would protect nothing, since the command stays open, while looking like protection: the worst of both, because you then stop checking where it counts. Someone clicking a server they may not enter gets the same reasoned refusal they would have got from the command.

The administration menu *does* filter, and that is not a contradiction: it would otherwise reflect servers its reader cannot touch, announcing infrastructure that is none of their business.

**The chest renderer needs [PacketEvents](https://github.com/retrooper/packetevents) on the proxy, and is optional.** Without it the plugin starts normally and menus render in chat; the startup log says which one is in use. It also declines for clients older than 1.20.5, whose item format predates data components.

It is worth knowing what that renderer actually does: the proxy opens a window the backend server knows nothing about. The client believes it is real and will send its clicks and its close to the backend, which would answer with a resynchronisation — a flickering inventory at best. Packets carrying the plugin's window id are therefore intercepted and **never forwarded**. The window id sits well above the range servers allocate, so no real container's click can be mistaken for ours.

Renderers are tried richest-first and the chat one always closes the march — it depends on nothing and cannot decline, so a menu always opens. Naming a renderer explicitly is a requirement rather than a hint: asking for `chat` forbids opening anything else. Only `auto` lets the plugin choose. That fallback is what makes a packet-based renderer adoptable without risk: if it stops working — a library lagging behind the game version, a client too new — menus render in chat instead of vanishing.

### Access control and administration

Three doors must open before a player enters a managed server, and they are checked **on the connection event** — never in a selection menu. A menu only asks for a server, exactly as `/server` does; filtering there would let the command, a forced host and other plugins' redirects straight through while looking like protection.

```yaml
access:
  enabled: true
  require-permission: false             # see below
  respect-proxy-restrictions: true      # BungeeCord's own `restricted` flag
  permission-format: "panelautostarter.join.%s"
```

| Node | Grants |
|---|---|
| `panelautostarter.join.<server>` | entering that server |
| `panelautostarter.admin` | administering every managed server |
| `panelautostarter.admin.<server>` | administering that one server |

Server names are lowercased in permission nodes, so a server called `Survie` yields `panelautostarter.join.survie` rather than a node nobody could grant.

**Nothing to integrate on the LuckPerms side.** The plugin asks the proxy's permission API, which LuckPerms hooks into — asking the proxy *is* asking LuckPerms.

`require-permission` is `false` by default on purpose: turning it on out of the box would shut every server of an existing install on upgrade, since nobody has been granted anything yet. Grant the nodes first, then flip it.

On BungeeCord a server marked `restricted` already requires `bungeecord.server.<name>`, and `ServerInfo.canAccess` answers the question — the plugin reuses it rather than asking you to declare the same restriction twice. Velocity has no equivalent (`RegisteredServer` has no notion of permission), so that setting has no effect there; the plugin's own node works on both.

**Whitelists live in `whitelist.yml`**, next to `config.yml` — which the plugin never rewrites, so your comments and formatting survive. Players are keyed by UUID, with the username kept alongside for readability: a username can change, and letting the new bearer of an old name in would defeat the point.

A permission and a whitelist answer different questions and do not replace each other: the permission says **which group** may enter and is managed in LuckPerms, the whitelist says **which people** and is managed in game. Holding the permission does not exempt from the whitelist, nor the reverse.

```
/pas status [server]                     state of the servers you administer
/pas start <server>
/pas stop <server>
/pas whitelist <server>                  show the list
/pas whitelist <server> on|off
/pas whitelist <server> add|remove <player>
```

The command only ever shows and completes the servers its author administers. A player can be added by name while online, or by UUID; removal also accepts the name stored in the list, so somebody can be removed while offline — which is the actual need.

### Startup tuning

```yaml
server-start:
  # How often the server state is checked, in seconds
  check-interval-normal: 15
  # Same, but while a server is booting (tighter polling)
  check-interval-startup: 3
  # Grace period before teleporting players once the server is online
  wait-before-teleport: 5
  # Delay between two teleports, to avoid overloading the server
  teleport-delay: 1
  # How often an offline server that nobody pings is checked
  check-interval-idle: 60
  # Time without any client ping before falling back to check-interval-idle
  idle-threshold: 300
  # How long a cached ping stays valid before the MOTD refreshes it
  ping-cache-ttl: 5
  # How long to wait for a watchdog ping before giving up on it
  ping-timeout: 3
  # Require the panel to report RUNNING before announcing a server online
  use-panel-state: true
  # How often the panel is asked while a server is believed online
  panel-state-interval: 30
```

The plugin does not merely poll servers at a fixed rate: a client ping on a stale cache triggers a background refresh, without ever delaying the response sent to the player. Conversely, an offline server that nobody is watching is polled less often. The defaults are fine for most setups.

**The polling cadence does not depend on how fast a server answers.** A booting server accepts the connection — the container publishes the port before the game listens — without replying, so the ping hangs until the proxy's own read timeout, around thirty seconds. Since the next round is armed at the start of a cycle rather than when a reply arrives, `check-interval-startup: 3` means three seconds and not thirty-three. A ping that stays unanswered for `ping-timeout` is dropped; its slot is released, but **nothing is reported to the state machine** — silence does not prove a server is gone, and treating it as such would empty the queue of a merely slow server.

**Going online needs both signals.** A successful ping proves the game listens, not that the panel considers the boot finished — players teleported in that window land on a server that is not ready to take them. So the panel must report `RUNNING` before a server is announced online. If it stays unreachable for a minute the ping takes over: blocking every start on a panel outage would be worse than the problem this solves.

**The panel is asked when its answer matters, not on a timer.** It is queried at the decision point — confirming a start, throttled so a boot does not hammer it — and then, while a server is believed online, every `panel-state-interval` seconds. An idle offline server costs no API call at all.

**The panel is the surer source for noticing a server has disappeared.** It *knows* the server is stopped, usually because it stopped it, where a ping has to wait for a connection to expire. Without `use-panel-state`, a stopped server stays believed online for several polls, during which players are neither queued nor followed by a restart — they run into a connection timeout instead. The converse does not hold: a panel reporting the server as running does not mean the game accepts connections, so **going online stays decided by the ping alone**. A panel reporting it stopped is likewise ignored while a start is under way, since the panel briefly reports the server down before the container comes up.

It costs one panel API call per server per poll. Turn it off if your panel is slow or you manage many servers.

## Architecture

Multi-module Maven project:

| Module | Role |
|---|---|
| `panel-auto-starter-common` | Business logic, panel abstraction, configuration loading |
| `panel-auto-starter-bungee` | BungeeCord implementation |
| `panel-auto-starter-velocity` | Velocity implementation |

### Supporting both panels

Panel access goes through the `PanelClient` and `PanelServer` interfaces (package `fr.farmvivi.panelautostarter.panel`), which expose **no type** from the underlying library.

Because Pelican's client API is deliberately kept compatible with Pterodactyl's, a single implementation — backed by [Pterodactyl4J](https://github.com/mattmalec/Pterodactyl4J) — serves both panels today. `PelicanPanelClient` extends the Pterodactyl implementation without overriding anything; it exists as the extension point for the day Pelican diverges on an endpoint.

## Building

```bash
mvn clean package
```

**JDK 25 is required to build.** `velocity-api` 4.0.0 ships as Java 25 bytecode (class file 69), which an older JDK cannot read at all. This is not a project decision, it is a Velocity 4 constraint.

The produced bytecode targets Java 21, which keeps the BungeeCord jar loadable on a Java 21 server. Velocity 4 servers run on Java 25 anyway.

Jars are produced in `panel-auto-starter-bungee/target/` and `panel-auto-starter-velocity/target/`.

## License

Maintained by FarmVivi
