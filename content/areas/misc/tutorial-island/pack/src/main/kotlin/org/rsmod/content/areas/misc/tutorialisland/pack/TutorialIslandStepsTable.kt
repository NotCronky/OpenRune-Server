package org.rsmod.content.areas.misc.tutorialisland.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object TutorialIslandStepsTable {

    const val STAGE = 0
    const val OVERLAY = 1
    const val HINT_NPC = 2
    const val HINT_COORD = 3
    const val HINT_HEIGHT = 4
    const val HINT_POSITION = 5
    const val OVERLAY_HINT = 6
    const val FLASH_TAB = 7
    const val SIDE_TAB = 8
    const val PROGRESS = 9

    private const val CENTER = 2
    private const val WEST = 3
    private const val EAST = 4
    private const val SOUTH = 5
    private const val NORTH = 6

    fun steps() =
        dbTable("dbtable.tutorial_island_steps", serverOnly = true) {
            column("stage", STAGE, VarType.INT)
            column("overlay", OVERLAY, VarType.STRING)
            column("hint_npc", HINT_NPC, VarType.NPC)
            column("hint_coord", HINT_COORD, VarType.COORDGRID)
            column("hint_height", HINT_HEIGHT, VarType.INT)
            column("hint_position", HINT_POSITION, VarType.INT)
            column("overlay_hint", OVERLAY_HINT, VarType.INT)
            column("flash_tab", FLASH_TAB, VarType.INT)
            column("side_tab", SIDE_TAB, VarType.INT)
            column("progress", PROGRESS, VarType.INT)

            row("dbrow.tutorial_island_step_1") {
                column(STAGE, 1)
                column(
                    OVERLAY,
                    "<col=0000ff>Setting your name</col><br>Before you get started, you'll " +
                        "need to set a display name. Please use the open interface to set one.",
                )
                column(PROGRESS, 0)
            }
            row("dbrow.tutorial_island_step_2") {
                column(STAGE, 2)
                column(
                    OVERLAY,
                    "<col=0000ff>Getting started</col><br>When you're ready to get " +
                        "started, click on the Gielinor Guide. He is indicated by a flashing " +
                        "yellow arrow.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_basics_instructor")
                column(PROGRESS, 0)
            }
            row("dbrow.tutorial_island_step_3") {
                column(STAGE, 3)
                column(
                    OVERLAY,
                    "<col=0000ff>Settings menu</col><br>Please click on the flashing " +
                        "spanner icon found at the bottom right of your screen. This will " +
                        "display your settings menu.",
                )
                column(FLASH_TAB, 11)
                column(PROGRESS, 0)
            }
            row("dbrow.tutorial_island_step_7") {
                column(STAGE, 7)
                column(
                    OVERLAY,
                    "<col=0000ff>Settings menu</col><br>On the side panel, you can now see " +
                        "a variety of game settings. You can also click the all settings " +
                        "button to see all available settings. Talk to the Gielinor Guide to " +
                        "continue.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_basics_instructor")
                column(SIDE_TAB, 11)
                column(PROGRESS, 0)
            }
            row("dbrow.tutorial_island_step_10") {
                column(STAGE, 10)
                column(
                    OVERLAY,
                    "<col=0000ff>Moving on</col><br>It's time to meet your first " +
                        "instructor. To continue, all you need to do is click on the door. " +
                        "It's indicated by a flashing yellow arrow. Remember, you can use your " +
                        "arrow keys to rotate the camera.",
                )
                columnCoord(HINT_COORD, CoordGrid(3098, 3107, 0))
                column(HINT_HEIGHT, 128)
                column(HINT_POSITION, WEST)
                column(OVERLAY_HINT, 1)
                column(PROGRESS, 2)
            }
            row("dbrow.tutorial_island_step_20") {
                column(STAGE, 20)
                columnRSCM(HINT_NPC, "npc.newbie_survival_instructor")
                column(OVERLAY_HINT, 6)
                column(PROGRESS, 2)
            }
            row("dbrow.tutorial_island_step_30") {
                column(STAGE, 30)
                column(
                    OVERLAY,
                    "<col=0000ff>You've been given an item</col><br>To view the item " +
                        "you've been given, you'll need to open your inventory. To do so, " +
                        "click on the flashing backpack icon to the right hand side of your " +
                        "screen.",
                )
                column(FLASH_TAB, 3)
                column(PROGRESS, 2)
            }
            row("dbrow.tutorial_island_step_40") {
                column(STAGE, 40)
                column(
                    OVERLAY,
                    "<col=0000ff>Inventory</col><br>This is your inventory. You can view " +
                        "all of your items here, including the net you've just been given. " +
                        "Let's use it to catch some shrimp.",
                )
                columnRSCM(HINT_NPC, "npc.0_48_48_newbiefishing")
                column(OVERLAY_HINT, 7)
                column(SIDE_TAB, 3)
                column(PROGRESS, 2)
            }
            row("dbrow.tutorial_island_step_50") {
                column(STAGE, 50)
                column(
                    OVERLAY,
                    "<col=0000ff>You've gained some experience</col><br>Click on the " +
                        "flashing bar graph icon near the inventory button to see your skills " +
                        "menu.",
                )
                column(OVERLAY_HINT, 8)
                column(FLASH_TAB, 1)
                column(PROGRESS, 2)
            }
            row("dbrow.tutorial_island_step_60") {
                column(STAGE, 60)
                column(
                    OVERLAY,
                    "<col=0000ff>Skills and Experience</col><br>On this menu you can view " +
                        "your skills. Your skills can be leveled up by earning experience. As " +
                        "you level up your skills, you will earn new unlocks. Speak to the " +
                        "survival expert to continue.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_survival_instructor")
                column(SIDE_TAB, 1)
                column(PROGRESS, 3)
            }
            row("dbrow.tutorial_island_step_70") {
                column(STAGE, 70)
                column(
                    OVERLAY,
                    "<col=0000ff>Woodcutting</col><br>It's time to cook your shrimp. " +
                        "However, you require a fire to do that which means you need some " +
                        "logs. You can cut down trees using your Woodcutting skill, all you " +
                        "need is an axe.",
                )
                columnCoord(HINT_COORD, CoordGrid(3099, 3095, 0))
                column(HINT_HEIGHT, 160)
                column(HINT_POSITION, EAST)
                column(OVERLAY_HINT, 9)
                column(SIDE_TAB, 3)
                column(PROGRESS, 3)
            }
            row("dbrow.tutorial_island_step_80") {
                column(STAGE, 80)
                column(
                    OVERLAY,
                    "<col=0000ff>Firemaking</col><br>Now that you have some logs, it's " +
                        "time to light a fire. First, click on the tinderbox in your " +
                        "inventory. Then, with the tinderbox highlighted, click on the logs to " +
                        "use the tinderbox on them.",
                )
                column(OVERLAY_HINT, 5)
                column(SIDE_TAB, 3)
                column(PROGRESS, 3)
            }
            row("dbrow.tutorial_island_step_90") {
                column(STAGE, 90)
                column(
                    OVERLAY,
                    "<col=0000ff>Cooking</col><br>Now it's time to get cooking. To do so, " +
                        "click on the shrimp in your inventory. Then, with the shrimp " +
                        "highlighted, click on a fire to cook them. If you look at the top " +
                        "left of the screen, you'll see the instructions that you're giving to " +
                        "your character.",
                )
                column(OVERLAY_HINT, 10)
                column(SIDE_TAB, 3)
                column(PROGRESS, 3)
            }
            row("dbrow.tutorial_island_step_120") {
                column(STAGE, 120)
                column(
                    OVERLAY,
                    "<col=0000ff>Moving on</col><br>Well done, you've just cooked your " +
                        "first meal! Speak to the survival expert if you want a recap, " +
                        "otherwise you can move on. Click on the gate shown and follow the " +
                        "path. Remember, you can use your arrow keys to rotate the camera.",
                )
                columnCoord(HINT_COORD, CoordGrid(3089, 3092, 0))
                column(HINT_HEIGHT, 65)
                column(HINT_POSITION, EAST)
                column(PROGRESS, 4)
            }
            row("dbrow.tutorial_island_step_130") {
                column(STAGE, 130)
                column(
                    OVERLAY,
                    "<col=0000ff>Moving on</col><br>Follow the path until you get to the " +
                        "door with the yellow arrow above it. Click on the door to open it. If " +
                        "you can't find it, try moving your camera by clicking and dragging " +
                        "your middle mouse button.",
                )
                columnCoord(HINT_COORD, CoordGrid(3079, 3084, 0))
                column(HINT_HEIGHT, 128)
                column(HINT_POSITION, WEST)
                column(PROGRESS, 4)
            }
            row("dbrow.tutorial_island_step_140") {
                column(STAGE, 140)
                column(
                    OVERLAY,
                    "<col=0000ff>Cooking</col><br>Talk to the chef to learn the more " +
                        "advanced aspects of Cooking such as combining ingredients!",
                )
                columnRSCM(HINT_NPC, "npc.newbie_cook_instructor")
                column(PROGRESS, 4)
            }
            row("dbrow.tutorial_island_step_150") {
                column(STAGE, 150)
                column(
                    OVERLAY,
                    "<col=0000ff>Making dough</col><br>This is the base for many meals. To " +
                        "make dough you must mix flour with water. To do so, click on the " +
                        "flour in your inventory. Then, with the flour highlighted, click on " +
                        "the water to combine them into dough.",
                )
                column(SIDE_TAB, 3)
                column(PROGRESS, 4)
            }
            row("dbrow.tutorial_island_step_160") {
                column(STAGE, 160)
                column(
                    OVERLAY,
                    "<col=0000ff>Cooking dough</col><br>Now you have made the dough, you " +
                        "can bake it into some bread. To do so, just click on the indicated " +
                        "range.",
                )
                columnCoord(HINT_COORD, CoordGrid(3075, 3081, 0))
                column(HINT_HEIGHT, 65)
                column(HINT_POSITION, EAST)
                column(OVERLAY_HINT, 11)
                column(PROGRESS, 5)
            }
            row("dbrow.tutorial_island_step_170") {
                column(STAGE, 170)
                column(
                    OVERLAY,
                    "<col=0000ff>Moving on</col><br>Well done! You've baked your first " +
                        "loaf of bread. As you gain experience in Cooking, you will be able to " +
                        "make other things like pies and cakes. You can now use the next door " +
                        "to move on. If you need a recap on anything, talk to the master chef.",
                )
                columnCoord(HINT_COORD, CoordGrid(3072, 3090, 0))
                column(HINT_HEIGHT, 128)
                column(HINT_POSITION, EAST)
                column(PROGRESS, 6)
            }
            row("dbrow.tutorial_island_step_200") {
                column(STAGE, 200)
                column(
                    OVERLAY,
                    "<col=0000ff>Fancy a run?</col><br>When navigating the world, you can " +
                        "either run or walk. Running is faster but you can't run for long as " +
                        "you'll soon run out of energy. You can use the flashing orb next to " +
                        "the minimap to toggle running. Why not try it as you head to the next " +
                        "section?",
                )
                columnCoord(HINT_COORD, CoordGrid(3086, 3126, 0))
                column(HINT_HEIGHT, 128)
                column(HINT_POSITION, SOUTH)
                column(OVERLAY_HINT, 21)
                column(PROGRESS, 7)
            }
            row("dbrow.tutorial_island_step_220") {
                column(STAGE, 220)
                column(
                    OVERLAY,
                    "<col=0000ff>Quests</col><br>It's time to learn about quests! Just " +
                        "talk to the Quest Guide to get started.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_quest_instructor")
                column(PROGRESS, 7)
            }
            row("dbrow.tutorial_island_step_230") {
                column(STAGE, 230)
                column(
                    OVERLAY,
                    "<col=0000ff>Quest journal</col><br>Click on the flashing icon to the " +
                        "left of your inventory.",
                )
                column(FLASH_TAB, 2)
                column(PROGRESS, 7)
            }
            row("dbrow.tutorial_island_step_240") {
                column(STAGE, 240)
                column(
                    OVERLAY,
                    "<col=0000ff>Quest journal</col><br>This is your quest journal. It " +
                        "lists every quest in the game. Talk to the quest guide again for an " +
                        "explanation on how it works.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_quest_instructor")
                column(SIDE_TAB, 2)
                column(PROGRESS, 7)
            }
            row("dbrow.tutorial_island_step_250") {
                column(STAGE, 250)
                column(
                    OVERLAY,
                    "<col=0000ff>Moving on</col><br>It's time to enter some caves. Click " +
                        "on the ladder to go down to the next area.",
                )
                columnCoord(HINT_COORD, CoordGrid(3088, 3119, 0))
                column(HINT_HEIGHT, 5)
                column(HINT_POSITION, CENTER)
                column(PROGRESS, 8)
            }
            row("dbrow.tutorial_island_step_260") {
                column(STAGE, 260)
                column(
                    OVERLAY,
                    "<col=0000ff>Mining and Smithing</col><br>Next let's get you a weapon, " +
                        "or more to the point, you can make your first weapon yourself. Don't " +
                        "panic, the mining instructor will help you. Talk to him and he'll " +
                        "tell you all about it.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_mining_instructor")
                column(PROGRESS, 8)
            }
            row("dbrow.tutorial_island_step_270") {
                column(STAGE, 270)
                column(PROGRESS, 8)
            }
            row("dbrow.tutorial_island_step_300") {
                column(STAGE, 300)
                column(
                    OVERLAY,
                    "<col=0000ff>Mining</col><br>It's quite simple really. To mine a rock, " +
                        "all you need to do is click on it. First up, try mining some tin.",
                )
                columnCoord(HINT_COORD, CoordGrid(3077, 9504, 0))
                column(HINT_HEIGHT, 45)
                column(HINT_POSITION, CENTER)
                column(OVERLAY_HINT, 12)
                column(SIDE_TAB, 3)
                column(PROGRESS, 8)
            }
            row("dbrow.tutorial_island_step_310") {
                column(STAGE, 310)
                column(
                    OVERLAY,
                    "<col=0000ff>Mining</col><br>Now that you have some tin ore, you just " +
                        "need some copper. To mine a rock, all you need to do is click on it.",
                )
                columnCoord(HINT_COORD, CoordGrid(3083, 9501, 0))
                column(HINT_HEIGHT, 45)
                column(HINT_POSITION, CENTER)
                column(SIDE_TAB, 3)
                column(PROGRESS, 8)
            }
            row("dbrow.tutorial_island_step_320") {
                column(STAGE, 320)
                column(
                    OVERLAY,
                    "<col=0000ff>Smelting</col><br>You now have some tin ore and some " +
                        "copper ore. You can smelt these into a bronze bar. To do so, just " +
                        "click on the indicated furnace. Try it now.",
                )
                columnCoord(HINT_COORD, CoordGrid(3079, 9496, 0))
                column(HINT_HEIGHT, 128)
                column(HINT_POSITION, CENTER)
                column(OVERLAY_HINT, 13)
                column(SIDE_TAB, 3)
                column(PROGRESS, 9)
            }
            row("dbrow.tutorial_island_step_330") {
                column(STAGE, 330)
                column(
                    OVERLAY,
                    "<col=0000ff>Smelting</col><br>You've made a bronze bar! Speak to the " +
                        "mining instructor and he'll show you how to make it into a weapon.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_mining_instructor")
                column(PROGRESS, 9)
            }
            row("dbrow.tutorial_island_step_340") {
                column(STAGE, 340)
                column(
                    OVERLAY,
                    "<col=0000ff>Smithing a dagger</col><br>To smith you'll need a hammer " +
                        "and enough metal bars to make the desired item, as well as a handy " +
                        "anvil. To start the process, click on the anvil, or alternatively use " +
                        "the bar on it.",
                )
                columnCoord(HINT_COORD, CoordGrid(3083, 9499, 0))
                column(HINT_HEIGHT, 50)
                column(HINT_POSITION, CENTER)
                column(OVERLAY_HINT, 14)
                column(SIDE_TAB, 3)
                column(PROGRESS, 9)
            }
            row("dbrow.tutorial_island_step_350") {
                column(STAGE, 350)
                column(
                    OVERLAY,
                    "<col=0000ff>Smithing a dagger</col><br>Use an anvil to open the " +
                        "smithing menu, in it you will see a list of all the things you can " +
                        "make. Only the dagger can be made at your skill level; this is shown " +
                        "by the white text under it. You'll need to select the dagger to " +
                        "continue.",
                )
                columnCoord(HINT_COORD, CoordGrid(3083, 9499, 0))
                column(HINT_HEIGHT, 50)
                column(HINT_POSITION, CENTER)
                column(SIDE_TAB, 3)
                column(PROGRESS, 9)
            }
            row("dbrow.tutorial_island_step_360") {
                column(STAGE, 360)
                column(
                    OVERLAY,
                    "<col=0000ff>Moving on</col><br>Congratulations, you've made your " +
                        "first weapon. Now it's time to move on. Go through the gates shown by " +
                        "the arrow. Remember, you may need to move the camera to see your " +
                        "surroundings. Speak to the mining instructor for a recap at any time.",
                )
                columnCoord(HINT_COORD, CoordGrid(3094, 9502, 0))
                column(HINT_HEIGHT, 128)
                column(HINT_POSITION, EAST)
                column(PROGRESS, 10)
            }
            row("dbrow.tutorial_island_step_370") {
                column(STAGE, 370)
                column(
                    OVERLAY,
                    "<col=0000ff>Combat</col><br>In this area you will find out about " +
                        "melee and ranged combat. Speak to the guide and he will tell you all " +
                        "about it.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_combat_instructor")
                column(PROGRESS, 10)
            }
            row("dbrow.tutorial_island_step_390") {
                column(STAGE, 390)
                column(
                    OVERLAY,
                    "<col=0000ff>Equipping items</col><br>You now have access to a new " +
                        "interface. Click on the flashing icon of a man, the one to the right " +
                        "of your backpack icon.",
                )
                column(FLASH_TAB, 4)
                column(PROGRESS, 10)
            }
            row("dbrow.tutorial_island_step_400") {
                column(STAGE, 400)
                column(
                    OVERLAY,
                    "<col=0000ff>Worn inventory</col><br>This is your worn inventory. Here " +
                        "you can see what items you have equipped. In the bottom left corner, " +
                        "you will notice a flashing button with a shield and helmet on it. " +
                        "This button lets you view more details on what you have equipped. " +
                        "Click on it now.",
                )
                column(SIDE_TAB, 4)
                column(PROGRESS, 10)
            }
            row("dbrow.tutorial_island_step_405") {
                column(STAGE, 405)
                column(
                    OVERLAY,
                    "<col=0000ff>Equipment stats</col><br>You can see what items you are " +
                        "wearing in the worn inventory to the left of the screen, with their " +
                        "combined statistics on the right. Let's add something. Click your " +
                        "dagger to equip it.",
                )
                column(SIDE_TAB, 3)
                column(PROGRESS, 10)
            }
            row("dbrow.tutorial_island_step_410") {
                column(STAGE, 410)
                column(
                    OVERLAY,
                    "<col=0000ff>Equipment stats</col><br>You're now holding your dagger. " +
                        "Clothes, armour, weapons and more are equipped like this. " +
                        "Alternatively, you can click items directly from your inventory to " +
                        "equip them without opening the stats window. Speak to the combat " +
                        "instructor to continue.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_combat_instructor")
                column(SIDE_TAB, 4)
                column(PROGRESS, 10)
            }
            row("dbrow.tutorial_island_step_420") {
                column(STAGE, 420)
                column(
                    OVERLAY,
                    "<col=0000ff>Unequipping items</col><br>To unequip an item, go to your " +
                        "worn inventory and click on the item. Alternatively, equipping a new " +
                        "item into the same slot will unequip the old one. Try this out now by " +
                        "swapping your dagger for the sword and shield that the combat " +
                        "instructor gave you.",
                )
                column(SIDE_TAB, 3)
                column(PROGRESS, 10)
            }
            row("dbrow.tutorial_island_step_430") {
                column(STAGE, 430)
                column(
                    OVERLAY,
                    "<col=0000ff>Combat interface</col><br>Click on the flashing crossed " +
                        "swords icon to open the combat interface.",
                )
                column(FLASH_TAB, 0)
                column(PROGRESS, 10)
            }
            row("dbrow.tutorial_island_step_440") {
                column(STAGE, 440)
                column(
                    OVERLAY,
                    "<col=0000ff>Combat interface</col><br>This is your combat interface. " +
                        "From here, you can select the attack style that you'll use in combat. " +
                        "Using different attack styles will give different types of " +
                        "experience. As well as this, monsters are weak to specific attack " +
                        "styles. Click on the gates to continue.",
                )
                columnCoord(HINT_COORD, CoordGrid(3110, 9518, 0))
                column(HINT_HEIGHT, 128)
                column(HINT_POSITION, EAST)
                column(OVERLAY_HINT, 15)
                column(SIDE_TAB, 0)
                column(PROGRESS, 11)
            }
            row("dbrow.tutorial_island_step_450") {
                column(STAGE, 450)
                column(
                    OVERLAY,
                    "<col=0000ff>Attacking</col><br>It's time to slay some rats! To attack " +
                        "a rat, all you have to do is click on it. This will cause you to walk " +
                        "over and start hitting it.",
                )
                columnRSCM(HINT_NPC, "npc.newbiegiantrat")
                column(PROGRESS, 11)
            }
            row("dbrow.tutorial_island_step_460") {
                column(STAGE, 460)
                column(
                    OVERLAY,
                    "<col=0000ff>Sit back and watch</col><br>While you are fighting you " +
                        "will see a bar over your head. The bar shows how much health you have " +
                        "left. Your opponent will have one too. You will continue to attack " +
                        "the rat until it's dead or you do something else.",
                )
                columnRSCM(HINT_NPC, "npc.newbiegiantrat")
                column(PROGRESS, 11)
            }
            row("dbrow.tutorial_island_step_470") {
                column(STAGE, 470)
                column(
                    OVERLAY,
                    "<col=0000ff>Well done, you've made your first kill!</col><br>Pass " +
                        "through the gate and talk to the combat instructor. He will give you " +
                        "your next task.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_combat_instructor")
                column(PROGRESS, 11)
            }
            row("dbrow.tutorial_island_step_480") {
                column(STAGE, 480)
                column(
                    OVERLAY,
                    "<col=0000ff>Rat ranging</col><br>Before you can use the shortbow and " +
                        "arrows you'll need to equip them by clicking on them. Once equipped " +
                        "with the ranging gear, try killing another rat. You don't need to " +
                        "enter the pen this time. To attack a rat, just click on it.",
                )
                columnRSCM(HINT_NPC, "npc.newbiegiantrat")
                column(OVERLAY_HINT, 16)
                column(SIDE_TAB, 3)
                column(PROGRESS, 12)
            }
            row("dbrow.tutorial_island_step_490") {
                column(STAGE, 490)
                column(
                    OVERLAY,
                    "<col=0000ff>Rat ranging</col><br>Before you can use the shortbow and " +
                        "arrows you'll need to equip them by clicking on them. Once equipped " +
                        "with the ranging gear, try killing another rat. You don't need to " +
                        "enter the pen this time. To attack a rat, just click on it.",
                )
                columnRSCM(HINT_NPC, "npc.newbiegiantrat")
                column(PROGRESS, 12)
            }
            row("dbrow.tutorial_island_step_500") {
                column(STAGE, 500)
                column(
                    OVERLAY,
                    "<col=0000ff>Moving on</col><br>You have completed the tasks here. To " +
                        "move on, click on the indicated ladder. If you need to go over any of " +
                        "what you learnt here, just talk to the combat instructor and he'll " +
                        "tell you what he can.",
                )
                columnCoord(HINT_COORD, CoordGrid(3111, 9526, 0))
                column(HINT_HEIGHT, 128)
                column(HINT_POSITION, CENTER)
                column(OVERLAY_HINT, 22)
                column(PROGRESS, 14)
            }
            row("dbrow.tutorial_island_step_510") {
                column(STAGE, 510)
                column(
                    OVERLAY,
                    "<col=0000ff>Banking</col><br>This is the Bank of Gielinor, where you " +
                        "can store all your most valued items. To open your bank, just click " +
                        "on the indicated booth.",
                )
                columnCoord(HINT_COORD, CoordGrid(3122, 3124, 0))
                column(HINT_HEIGHT, 128)
                column(HINT_POSITION, CENTER)
                column(PROGRESS, 14)
            }
            row("dbrow.tutorial_island_step_520") {
                column(STAGE, 520)
                column(
                    OVERLAY,
                    "<col=0000ff>Banking</col><br>This is your bank. You can store things " +
                        "here for safekeeping. To deposit something from your inventory, just " +
                        "click on it. You can withdraw things in the same way. To continue, " +
                        "close the bank and click on the indicated poll booth.",
                )
                columnCoord(HINT_COORD, CoordGrid(3119, 3121, 0))
                column(HINT_HEIGHT, 128)
                column(HINT_POSITION, CENTER)
                column(PROGRESS, 14)
            }
            row("dbrow.tutorial_island_step_525") {
                column(STAGE, 525)
                column(
                    OVERLAY,
                    "<col=0000ff>Moving on</col><br>Polls are run periodically to let the " +
                        "Old School RuneScape community vote on how the game should - or " +
                        "shouldn't - change. When you're ready, move on through the door " +
                        "indicated.",
                )
                columnCoord(HINT_COORD, CoordGrid(3125, 3124, 0))
                column(HINT_HEIGHT, 128)
                column(HINT_POSITION, WEST)
                column(PROGRESS, 15)
            }
            row("dbrow.tutorial_island_step_530") {
                column(STAGE, 530)
                column(
                    OVERLAY,
                    "<col=0000ff>Account Management</col><br>The guide here will tell you " +
                        "all about your account. Just click on him to hear what he's got to " +
                        "say.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_account_instructor")
                column(PROGRESS, 15)
            }
            row("dbrow.tutorial_island_step_531") {
                column(STAGE, 531)
                column(
                    OVERLAY,
                    "<col=0000ff>Account Management</col><br>Click on the flashing icon to " +
                        "open your Account Management menu.",
                )
                column(FLASH_TAB, 8)
                column(PROGRESS, 15)
            }
            row("dbrow.tutorial_island_step_532") {
                column(STAGE, 532)
                column(
                    OVERLAY,
                    "<col=0000ff>Account Management</col><br>This is your Account " +
                        "Management menu where you can control various aspects of your " +
                        "account. Talk to the Account Guide to learn more.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_account_instructor")
                column(SIDE_TAB, 8)
                column(PROGRESS, 15)
            }
            row("dbrow.tutorial_island_step_540") {
                column(STAGE, 540)
                column(
                    OVERLAY,
                    "<col=0000ff>Moving on</col><br>Continue through the next door.",
                )
                columnCoord(HINT_COORD, CoordGrid(3130, 3124, 0))
                column(HINT_HEIGHT, 128)
                column(HINT_POSITION, WEST)
                column(OVERLAY_HINT, 17)
                column(PROGRESS, 16)
            }
            row("dbrow.tutorial_island_step_550") {
                column(STAGE, 550)
                columnRSCM(HINT_NPC, "npc.brother_noob")
                column(PROGRESS, 16)
            }
            row("dbrow.tutorial_island_step_560") {
                column(STAGE, 560)
                column(
                    OVERLAY,
                    "<col=0000ff>Prayer menu</col><br>Click on the flashing icon to open " +
                        "the Prayer menu.",
                )
                column(FLASH_TAB, 5)
                column(PROGRESS, 16)
            }
            row("dbrow.tutorial_island_step_570") {
                column(STAGE, 570)
                column(
                    OVERLAY,
                    "<col=0000ff>Prayer menu</col><br>Talk with Brother Brace and he'll " +
                        "tell you about prayers.",
                )
                columnRSCM(HINT_NPC, "npc.brother_noob")
                column(SIDE_TAB, 5)
                column(PROGRESS, 16)
            }
            row("dbrow.tutorial_island_step_580") {
                column(STAGE, 580)
                column(PROGRESS, 17)
            }
            row("dbrow.tutorial_island_step_610") {
                column(STAGE, 610)
                column(
                    OVERLAY,
                    "<col=0000ff>Your final instructor!</col><br>You're almost finished on " +
                        "tutorial island. Pass through the door to find the path leading to " +
                        "your final instructor.",
                )
                columnCoord(HINT_COORD, CoordGrid(3122, 3102, 0))
                column(HINT_HEIGHT, 128)
                column(HINT_POSITION, NORTH)
                column(OVERLAY_HINT, 18)
                column(PROGRESS, 18)
            }
            row("dbrow.tutorial_island_step_620") {
                column(STAGE, 620)
                column(
                    OVERLAY,
                    "<col=0000ff>Your final instructor!</col><br>Follow the path to the " +
                        "wizard's house, where you will be shown how to cast spells. When you " +
                        "get there, just talk with the magic instructor.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_magic_instructor")
                column(PROGRESS, 18)
            }
            row("dbrow.tutorial_island_step_630") {
                column(STAGE, 630)
                column(
                    OVERLAY,
                    "<col=0000ff>Open up your final menu</col><br>Open up the magic " +
                        "interface by clicking on the flashing icon.",
                )
                column(FLASH_TAB, 6)
                column(PROGRESS, 18)
            }
            row("dbrow.tutorial_island_step_640") {
                column(STAGE, 640)
                column(
                    OVERLAY,
                    "<col=0000ff>Magic</col><br>This is your magic interface. All of your " +
                        "spells can be found here. The list is currently filtered to only show " +
                        "the spells you have the Magic level for. You can change this in the " +
                        "filter menu. Talk to the instructor to continue.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_magic_instructor")
                column(SIDE_TAB, 6)
                column(PROGRESS, 20)
            }
            row("dbrow.tutorial_island_step_650") {
                column(STAGE, 650)
                column(
                    OVERLAY,
                    "<col=0000ff>Magic casting</col><br>You now have some runes. All " +
                        "spells require runes to cast them. Look for the Wind Strike spell in " +
                        "your magic interface. Click on this spell to select it and then click " +
                        "on a chicken to cast it. Talk to the instructor if you need more " +
                        "runes.",
                )
                columnRSCM(HINT_NPC, "npc.newbiechicken")
                column(OVERLAY_HINT, 19)
                column(SIDE_TAB, 6)
                column(PROGRESS, 20)
            }
            row("dbrow.tutorial_island_step_660") {
                column(STAGE, 660)
                column(PROGRESS, 20)
            }
            row("dbrow.tutorial_island_step_670") {
                column(STAGE, 670)
                column(
                    OVERLAY,
                    "<col=0000ff>To the mainland!</col><br>You're nearly finished with the " +
                        "tutorial. All you need to do now is move on to the mainland. Just " +
                        "speak with the magic instructor to continue.",
                )
                columnRSCM(HINT_NPC, "npc.newbie_magic_instructor")
                column(OVERLAY_HINT, 20)
                column(SIDE_TAB, 6)
                column(PROGRESS, 20)
            }
            row("dbrow.tutorial_island_step_671") {
                column(STAGE, 671)
                column(OVERLAY_HINT, 23)
                column(PROGRESS, 20)
            }
            row("dbrow.tutorial_island_step_680") {
                column(STAGE, 680)
                column(
                    OVERLAY,
                    "<col=0000ff>To the mainland!</col><br>You're nearly finished with the " +
                        "tutorial. All you need to do now is move on to the mainland. Cast " +
                        "your Home Teleport spell to be taken to Lumbridge.",
                )
                column(OVERLAY_HINT, 23)
                column(PROGRESS, 20)
            }
        }
}
