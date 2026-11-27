# Creature kinds

Every hostile or neutral mob that can appear in the pack belongs to one or more kinds. A kind is an entity type tag at `data/mms_dungeons/tags/entity_type/kind/<kind>.json`, so the tag id is `mms_dungeons:kind/<kind>`. A mob may carry several kinds, and a kind may include another tag with a `#` entry.

Third-party entries are optional, so a missing mod only leaves its entries out. Mobs that are in no kind take no part in relations.

## Kinds

| Kind | Members | Meaning |
|---|---|---|
| `undead` | 61 plus tag | Risen dead, skeletons, ghosts and wraiths. Kobolds feud with them. |
| `kobold` | 8 | Every living kobold. The undead kobolds (Skelebold, Zombold, Witherbold) are undead. |
| `human` | 4 | The faction humans and the Lunatic cult. They treat players and monsters alike as enemies. |
| `illager` | 13 plus tag | Illagers, raiders and their imitators. |
| `dragon` | 4 | Hunting dragons. Useless Reptile's Moleclaw is a reptile but not a dragon. |
| `reptile` | 17 | Reptiles, dinosaurs and dragons. |
| `beast` | 36 | Wild animals and animal-like monsters. |
| `arthropod` | 26 | Spiders, scorpions, insects and crustaceans. |
| `construct` | 34 | Golems, automatons and other animated objects. |
| `spirit` | 21 | Ghosts, spirits and elementals. |
| `demon` | 22 | Nether creatures and fiends. |
| `aberration` | 24 | Eldritch, alien and warped creatures. |
| `ooze` | 13 | Slimes, gels and tar. |
| `plant` | 13 | Trees, fungi and plant creatures. |
| `aquatic` | 19 | Creatures of the water. |
| `flying` | 32 | Creatures that fly. |
| `giant` | 11 | Ogres, trolls and other very large creatures. |
| `goblin` | 4 | Goblins, gremlins and imps. |
| `explosive` | 8 | Creepers and other things that blow up. |

### undead

Risen dead, skeletons, ghosts and wraiths. Kobolds feud with them.

Includes the tag `#minecraft:undead`.

- `minecraft`: zombie, husk, drowned, zombie_villager, zombified_piglin, skeleton, stray, bogged, wither_skeleton, wither, phantom, zoglin, skeleton_horse, zombie_horse, zombie_nautilus, camel_husk, parched, giant
- `mms_dungeons`: cherry_skeleton, cherry_zombie, crimson_walker, dark_oak_skeleton, dark_oak_zombie, hollow_skeleton, icy_skeleton, icy_zombie, miner_skeleton, miner_zombie, revenant_skeleton, savanna_skeleton, savanna_zombie, sunken_skeleton, swamp_skeleton, swamp_zombie, silver_skeleton, skeleton_horse, zombie_horse, nightmare_horse, ghost, fire_ghost, eld_knight, fallen_knight, ghosty, samhain, wisp, wraith, kobold_skeleton, kobold_zombie, witherbold
- `mutantmonsters`: mutant_skeleton, mutant_zombie, skull_spirit
- `aerialhell`: fat_phantom, mummy, verdigris_zombie, shadow_flying_skull
- `alexscaves`: forsaken
- `undergroundworlds`: frozen_zombie, glow_bogged, jungle_zombie, wrapped

### kobold

Every living kobold. The undead kobolds (Skelebold, Zombold, Witherbold) are in undead.

- `mms_dungeons`: kobold, kobold_child, kobold_captain, kobold_enchanter, kobold_engineer, kobold_pirate, kobold_rascal, kobold_warrior (the undead kobold_skeleton, kobold_zombie and witherbold are undead)

### human

The faction humans and the Lunatic cult. They treat players and monsters alike as enemies.

- `mms_dungeons`: bandit, mercenary, dark_guildsman
- `aerialhell`: lunatic_priest

### illager

Illagers, raiders and their imitators.

Includes the tag `#minecraft:illager`.

- `minecraft`: pillager, vindicator, evoker, illusioner, witch, ravager
- `friendsandfoes`: iceologer, illusioner
- `frostiful`: chillager, frostologer
- `takesapillage`: archer, legioner, skirmisher

### dragon

Hunting dragons. Useless Reptile's Moleclaw is a reptile but not a dragon.

- `uselessreptile`: wyvern, lightning_chaser, magmamuncher, river_pikehorn

### reptile

Reptiles, dinosaurs and dragons.

- `uselessreptile`: wyvern, lightning_chaser, magmamuncher, river_pikehorn, moleclaw
- `mms_dungeons`: lizzy, momma_lizzy
- `alexscaves`: atlatitan, grottoceratops, luxtructosaurus, relicheirus, subterranodon, tremorsaurus, tremorzilla, vallumraptor
- `aerialhell`: venomous_snake
- `wizards`: fire_hydra

### beast

Wild animals and animal-like monsters.

- `minecraft`: wolf, polar_bear, panda, fox, goat, llama, trader_llama, hoglin
- `mms_dungeons`: baby_hyena, hyena, baby_owlbear, owlbear, baby_shrike, shrike, baby_wild_wolf, wild_wolf, evil_bat, horse_bat, nightmare_horse, werewolf, ratman, craw
- `alexscaves`: candicorn, corrodent, gummy_bear, hullbreaker, raycat, sea_pig, vesper
- `aerialhell`: evil_cow, stellar_boar, stellar_chicken, worm
- `direbats`: direbat
- `friendsandfoes`: mauler
- `mutantmonsters`: spider_pig

### arthropod

Spiders, scorpions, insects and crustaceans.

- `minecraft`: spider, cave_spider, silverfish, endermite, bee
- `mms_dungeons`: baby_scorpion, scorpion, desert_scorpion, frost_scorpion, nether_scorpion
- `alexscaves`: gammaroach, gloomoth, gossamer_worm, trilocaris
- `aerialhell`: crystal_caterpillar, crystal_spider, forest_caterpillar, hell_spider, shadow_spider
- `enderscape`: rubblemite, rustle
- `netherdescent`: hornet
- `mutantmonsters`: spider_pig
- `undergroundworlds`: black_recluse, spider_hatchling, hatchling_spider

### construct

Golems, automatons and other animated objects.

- `minecraft`: iron_golem, snow_golem, copper_golem
- `mms_dungeons`: golem, ice_golem, eld_bomb, the_architect_of_chaos
- `alexscaves`: boundroid, boundroid_winch, caniac, gingerbread_man, gumbeeper, magnetron, mine_guardian, notor, quarry_smasher, teletor, tephra
- `aerialhell`: chained_god, crystal_golem, mud_cycle_mage, mud_golem, mud_soldier, mud_spectral_cycle_mage, mud_spectral_golem, mud_spectral_soldier, shadow_automaton, stellar_stone_automaton, volucite_golem, volucite_warden
- `friendsandfoes`: copper_golem, tuff_golem
- `mutantmonsters`: mutant_snow_golem
- `takesapillage`: clay_golem

### spirit

Ghosts, spirits and elementals.

- `minecraft`: vex, breeze
- `mms_dungeons`: ghost, fire_ghost, ghosty, wisp, wraith
- `alexscaves`: dinosaur_spirit
- `aerialhell`: electro_spirit, fire_spirit, ice_spirit, kodama, mud_spectral_cycle_mage, mud_spectral_golem, mud_spectral_soldier, ghost_slime_pirate, ghost_slime_ninja_pirate, shadow_flying_skull
- `mutantmonsters`: skull_spirit
- `wizards`: frost_elemental
- `variantsandventures`: gelid

### demon

Nether creatures and fiends.

- `minecraft`: blaze, ghast, piglin, piglin_brute, hoglin, zombified_piglin, magma_cube, wither
- `mms_dungeons`: imp, hell_ogre, nether_scorpion, nightmare_horse, crimson_walker, netherman, netherman_clone, ghastling
- `aerialhell`: lilith
- `friendsandfoes`: wildfire
- `netherdescent`: hornet, pendorite_blaze, soul_blaze, soul_ghast

### aberration

Eldritch, alien and warped creatures.

- `minecraft`: enderman, warden, creaking, shulker, ender_dragon
- `mms_dungeons`: mimic, purple_worm, the_architect_of_chaos
- `alexscaves`: brainiac, deep_one, deep_one_knight, deep_one_mage, floater, gum_worm, licowitch, nucleeper, underzealot, watcher
- `aerialhell`: flying_jellyfish, lilith
- `enderscape`: drifter
- `mutantmonsters`: mutant_creeper, mutant_enderman
- `variantsandventures`: murk

### ooze

Slimes, gels and tar.

- `minecraft`: slime, magma_cube, sulfur_cube
- `mms_dungeons`: tar_cube, sap
- `alexscaves`: caramel_cube, ferrouslime, gum_worm
- `aerialhell`: crystal_slime, ghost_slime_pirate, ghost_slime_ninja_pirate, slime_pirate, slime_ninja_pirate

### plant

Trees, fungi and plant creatures.

- `minecraft`: creaking
- `mms_dungeons`: bad_patch, swampman
- `aerialhell`: aerial_tree_mimic, copper_pine_mimic, golden_beech_mimic, kodama, shadow_pine_mimic, shroomboom, sky_cactus_fiber_mimic, stellar_ent
- `variantsandventures`: thicket, verdant

### aquatic

Creatures of the water.

- `minecraft`: guardian, elder_guardian, drowned, pufferfish, dolphin, nautilus, zombie_nautilus
- `mms_dungeons`: sunken_skeleton
- `alexscaves`: deep_one, deep_one_knight, deep_one_mage, hullbreaker, lanternfish, radgill, sea_pig, sweetish_fish, tripodfish
- `frostiful`: biter
- `undergroundworlds`: abyssal_guardian

### flying

Creatures that fly.

- `minecraft`: phantom, bat, ghast, vex, blaze, breeze, bee
- `mms_dungeons`: evil_bat, horse_bat, ghastling, craw, shrike, baby_shrike, wraith, wisp
- `alexscaves`: gloomoth, subterranodon, vesper, floater
- `aerialhell`: fat_phantom, flying_jellyfish, shadow_flying_skull, stellar_chicken
- `direbats`: direbat
- `enderscape`: drifter, rustle
- `friendsandfoes`: wildfire
- `netherdescent`: hornet, pendorite_blaze, soul_blaze, soul_ghast
- `mutantmonsters`: skull_spirit

### giant

Ogres, trolls and other very large creatures.

- `minecraft`: giant, ravager
- `mms_dungeons`: cave_ogre, ogre, hell_ogre
- `alexscaves`: atlatitan, luxtructosaurus, tremorsaurus, tremorzilla
- `aerialhell`: shadow_troll, stellar_ent

### goblin

Goblins, gremlins and imps.

- `mms_dungeons`: goblin, dart_goblin, gremlin, imp

### explosive

Creepers and other things that blow up.

- `minecraft`: creeper
- `mms_dungeons`: eld_bomb
- `alexscaves`: gumbeeper, nucleeper
- `aerialhell`: shroomboom
- `mutantmonsters`: mutant_creeper, creeper_minion
- `undergroundworlds`: icy_creeper

A kind named `player` is implied for every player. Entities that are in no kind are ignored.

## Relations file

`data/mms_dungeons/mob_relations/relations.json` is read on every data reload (`/reload`). Packs stack: the file from every enabled data pack is merged in pack order, later packs overriding single cells, so a pack can add feuds without touching this one.

```json
{
  "kinds": ["undead", "kobold"],
  "treated_as_player": ["human"],
  "relations": {
    "kobold": {
      "*": "neutral",
      "undead": "hostile"
    }
  }
}
```

- `kinds`: the kind names to look up. A kind not listed here is ignored.
- `relations`: for each kind, how it sees other kinds. `hostile` makes members of that kind go after members of the other kind on sight. `allied` makes them never fight each other and forgive any accidental or deliberate hit between them. `neutral` leaves things as they are. The key `"*"` is the answer for every kind (and `player`) without an entry of its own.
- `treated_as_player`: kinds that every mob hostile to players also treats as an enemy, unless a relations entry says otherwise.
- A relation is read from the viewer to the target, so a feud needs an entry in both directions to be mutual. A mob with several kinds is hostile if any pair of its kinds and the target kinds is hostile; an entry for a specific kind beats `"*"`.

## Matrix

Rows are the viewer, columns the target. `H` hostile, `A` allied, `N` neutral, `.` nothing set (neutral). Edit the JSON file, then run `/reload`.

| viewer \ target | `undead` | `kobold` | `human` | `illager` | `dragon` | `reptile` | `beast` | `arthropod` | `construct` | `spirit` | `demon` | `aberration` | `ooze` | `plant` | `aquatic` | `flying` | `giant` | `goblin` | `explosive` | `player` |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `undead` | . | H | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `kobold` | H | N | N | N | N | N | N | N | N | N | N | N | N | N | N | N | N | N | N | N |
| `human` | H | H | A | H | H | H | H | H | H | H | H | H | H | H | H | H | H | H | H | H |
| `illager` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `dragon` | H | H | H | H | H | A | H | H | H | H | H | H | H | H | H | H | H | H | H | H |
| `reptile` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `beast` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `arthropod` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `construct` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `spirit` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `demon` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `aberration` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `ooze` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `plant` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `aquatic` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `flying` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `giant` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `goblin` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |
| `explosive` | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . | . |

Mobs in no row use their own default behaviour. Every monster that is hostile to players is also hostile to the `treated_as_player` kinds (humans) wherever its row has no entry for them.

## Rules built on the kinds

- Kobolds are neutral to every mob except undead, and keep their feud with players who loot their dens.
- Humans are hostile to every kind and to players, except other humans. They stay neutral to players whose gear is better than theirs until attacked.
- Dragons are hostile to every kind and to players except reptiles.
- Moleclaws ignore the matrix: they are neutral to a mob unless it is within 2 blocks or carries a light, and neutral to a player unless the player carries a light. The entity type tag `mms_dungeons:light_wary` lists them.
- In the dungeon, monsters that hit each other by accident only fight if the kinds are hostile.
- Light sources are the item tag `mms_dungeons:light_sources` plus every block item that emits light.
