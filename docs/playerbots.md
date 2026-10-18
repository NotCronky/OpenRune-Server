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

### M2: Random accounts and population ✅
- `BotSpawner` with the archetypes above; appearance and name generation.
- `PopulationManager`: target online count, staggered logins/logouts.

Status:
- [x] `playerbots/pack`: `dbtable.playerbot_kit` (gear, ammo and food by role; requirements
  come from each obj's `statreq` params; quest-locked gear left out until M7) and
  `dbtable.playerbot_spawns` (weighted bank/town tiles). Ids: dbtables 54900–54901, dbrows
  54000–54127, in `src/main/resources/gamevals.toml`.
- [x] `spawn/`: `BotNames` (word lists in resources), `BotArchetype`, `BotProfile` (applies
  stats, worn, inv, bank), `BotSpawner` (rolls a profile + spawn tile), `BotLook`.
- [x] `BotLogin.login(name, spawn, profile)`: with a profile the account must be new; an existing
  one fails as `BotLogin.NAME_TAKEN` and the caller rolls another name. The profile is applied in
  `register()` after `playerRegistry.add` (invs exist from `Initialize`) and before `Login`. The
  generated name keeps its casing as the display name.
- [x] Appearance isn't saved by the account pipeline (for anyone, real players included), so a
  bot's look comes from `BotSpawner.look(accountName)`: a `DefaultGameRandom` seeded from the
  name, applied on every login. Same name, same look, nothing to store.
- [x] `PopulationManager` (driven from `GameLifecycle.StartCycle`): one login started per tick,
  at most 4 loading at once; offline bot accounts are reused before new ones are rolled; each
  bot it logs in gets a 30 min–3 h session, after which it logs out and is replaced; above the
  target, up to 2 bots log out per tick, earliest session end first.
- [x] Commands: `::bot spawn <n>` (roll n new accounts, staggered), `::bot population <n|off>`
  (target online count; no argument shows it). `::bot list` shows the target.
- [x] Verified: 20 spawned accounts logged in over ~70 ticks with no errors. The mix included a
  maxed main (cb 120), mid mains, pures, low-levels, level-3 skillers (e.g. 41 Woodcutting) and
  fresh accounts, with varied names, body types, gear and spawn tiles. After a restart, stats,
  worn, inventory, coords and display name came back; the look matched across two restarts.
- Not done: tests for `PopulationManager` (the module has no test setup yet).
- **Done when:** `::bot spawn 20` creates a believable mix of accounts that persist across
  restarts.

### M3: Brain, actions and the first activity ✅
- Action layer (walk, op loc/npc/obj/held item, bank, resume dialogue) on top of `BotClient`.
- Brain/goal/task framework; `Wander` and `Idle` goals; canned public chat.
- First skill loop: woodcutting + banking around Lumbridge/Draynor.
- **Done when:** bots chop, bank and wander for an hour unattended without getting stuck.

Status:
- [x] `brain/BotActions`: walk (`MoveGameClick`), op loc (`OpLocV2`), interface button
  (`If3Button`), close modal (`CloseModal`, Escape), public chat (`MessagePublic`). Everything a
  bot does is a real client message through the normal handlers.
- [x] `brain/BotWorld`: loc scan around a tile (per zone), walkability checks.
- [x] `brain/BotBrain` + `BotBrains`: one brain per online bot, made on `Login`, dropped on
  `Delete`, thought about every tick from `GameLifecycle.StartCycle`. A bot re-decides every
  1–3 ticks, with a 4% chance of a 5–15 tick distraction. Long walks go in hops of at most 14
  tiles so each stays inside the route finder's search area. Any modal the current goal doesn't
  own is closed (dialogue answering comes with quests in M7).
- [x] Goals: `IdleGoal`, `WanderGoal` (a few walks around where the bot logged in),
  `WoodcuttingGoal`. The picker weighs woodcutting 60 (when the bot is near a spot and has an
  axe, carried or banked), wander 25, idle 15.
- [x] Woodcutting: `dbtable.playerbot_woodcutting_spots` (Draynor, Varrock West, Edgeville with
  banks; Lumbridge drops). Trees are any `content.tree` loc the bot has the level for, usually
  the best tier, one of the 4 nearest so bots spread out. Full loads are banked at the nearest
  `content.bank_booth` (deposit all, withdraw an axe again if it went in) or, at a no-bank spot,
  dropped in one go. Unreachable trees are skipped after 4 tries; a bot standing still for 40
  thinks gives up the goal. Sessions last 10–60 minutes.
- [x] Canned chat: `resources/playerbots/chat/idle.txt`, each bot with its own chattiness.
- [x] `BotSpawner` gives 75% of new accounts the best F2P axe they can use (kit role `axe`),
  carried 40% of the time, otherwise in the bank.
- [x] Fixed in M2's profile code: items put straight into a bot's bank now grow
  `varbit.bank_tab_main`, and every bot login repairs it. Without it the bank's withdraw path
  threw and kicked the bot.
- [x] `::bot status <name>` shows a bot's goal, tile and whether it's busy.- [x] Soak: 18 fresh bots (12 at Draynor, 6 at Lumbridge) ran 51 minutes unattended with no
  errors and none logged out or stuck. Draynor woodcutters each banked 160–350 logs and spread
  over the willows; the Lumbridge ones power-chopped; wanderers stayed near where they logged in.
- Not done: tests for the brain/goal state machines (the module has no test setup yet).
- [x] Fixed: some bots chopped without facing their tree. rsprot only re-sends a stored face
  angle to observers that have seen the player before, so a client that first saw a bot
  mid-chop drew it facing south. A bot standing at a loc it's interacting with now faces it
  again every 8 ticks (`BotBrain.refreshFacing`). Real players still have this; fixing it for
  them means a change in `RspCycle` or rsprot.

### M4: Basic combat
- Chickens/cows/goblins: attack style choice, eat, loot, bury bones, flee on low HP, re-gear
  after death.

Status (core loop verified in a live client; no long soak yet):
- [x] `playerbots/pack`: `dbtable.playerbot_combat_spots` (Fred's chickens, the east-farm
  chickens, the east cow field and the goblins east of the river, with the bot combat range each
  suits; centres picked from the npc spawn data) and `dbtable.playerbot_combat_loot` (bones are
  buried; coins and feathers kept). Ids: dbtables 54903–54904, dbrows 54139–54145.
- [x] Actions: attack (`OpNpcV2`, op looked up by the "Attack" text), take (`OpObjV2` op 3),
  held-item options by name (`heldOp`: Eat, Bury, Wield/Wear, mapped to the inventory's
  `If3Button` ops), combat-tab stance buttons.
- [x] `BotWorld`: npc and ground-obj scans next to the loc scan.
- [x] `CombatGoal`: wields carried gear first; stance for the lowest of Attack/Strength/Defence
  (pures with Defence 1 skip Defence), rapid or accurate with a bow; targets are npcs of the
  spot's name with an Attack op, not in combat with someone else, one of the 3 nearest; a target
  that never gets into combat is skipped after 15 ticks. Buries bones, loots within 6 tiles,
  picks up and re-wields its own ammo, stops when a bow runs out of ammo, and walks 12 tiles away
  at 25% HP with no food (the goal ends).
- [x] Eating is the brain's job, before any goal: below 40–60% HP (rolled per bot) a bot with
  food eats, whatever it's doing.
- [x] Goal picker: a weighted pick over what's available (woodcutting 60, combat 50, wander 25,
  idle 15). Combat needs a spot for the bot's combat level within 90 tiles and at least 70% HP;
  level-3 skillers (no combat stats, 10+ Woodcutting) never fight.
- [x] Death: `BotDeathHook` (a `PlayerDeathCleanupHook`, bound in `PlayerbotsModule`) runs after
  every respawn teleport and switches the bot to `RecoverGoal`, aimed at the tile the bot stood on
  the tick before (it stands still while dying). Polling HP from the brain missed deaths with no
  killing blow. `RecoverGoal`: wield what was kept, walk back, pick up the pile, wield it again;
  gives up after 1,000 ticks or when stuck.
- [x] Debug commands (admin): `::bot spawn <n> here` (roll accounts on your tile),
  `::bot goal <name> <woodcutting|combat|wander|idle>` (force a goal the bot could pick now),
  `::bot inv <name>`, `::bot hp <name> <n>` (lowers the HP stat; no hitsplat, so watching clients
  don't see the bar change), `::bot kill <name>` (normal death sequence). `::bot status` shows HP
  and combat level; long `::bot list` output is split under the 255-char message limit.
- [x] Verified in a live client (Lumbridge goblins, east cows):
  - ranged (Pete38, yew shortbow) and unarmed melee (Kev_9) fights; goblins die, bones are
    picked up and buried, coins looted (5 → 45);
  - eating outside combat (Pete38 at 15/42 ate a tuna mid-wander) and inside it (knight885 ate
    all 4 bread as its HP was knocked down);
  - fleeing (knight885, no food, 2/10 HP: walked ~16 tiles off, then wandered until healed);
  - walking from the goblins to the east cow field;
  - death → recovery (Pete38: respawned in Lumbridge, walked back, picked up axe, 10 tuna, logs,
    coins, leather body, boots and 124 arrows, and wore all of it again).
- Seen while testing: this realm's XP rate is high, so low bots level out of their spot's combat
  range within minutes (Kev_9 went from combat 12 to 48 in about five minutes, with a level-up
  firework on nearly every hit). Not a bot bug, but spots need wider ranges, or more spots, on a
  boosted realm.
- Not done: the stance button can't be confirmed from the client (punch looks the same in each
  style); magic bots fight with their staff in melee (no runes, no autocast yet); no hour-long
  soak yet.

### M5: More skills
- Mining, fishing, cooking, firemaking, smithing, crafting, thieving, runecrafting, prayer,
  magic: each a goal script on top of the shared tasks.

Status (gathering done and verified live; processing skills next):
- [x] `GatheringGoal` replaces `WoodcuttingGoal`: one goal for any `GatheringSkill`
  (`brain/gathering/`: `WoodcuttingSkill`, `MiningSkill`, `FishingSkill`). It walks to the
  spot, works one of the 4 nearest targets of the best tier it can use (80% of the time), skips a
  target that gives nothing after 4 tries, banks at the nearest booth (deposit all, then withdraw
  tool and bait) or drops at a spot with no bank. Only what the bot didn't carry when the goal
  started (minus tools and bait) is dropped, so food and coins are safe.
- [x] `dbtable.playerbot_woodcutting_spots` became `dbtable.playerbot_gathering_spots` (same id,
  54902) with a `skill` column. Mining: Varrock East (banks; rocks checked in a live client at
  3281–3290, 3361–3370), Varrock West, Lumbridge Swamp, Rimmington. Fishing (spots taken from the
  npc spawn data): Draynor (banks), Lumbridge river, Lumbridge Swamp, Barbarian Village, Musa
  Point. New dbrows 54146–54167. The gameval merger doesn't remove a renamed symbol, so the old
  `playerbot_woodcutting_spots=54902` line had to be deleted from `.data/gamevals/dbtable.rscm`
  by hand before `buildCache` would run.
- [x] Mining: any `content.rock` whose `dbtable.mining_rocks` level the bot meets; pickaxes via
  the mining module's `findPickaxe` / `pickaxeLevelReq`.
- [x] Fishing reads the fishing module's own tables: each spot type's op1/op3 method
  (`fishing_spot_def`), the method's tool and bait (`fishing_method`) and the lowest level that
  catches anything (`fishing_spot`). Gear checks and bank withdrawals only count methods offered
  by the spots actually at the bot's spot, so a lobster pot doesn't count at Draynor.
- [x] `BotBrain.busy` also counts a pending weak queue. Fishing runs its catch loop on one, so
  without this the bot looked idle, re-clicked the spot every think and reset the catch timer.
- [x] `BotSpawner`: the best axe, pickaxe and fishing tool (with its method's bait, from kit role
  `fishing_bait`) the bot can use, each owned 75% of the time, carried 40% (else banked). Kit
  dbrows for pickaxes, net, rods, lobster pot, harpoon, fishing bait and feathers.
- [x] `::bot goal` takes `woodcutting|mining|fishing|combat|wander|idle`; `::bot status` shows
  how the previous goal ended (`last: fishing at draynor: Failed`).
- [x] Verified live: 4 bots mined at Varrock East (one carried a rune pickaxe, three fetched
  theirs from Varrock East bank); a full load of iron ore was banked and the bot went back with
  only its pickaxe. At Draynor bots netted shrimp, fetched a banked net, and a bot whose only
  kit was a lobster pot was correctly refused.
- Next: cooking (raw catch on a range or fire), firemaking (logs), smithing (ore at a furnace,
  bars at an anvil), crafting, then thieving, prayer (bury bones from the bank), runecrafting and
  magic.

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
