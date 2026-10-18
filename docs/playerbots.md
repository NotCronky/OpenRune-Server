# Playerbots plan

Branch: `playerbots` on `NotCronky/OpenRune-Server` (fork of `OpenRune/OpenRune-Server`).

## Goal

Server-side bots that log in as real characters and play like people. Accounts are generated at
random, from fresh level-3 accounts to maxed mains. Bots train skills, do quests, kill bosses, PvP,
and later do minigames once those exist upstream. Real players should not be able to tell
easily which players are bots.

We start small: one bot standing in Lumbridge, visible to a real client. Each milestone below
builds on the previous one and should be playable on its own.

## Guiding decisions

- **Bots act through the real input path.** A bot does what a client does: it produces the
  same incoming game messages (`OpLocV2`, `OpNpc`, `MoveGameClick`, `ResumePauseButton`, …)
  and they go through the existing `MessageHandler`s in
  `api/net/.../rsprot/handlers/`. Every piece of content, including content added upstream
  later, then works for bots with no bot-specific glue, and bots get the same validation
  real players do. The test harness's `CaptureClient` (`api/testing/test-capture`) already shows
  the pattern: queue messages, consume them in `Client.read(player)`.
- **Bots read state server-side.** There's no client to render anything, so bot decisions come
  from the registries and player state directly: `player.ui` (open modals / dialogues),
  inventories, stats, `NpcRegistry`, `LocRegistry`, `PlayerRegistry`, coords.
- **Real accounts.** Bots are normal characters loaded and saved through `AccountManager`
  (`loadOrCreate` / `save`), so they persist and keep progressing between restarts.
- **Stay mergeable with upstream.** All bot code lives in a new `content/other/playerbots`
  module. Engine/api changes are kept minimal, upstream-friendly, and listed in
  [Upstream touch points](#upstream-touch-points).
- **One tick thread.** The bot brain runs on the game thread, but its work is spread out: a bot
  only re-decides every few ticks, and heavy work (path planning across the world) is cached or
  split over ticks.

## How a real player gets into the world (what we have to replicate)

From `AccountLoadResponseHook` (`api/net/.../rsprot/player/`) and `NetworkScript`:

1. `AccountManager` loads the account → `AccountLoadResponse.Ok`.
2. `createPlayer`: `Player()`, apply `response.transforms`, window status, `newAccount`,
   `worldType`; then `applyConfigTransforms` (spawn coord, xp rate, display name, dev-mode
   rights).
3. `accountRegistry.queueLogin(player, response, callback)` → on the game thread:
   `playerRegistry.nextFreeSlot()`, set `player.slotId`, publish `SessionStart` (which is where
   `NetworkScript` allocates `infoProtocols.alloc(slot, …)` and attaches `RspClient` /
   `RspCycle`), `playerRegistry.add(player)`, then publish `SessionStateEvent.Login` and
   `SessionStateEvent.EngineLogin`.
4. Each tick, `PlayerInputProcess` calls `client.read(player)`, and `PlayerPostTickProcess` calls
   `clientCycle.update` (pushes coords/appearance/anims/hits into the rsprot avatar so *other*
   players see you), then `clientCycle.flush` / `client.flush`.
5. Logout: `client.unregister` deallocates the info protocol (`RspClient.unregister`).

Bots skip the socket, login block, 2FA/trusted devices and central auth. Everything else stays
the same.

## Architecture

```
PopulationManager   how many bots online, who logs in/out and when (time-of-day curve)
   │
BotSpawner          creates new bot accounts: archetype → stats, gear, bank, quests, look, name
   │
BotLogin            loads the account through AccountManager and registers it the way a real login does
   │
BotBrain            personality + session plan; picks the next Goal
   │
Goals               TrainSkill, DoQuest, KillBoss, WildernessPk, Wander, Idle/AFK …
   │
Tasks               reusable steps: TravelTo, Bank(deposit/withdraw), KillNpc, InteractLoc,
   │                Eat, Loot, AnswerDialogue, Flee …
Actions             produce real incoming messages for BotClient (OpLoc, OpNpc, Move, Resume…)
   │
BotClient / BotCycle   Client + ClientCycle implementations with no socket
```

### BotClient / BotCycle

- `BotClient : Client<Any, Any>`. `read(player)` drains a queue of pending incoming messages
  into their handlers. `write()` drops outgoing messages (maybe watching a few, e.g. chatbox
  messages like "You need a higher level"). `unregister()` deallocates the info protocol.
- `BotCycle : ClientCycle`: the avatar half of `RspCycle.update()` (coords, move speed, anims,
  spotanims, say, hits, headbars, appearance…) so real players see the bot. `flush()` releases
  the computed info packets instead of queuing them to a session. A real client also sends
  `MapBuildComplete` after a rebuild, so `BotCycle` sets `player.lastMapBuildComplete` itself.
- **Biggest performance unknown:** rsprot computes player/NPC info for every allocated `Infos`,
  i.e. "what this bot can see", each tick. Measure in milestone 1. If it's too expensive, look
  at reducing a bot's view distance, or at whether rsprot supports an avatar with no observer.

### Bot identity

- A runtime flag (attr) marks a `Player` as a bot. A persistent list of bot account names
  (TOML under `.data/` at first, a DB table via Flyway later) says which accounts belong to bots.
- Things that should *not* special-case bots, so they look real: combat level, hiscores, the
  friends list, the player count.
- Things that *should* skip bots: central session/activity logging, Discord, trusted devices,
  idle logout (bots manage their own sessions).

### Profiles (BotSpawner)

- Archetypes picked by weight: `fresh` (lvl 3, tutorial gear), `low` main, `mid` main, `pure`
  (1 def), `skiller` (lvl 3 combat, high skills), `maxed` main.
- Stats sampled to fit the archetype: total XP target first, then spread it across skills with
  per-archetype weights so the stats make sense together (no 99 Attack with 1 HP).
- Gear and bank picked from level tiers; quest progress consistent with levels and gear
  requirements.
- Random valid appearance (kits/colours), realistic name generator, uniqueness checked against
  existing accounts.

### Brain

- Personality traits: aggression (PK ↔ skiller), efficiency, chattiness, patience/boredom,
  preferred hours.
- Picks goals like a real player: unlock something (quest, gear tier, requirement) > train a
  goal or lowest skill > grind boss/money. Boredom and randomness stop bots doing one thing
  forever.
- Human-like timing: reaction delays drawn from a distribution, not acting every tick, short
  AFKs, occasional longer breaks, rare misclicks.

### Navigation

- Short range: the engine route finder, through `MoveGameClick` messages.
- Long range (milestone 6): a world waypoint graph of nodes (banks, towns, activity spots)
  connected by walkable edges plus transitions (doors, gates, stairs, ladders, shortcuts,
  teleports), each transition being an `OpLoc` with its expected result. Built by hand at first
  for the F2P core, extended per area.

## Milestones

### M0: Groundwork ✅
- Fork, `playerbots` branch, server up to date with upstream and booting.
- This plan.

### M1: A bot logs in and is visible ✅
- New module `content/other/playerbots` with `BotClient`, `BotCycle`, `BotLogin`.
- Admin commands: `::bot add <name>`, `::bot remove <name>`, `::bot list`.
- Works out which `SessionStateEvent.Login` listeners need guarding for bots (DB online
  session, central activity log, Discord, autosave).
- **Done when:** a bot appears in Lumbridge and a real client sees it (name, appearance,
  combat level); it survives a few minutes of ticks with no exceptions; `::bot remove` logs it
  out and saves; logging it back in restores it. Tick cost recorded for 1 / 50 / 200 bots.

Status:
- [x] `BotClient` queues incoming messages and hands them to the real rsprot consumers
  (`BotInput`, built from `MessageConsumerProvider`); outgoing messages are released.
- [x] `BotCycle` reuses `RspCycle` for avatar sync, releases the bot's own info packets instead
  of sending them, and answers every map rebuild with `MapBuildComplete`.
- [x] `BotLogin`: `loadOrCreate` → same player setup as `AccountLoadResponseHook` (no dev
  rights for bots) → game thread: slot, `PrepareLogin`, info alloc, `playerRegistry.add`,
  `Login`, `EngineLogin`. Logout is the normal `manualLogout` path; the `Delete` listener
  deallocates the info protocol.
- [x] Bot identity: `Player.isBot` is `client is BotClient`, so no attr or varbit. Bot account
  names are kept in `.data/playerbots/accounts.txt` (a plain list for now). `::bot add` refuses
  an existing account that isn't on the list, so it can't take over a real player.
- [x] Commands (admin): `::bot add|remove <name>`, `::bot list`, `::bot fill <n>` (log in
  `bot1..botN` up to n online), `::bot clear`, `::bot tick` (avg/max tick time over the last
  100 ticks). New bot accounts start on the admin's tile.
- [x] A real client sees the bots: model, name and combat level.
- [x] Remove → save → re-add restores the bot where it was (added at A, re-added from B, came
  back at A).
- [x] Tick cost (`::bot tick`, 100-tick window, budget 600ms):

  | Bots | Avg | Max | Note |
  |---|---|---|---|
  | 50 | 19ms | 216ms | window includes the login burst |
  | 171 | 28ms | 71ms | steady state |
  | 200 | 73ms | 1559ms | window includes the login burst |
  | 200 | 25ms | 80ms | steady state |

  Steady state is about 25ms at 200 bots, roughly 4% of the tick, so idle bots are cheap. Spikes come from login bursts: the loader returns 50 accounts at a
  time, and each login runs a blocking DB write plus every login handler on the game thread.
  M2's `PopulationManager` has to stagger logins, a few per tick at most.

Login/logout listener review:
- `NetworkScript` DB online session (`setOnlineSession`, heartbeat, clear on logout): fine for
  bots. They're online characters, and it blocks a duplicate login from another world.
- `NetworkScript` central activity log (`logPlayerLogin` / `logPlayerLogout`) and
  `LoginScript.sendSocial` (central social snapshot): these run for bots too. They do nothing
  while central is off, but they need a bot guard (upstream touch) before bots run on a
  central-linked world.
- `DiscordLinkScript` logout (`invalidatePending`): harmless, left alone.
- Autosave: bots are autosaved like players, which we want because they're real accounts.
- Gameplay `onPlayerLogin` listeners (timers, quests, settings): should run for bots.

### M2: Random accounts and population
- `BotSpawner` with the archetypes above; appearance and name generation.
- `PopulationManager`: target online count, staggered logins/logouts.
- **Done when:** `::bot spawn 20` creates a believable mix of accounts that persist across
  restarts.

### M3: Brain, actions and the first activity
- Action layer (walk, op loc/npc/obj/held item, bank, resume dialogue) on top of `BotClient`.
- Brain/goal/task framework; `Wander` and `Idle` goals; canned public chat.
- First skill loop: woodcutting + banking around Lumbridge/Draynor.
- **Done when:** bots chop, bank and wander for an hour unattended without getting stuck.

### M4: Basic combat
- Chickens/cows/goblins: attack style choice, eat, loot, bury bones, flee on low HP, re-gear
  after death.

### M5: More skills
- Mining, fishing, cooking, firemaking, smithing, crafting, thieving, runecrafting, prayer,
  magic: each a goal script on top of the shared tasks.

### M6: World navigation
- Waypoint graph with transitions; bots travel between towns and to any activity spot.

### M7: Quests
- Quest scripts as step lists using the real dialogue (answer options via resume messages).
- Order: Cook's Assistant → Rune Mysteries → The Restless Ghost → Ernest the Chicken → others as
  they land upstream.

### M8: Bosses
- Shared combat core plus per-boss logic: Barrows and KBD first, then GWD generals, wilderness
  bosses, and the rest as upstream adds them.

### M9: PvP and minigames
- Wilderness PK and fleeing, tuned to be beatable and not tick-perfect.
- Minigames once upstream has them (currently 0/51).

## Upstream touch points

Keep this list current. Every change outside `content/other/playerbots` goes here.

- `api/net/.../RspCycle.kt`: takes an outgoing sink `(OutgoingGameMessage) -> Unit` instead of
  a `Session`, so `BotCycle` reuses it as-is. `NetworkScript` passes `{ session.queue(it) }`.
- `api/db/.../GameDatabase.kt`: transactions now take a lock. The game, loader and saver threads
  share one JDBC connection, so a commit on one thread ended another's transaction and its
  `SET LOCAL search_path`. The next query then ran against `public` and failed with
  `relation "character_progress" does not exist`. Bot login bursts hit this every time, and
  real players can hit it too, so it's worth sending upstream on its own.
- Still copied rather than shared: the player setup from `AccountLoadResponseHook`
  (`createPlayer` / `applyConfigTransforms`, both private). The copy in `BotLogin` skips dev
  rights and the saved instance-exit coord.

## Risks and open questions

- Info-protocol CPU cost per bot (see BotCycle). This decides the realistic bot count.
- Content that assumes a real client (cutscenes, camera, interfaces waiting on client scripts)
  may stall bots; bots need time-outs that recover from a stuck coroutine.
- Account storage: bot accounts share the player DB; they need a clean way to bulk-delete or
  reset them.
- Without central there's no local password check, so a real client can log into a bot's
  account while that bot is offline. Bots are only safe from takeover on a central-linked world.
- Upstream churn: rebase/merge `upstream/main` into `playerbots` regularly (see dev notes).

## Dev notes

- Update from upstream: `git switch main && git pull` (tracks `upstream/main`), then
  `git switch playerbots && git merge main`.
- After an upstream revision bump: update `revision` in `game.yml` *before* running install
  (install builds the cache for whatever revision `game.yml` says), run
  `./gradlew install --no-configuration-cache`, and update the RSProx proxy target in
  `~/.rsprox/proxy-targets.yaml` (`revision` and `jav_config_url`) to match.
- The ready line is `Server ready in …`.
