# AGENTS.md

Instructions for AI coding agents working on this repository. Written for the
[AGENTS.md convention](https://agents.md), read by Codex, Claude Code (via
`CLAUDE.md`), and other agent tools.

## What this project is

A BungeeCord and Velocity plugin that starts and stops Minecraft servers on
demand through a game panel's API, so idle servers cost nothing. It supports
both **Pterodactyl** and **Pelican**.

Multi-module Maven project:

| Module | Role |
|---|---|
| `panel-auto-starter-common` | Business logic, panel abstraction, config, MOTD. All tests live here. |
| `panel-auto-starter-bungee` | BungeeCord implementation |
| `panel-auto-starter-velocity` | Velocity implementation |

The two platform modules contain only thin adapters. Anything reusable belongs
in `common`.

## Building

**JDK 25 is required to build.** `velocity-api` 4.0.0 ships as Java 25 bytecode
(class file 69); an older JDK cannot read its classes at all and the build fails
with a confusing `package ServerPing does not exist`. This is a Velocity 4
constraint, not a project choice.

```bash
mvn clean verify -P release      # full build, tests, sources, javadoc
mvn clean test                   # tests only
mvn -q test -Dtest=MotdStoreTest -pl panel-auto-starter-common   # one class
```

The produced bytecode targets **Java 21** (`maven.compiler.release`), which
keeps the BungeeCord jar loadable on Java 21 servers. Do not raise this without
a reason: Velocity 4 servers run Java 25 anyway, but BungeeCord ones do not.

Jars land in `panel-auto-starter-{bungee,velocity}/target/`.

## Architectural invariants

These are not style preferences. Breaking one causes bugs that are painful to
diagnose, and each is guarded by a test.

**1. No panel library type crosses the abstraction.**
`PanelClient` and `PanelServer` (package `…panelautostarter.panel`) must never
expose `ClientServer`, `PteroClient`, `PteroAction` or `UtilizationState`.
Pterodactyl4J is confined to `panel/pterodactyl/`. This is what allows a future
Pelican divergence to be handled by overriding one method.

```bash
# Must only ever return files under panel/pterodactyl/
grep -rn "mattmalec" panel-auto-starter-common/src/main/java
```

**2. The presentation cache is separate from the state machine.**
`MinecraftServer` holds two distinct ping fields on purpose:

- `lastPolledPing` — the state machine's memory of the previous poll. Written
  **only** by the scheduler, which is its single writer.
- `cachedPing` — what gets served to client pings. Written by the scheduler and
  by on-demand refreshes.

Merging them makes the scheduler believe it has already seen a ping when a
client triggered a refresh, so the offline → online transition is missed and the
server stays `OFFLINE` forever.

**3. Never block the ping event path.**
`peekServerPing()` returns the cache immediately and refreshes in the
background. Waiting on the backend would add its latency to every client ping
and hand out a trivial abuse lever. An `AtomicBoolean` guards against a burst of
pings triggering a burst of backend requests.

**4. Nothing blocks the proxy's network thread.**
Ping callbacks run on Velocity's Netty event loop. `PanelServer.retrieveState()`
is a blocking HTTP call and was once made from there, freezing the event loop
for a panel round-trip. Panel calls go through `CommonProxy.runAsync`; the
watchdog reads a cached `lastPanelState` instead.

**5. The watchdog cadence is independent of how fast a server answers.**
The next round is armed at the *start* of a cycle, never from the ping callback.
A booting server accepts the connection without replying, so the ping hangs
until the proxy's read timeout — chaining the reschedule to the reply turned
`check-interval-startup: 3` into thirty-three seconds. A poll left unanswered
for `ping-timeout` is dropped and its slot released, but **nothing is reported
to the state machine**: silence is not proof a server is gone. A generation
counter tells a late reply apart from its successor's slot.

**6. Going online needs both the ping and the panel; going offline needs either.**
A successful ping proves the game listens, not that the panel considers the
boot finished — players teleported in that window land on a server that is not
ready. So `RUNNING` is required to reach `ONLINE`, and an unconfirmed ping is
deliberately *not* stored in `lastPolledPing`, leaving the transition to be
retried. The converse is asymmetric on purpose: a panel reporting the server
stopped is believed at once, but it is ignored while a start is under way, when
the panel briefly reports the container down.

**7. The panel is asked when its answer matters, not on a timer.**
`retrieveState()` used to run every watchdog round — every three seconds during
a boot. It now runs at the decision point (confirming a start, throttled) and,
while a server is believed online, at `panel-state-interval`. An idle offline
server costs zero API calls. If the panel stays unreachable for a minute, the
ping takes over: blocking every start on a panel outage is worse than the bug
this guards against.

**8. Menu actions are commands, never callbacks.**
This is what lets a chat renderer and an inventory renderer coexist — a chat
click can only carry a command — and it means a menu can do nothing its user
could not have typed, so every action goes through the same checks. The
selection menu is deliberately dumb and does **not** filter by permission; the
connection event decides. Filtering there would protect nothing (the command
stays open) while looking like protection.

**9. Player-facing text is a translatable component, never a literal.**
Messages stay *unresolved* until handed to someone, which is the only way two
players on one server can read two languages. Rendering happens in
`CommonPlayer` implementations against `getLocale()`. Style stays in code —
colours are design and identical in every language — and each sentence is one
key with `{0}` arguments, never fragments joined at runtime: word order differs
between languages.

Three traps, all already paid for. `MessageFormat` eats single quotes unless
doubled, which only shows in French where they abound. `{0,choice,…}` does
**not** work here: Adventure passes arguments as components, not numbers, so
phrase counts as `Label: {0}` rather than relying on plural forms.

And the one that shipped broken: **anything that builds a packet bypasses the
send path entirely.** `menu/chest` hands components to PacketEvents — window
titles, item names, lore — where no `CommonPlayer` method ever runs, so nothing
renders them and players read raw keys. Every component leaving that package
must be passed through `Translations.render` with the viewer's locale. No test
guards this: PacketEvents is not initialised under test, so the whole renderer
is unreachable there. Read the code.

`TranslationsTest` guards the rest: every key used in the sources must exist,
and every bundle must carry the same keys with the same placeholders.

**10. The shipped `config.yml` must match the coded defaults.**
`MotdSettingsTest` loads the real resource and compares it to
`MotdSettings.defaults()`. Without it the file would silently advertise
behaviour the code does not implement.

## Conventions

**Language.** Code comments and Javadoc are in **French**. The README, release
notes and this file are in **English**. Keep it that way unless asked otherwise.

Player-facing text is no longer written in code at all: it is a translation key
resolved from `lang/`, where English is the fallback and French ships alongside
(invariant 9). Adding a message means adding a key to **both** bundles — a test
enforces it.

**Comments explain why, not what.** The codebase documents the reasoning behind
non-obvious choices — why a field exists, why an alternative was rejected. Match
that. Do not narrate what the next line does.

**Commit messages** follow Conventional Commits, with a body explaining the
reasoning and what was verified. Look at recent history for the tone.

**Tests are the specification.** JUnit 5 and Mockito. When adding a mechanism
whose failure would be silent, verify the test actually guards it by breaking
the code on purpose and checking the test fails. Several existing tests were
validated that way; the commit bodies say so.

## Working method

The discipline below was not chosen in the abstract — every line of it comes
from something that went wrong here.

**Verify a guard by breaking it.** A green suite proves nothing about a test
you just wrote. The procedure: copy the file aside, apply a mutation that
removes exactly the mechanism under test, run *only* the affected class, restore,
then `diff` against the copy to prove the restore worked. State the result in
the commit body: *"removing X kills N tests"*.

Two ways this has gone wrong, both silently:

- **A mutation that never applied.** A pattern that does not match leaves the
  file untouched, the suite green, and the conclusion false. Make the patch
  script exit non-zero when the pattern is absent, and read its output before
  trusting the test run.
- **A restore that never happened.** `cp a b || cp a c` runs the fallback only
  on failure; a Python script that `sys.exit`s before writing leaves the mutation
  in place. Always `diff` after restoring, and say so.

**A mutation that kills nothing is a finding, not a failure.** It once revealed
that `canAdminister` was never called — the per-server rights path was dead code
that no test could reach. Investigate before adjusting the mutation.

**Line endings are mixed.** Tracked files are CRLF; the Edit and Write tools
produce LF. Python patterns written with `\n` fail on old files and patterns
with `\r\n` fail on new ones — this has cost several wasted cycles. Prefer the
Edit tool, or match with `\r?\n`.

**Audit by sweeping, not by recalling.** Both incomplete internationalisation
passes came from checking the paths that came to mind instead of grepping the
whole tree. When a change is cross-cutting, enumerate every occurrence first and
work the list.

**Some code cannot be tested here.** `menu/chest` needs PacketEvents, which is
never initialised under test, so the whole renderer is unreachable. It shipped
broken once with the suite green. Where that is true, write the constraint into
the class Javadoc *and* an invariant above, and say plainly in the commit that
no test guards it.

**Commits are unsigned on this project** (`-c commit.gpgsign=false`): the SSH
signing key's passphrase cannot be supplied non-interactively.

**One concern per commit.** `git add -A` after finishing two things in the same
working tree merges them; the split afterwards is fiddly. Stage by path.

## Gotchas worth knowing

- **`ping-passthrough` must be `disabled`** in `velocity.toml`. The plugin
  serves backend MOTDs itself, per host. Velocity cannot tell its own address
  from a forced host, so `"all"` leaks the first `try` server's MOTD onto the
  proxy address.
- **`version-label` and the player count share one slot** in the client. Setting
  it to `text` hides the counter — that was the 2.x behaviour.
- **SnakeYAML is not transitive.** `bungeecord-config` declares it
  `<optional>true</optional>`. It is provided by the proxy at runtime and
  declared `test`-scoped here; without it `ConfigurationProvider.getProvider()`
  returns null.
- **Gson is `provided`**, supplied by both proxies, so no second copy is shaded
  in.
- **Pterodactyl4J cannot be updated by Dependabot** — see the comment in
  `.github/dependabot.yml`. Check for new versions by hand.
- **Pelican panels older than `1.0.0-beta15`** never display the full API token,
  only its 16-character identifier, which yields a `401`. See the README.
- **Mockito is attached as an explicit `-javaagent`**, wired through
  `maven-dependency-plugin`'s `properties` goal. Its self-attach stopped working
  once the classpath grew, and the JDK is removing that mechanism anyway. A
  consequence: **the first build needs network access** to fetch that plugin,
  even with `-o` everywhere else.
- **`PacketEvents` is `provided` and optional.** It must never be shaded; the
  presence check lives in `menu/PacketEventsSupport` and uses reflection so that
  loading it cannot fail on a proxy without the library.
- **A forked-VM crash usually means the machine is out of memory.** Surefire
  reports it as `Corrupted channel by directly writing to native stream … '#'` —
  that `#` is the JVM's crash-report header, not a test failure. Free memory and
  re-run rather than hunting a phantom bug.
- **`MessageFormat` and Adventure disagree about arguments.** Translation
  arguments arrive as components, not numbers, so `{0,choice,…}` silently does
  nothing. Phrase counts as `Label: {0}`.

## Releasing

The version on `master` stays a `-SNAPSHOT`; the release workflow strips it at
build time.

1. `mvn versions:set -DnewVersion=X.Y.Z-SNAPSHOT -DgenerateBackupPoms=false -DprocessAllModules`
2. Commit and push to `master`.
3. `gh release create vX.Y.Z --target master --title "vX.Y.Z - …" --notes-file …`

Tags are prefixed `v`; release titles read `vX.Y.Z - Short description`. The
`Release Build` workflow then attaches both jars automatically.

Release notes are written for server owners, not for the changelog: lead with
what changed for them, and spell out any manual migration step. Say explicitly
what does **not** change on upgrade — new behaviour that is off by default is
reassuring only if it is stated.

**Check which version is already released** before choosing the next number.
`git describe --tags --abbrev=0` and `git log <tag>..HEAD` settle it; assuming
from memory once nearly published a duplicate.

Before tagging, confirm the four things that a green suite does not:

```bash
javap -verbose -cp panel-auto-starter-velocity/target/classes \
  fr.farmvivi.panelautostarter.velocity.VelocityPlugin | grep major   # 65
grep -rn "mattmalec" panel-auto-starter-common/src/main/java | grep -v /panel/pterodactyl/
unzip -l panel-auto-starter-velocity/target/*-SNAPSHOT.jar | grep -c retrooper/packetevents  # 0
unzip -l panel-auto-starter-velocity/target/*-SNAPSHOT.jar | grep -c lang/messages           # 2
```
