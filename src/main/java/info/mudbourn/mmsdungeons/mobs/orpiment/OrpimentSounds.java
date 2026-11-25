package info.mudbourn.mmsdungeons.mobs.orpiment;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;

public final class OrpimentSounds {

    public static SoundEvent TAR_CUBE_JUMP;
    public static SoundEvent TAR_CUBE_HURT;
    public static SoundEvent TAR_CUBE_DEATH;
    public static SoundEvent TARRED_SAND_HIT;
    public static SoundEvent TARRED_SAND_BREAK;
    public static SoundEvent TARRED_SANDSTONE_HIT;
    public static SoundEvent TARRED_SANDSTONE_BREAK;
    public static SoundEvent ORPIMENT_AMBER_PLACE;
    public static SoundEvent ORPIMENT_AMBER_BREAK;
    public static SoundEvent MUSIC_DISC_FOSSIL;
    public static SoundEvent TAR_EMPTY;
    public static SoundEvent ORPIMENT_LIGNITE_BREAK;
    public static SoundEvent ORPIMENT_LIGNITE_HIT;
    public static SoundEvent WISP_AMBIENT;
    public static SoundEvent WISP_HURT;
    public static SoundEvent WISP_DEATH;
    public static SoundEvent WRAITH_HURT;
    public static SoundEvent WRAITH_DEATH;
    public static SoundEvent WRAITH_SHOOT;
    public static SoundEvent WRAITH_SMASH;
    public static SoundEvent WRAITH_BITE;
    public static SoundEvent WRAITH_SUMMON;
    public static SoundEvent WRAITH_SWOOP;
    public static SoundEvent WRAITH_AMBIENT;
    public static SoundEvent WRAITH_SPAWN;
    public static SoundEvent ECTOPLASM_SCAFFOLD_BREAK;
    public static SoundEvent ECTOPLASM_SCAFFOLD_HIT;
    public static SoundEvent ECTOPLASM_SCAFFOLD_PLACE;
    public static SoundEvent ORPIMENT_LIGNITE_PLACE;
    public static SoundEvent TARRED_SAND_PLACE;
    public static SoundEvent TARRED_SAND_STEP;
    public static SoundEvent TARRED_SANDSTONE_PLACE;
    public static SoundEvent TARRED_SANDSTONE_STEP;
    public static SoundEvent SAP_FILL;
    public static SoundEvent SAP_EMPTY;
    public static SoundEvent SAP_INSERT;
    public static SoundEvent SAP_JUMP;
    public static SoundEvent SAP_HURT;
    public static SoundEvent SAP_DEATH;
    public static SoundEvent BURST_SPAWNER_ACTIVATE;
    public static SoundEvent RELAY_SPAWNER_CONNECT;
    public static SoundEvent CRAW_EGG_CRACK;
    public static SoundEvent CRAW_EGG_HATCH;
    public static SoundEvent CRAW_HURT;
    public static SoundEvent CRAW_DEATH;
    public static SoundEvent CRAW_STEP;
    public static SoundEvent CRAW_EAT;
    public static SoundEvent CRAW_TIRED;
    public static SoundEvent CRAW_DASH;

    public static SoundType AMBER;
    public static SoundType LIGNITE;
    public static SoundType TARRED_SAND;
    public static SoundType TARRED_SANDSTONE;
    public static SoundType ECTOPLASM_SCAFFOLD;

    private OrpimentSounds() {
    }

    public static void register() {
        TAR_CUBE_JUMP = register("tar_cube_jump");
        TAR_CUBE_HURT = register("tar_cube_hurt");
        TAR_CUBE_DEATH = register("tar_cube_death");
        TARRED_SAND_HIT = register("tarred_sand_hit");
        TARRED_SAND_BREAK = register("tarred_sand_break");
        TARRED_SANDSTONE_HIT = register("tarred_sandstone_hit");
        TARRED_SANDSTONE_BREAK = register("tarred_sandstone_break");
        ORPIMENT_AMBER_PLACE = register("orpiment_amber_place");
        ORPIMENT_AMBER_BREAK = register("orpiment_amber_break");
        MUSIC_DISC_FOSSIL = register("music_disc_fossil");
        TAR_EMPTY = register("tar_empty");
        ORPIMENT_LIGNITE_BREAK = register("orpiment_lignite_break");
        ORPIMENT_LIGNITE_HIT = register("orpiment_lignite_hit");
        WISP_AMBIENT = register("wisp_ambient");
        WISP_HURT = register("wisp_hurt");
        WISP_DEATH = register("wisp_death");
        WRAITH_HURT = register("wraith_hurt");
        WRAITH_DEATH = register("wraith_death");
        WRAITH_SHOOT = register("wraith_shoot");
        WRAITH_SMASH = register("wraith_smash");
        WRAITH_BITE = register("wraith_bite");
        WRAITH_SUMMON = register("wraith_summon");
        WRAITH_SWOOP = register("wraith_swoop");
        WRAITH_AMBIENT = register("wraith_ambient");
        WRAITH_SPAWN = register("wraith_spawn");
        ECTOPLASM_SCAFFOLD_BREAK = register("ectoplasm_scaffold_break");
        ECTOPLASM_SCAFFOLD_HIT = register("ectoplasm_scaffold_hit");
        ECTOPLASM_SCAFFOLD_PLACE = register("ectoplasm_scaffold_place");
        ORPIMENT_LIGNITE_PLACE = register("orpiment_lignite_place");
        TARRED_SAND_PLACE = register("tarred_sand_place");
        TARRED_SAND_STEP = register("tarred_sand_step");
        TARRED_SANDSTONE_PLACE = register("tarred_sandstone_place");
        TARRED_SANDSTONE_STEP = register("tarred_sandstone_step");
        SAP_FILL = register("sap_fill");
        SAP_EMPTY = register("sap_empty");
        SAP_INSERT = register("sap_insert");
        SAP_JUMP = register("sap_jump");
        SAP_HURT = register("sap_hurt");
        SAP_DEATH = register("sap_death");
        BURST_SPAWNER_ACTIVATE = register("burst_spawner_activate");
        RELAY_SPAWNER_CONNECT = register("relay_spawner_connect");
        CRAW_EGG_CRACK = register("craw_egg_crack");
        CRAW_EGG_HATCH = register("craw_egg_hatch");
        CRAW_HURT = register("craw_hurt");
        CRAW_DEATH = register("craw_death");
        CRAW_STEP = register("craw_step");
        CRAW_EAT = register("craw_eat");
        CRAW_TIRED = register("craw_tired");
        CRAW_DASH = register("craw_dash");
        AMBER = new SoundType(
            1.0f,
            1.0f,
            ORPIMENT_AMBER_BREAK,
            SoundEvents.GLASS_STEP,
            ORPIMENT_AMBER_PLACE,
            SoundEvents.STONE_HIT,
            SoundEvents.STONE_FALL
        );
        LIGNITE = new SoundType(
            1.0f,
            1.0f,
            ORPIMENT_LIGNITE_BREAK,
            SoundEvents.STONE_STEP,
            ORPIMENT_LIGNITE_PLACE,
            ORPIMENT_LIGNITE_HIT,
            SoundEvents.STONE_FALL
        );
        TARRED_SAND = new SoundType(
            1.0f,
            1.0f,
            TARRED_SAND_BREAK,
            TARRED_SAND_STEP,
            TARRED_SAND_PLACE,
            TARRED_SAND_HIT,
            SoundEvents.SAND_FALL
        );
        TARRED_SANDSTONE = new SoundType(
            1.0f,
            1.0f,
            TARRED_SANDSTONE_BREAK,
            TARRED_SANDSTONE_STEP,
            TARRED_SANDSTONE_PLACE,
            TARRED_SANDSTONE_HIT,
            SoundEvents.SAND_FALL
        );
        ECTOPLASM_SCAFFOLD = new SoundType(
            1.0f,
            1.0f,
            ECTOPLASM_SCAFFOLD_BREAK,
            SoundEvents.WOOL_STEP,
            ECTOPLASM_SCAFFOLD_PLACE,
            ECTOPLASM_SCAFFOLD_HIT,
            SoundEvents.WOOL_FALL
        );
    }

    private static SoundEvent register(String name) {
        Identifier id = MmsDungeons.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
