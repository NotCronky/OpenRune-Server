# Tutorial Island

Classic Tutorial Island (`varp.tutorial`, stages 1 → 1000), rebuilt from an RSProx capture of a
full live run (rev 240). Module: `content/areas/misc/tutorial-island`.

Enable with `gameplay.tutorial-island: true` in `game.yml`. New main-mode accounts then start at
3094, 3107 on stage 2. Bots are created at stage 1000 and skip it. Existing accounts are left alone.

## How it fits together

- **Stage data is in the cache.** `dbtable.tutorial_island_steps` holds, for every stage, the
  chatbox overlay text, the hint arrow (npc, or tile + height + anchor), the `tutorial_overlay_hint`
  id, the flashing side tab, the tab to switch to, and the progress-bar value. All stages from 1 to
  680 are filled in from the capture. `dbtable.tutorial_island_side_tabs` holds the stage at which
  each side tab unlocks, and the stage that clicking it moves to.
- `TutorialIsland.refresh(player)` re-sends the current stage's overlay, arrow and flashing tab. It
  runs on login and after every stage change. `setStage` opens newly unlocked tabs, and login
  closes the tabs that are still locked.
- Side-stone clicks come through the new unbound `IfOverlayButtonPressed` event, because the
  settings, journal, prayer and magic stones already have keyed handlers.
- `::tutorial [stage]` (admin) prints or sets your stage.

## Done (stages 2–1000)

Every section from the Gielinor Guide to the Home Teleport: survival skills, kitchen, quest guide,
mining and smithing, combat (equipping, equipment stats, the rat pit with melee then ranged),
bank and poll booth, Account Guide, Brother Brace, and the magic instructor (Wind Strike on a
chicken, Learning the Ropes completing at 671, the mainland/Ironman questions). Casting Home
Teleport at stage 680 takes the player off the island; once they're off it, a post-tick hook
finishes the tutorial: inventory and equipment are swapped for the starter kit
(`dbtable.tutorial_island_starter_kit`), the remaining side tabs open and `tutorial_end` runs.

Systems owned by other modules are observed rather than replaced:

- Anvil: `onIfOpen("interface.smithing")` and the smithing module's `AnvilSmithed` event.
- Rats and chicken: `NpcAttackValidateHook` (also refuses repeat kills) and `NpcDeathKillHook`.
- Equipping: `PlayerInvUpdateHook` on the worn inventory.
- Teleports: `PlayerTeleportValidateHook` blocks them until stage 680.
- Dying on the island keeps everything and respawns on the island.

## Remaining

- Stage 1 screens: display name (`tutorial_displayname`), character design (`player_design`, which
  has no server implementation yet) and the experience picker (`tutorial_player_experience`).
  New players currently skip straight to stage 2 with the default appearance.
- The spell module's Home Teleport is a plain standard teleport to (3222, 3222). Live plays the
  long Lumbridge Home Teleport animation and lands at (3232, 3232).
- The poll booth doesn't open the `ballot` interface or move the camera.
- Dialogue the capture never showed: the "Tell me about…" recap menus, the replies to "What's an
  Ironman?" and "Yes, I do want to become an Ironman." (both open the Ironman setup interface for
  now), the messages for clicking a door too early, and NPC replies at off-path stages. For now,
  off-path stages re-give lost items or re-show the current instruction.
- Equipping items before the combat section (live blocks it with "You'll be told how to equip
  items later."), tin rock depletion, and inventory/component highlights past the kitchen.
- Tests for the item-moving flows (dough, smelting, the starter-kit swap).
