package info.mudbourn.mmsdungeons.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class FangsNClawsSounds {

    public static SoundEvent GOBLIN_AMBIENT;
    public static SoundEvent GOBLIN_HURT;
    public static SoundEvent GOBLIN_DEATH;
    public static SoundEvent GOBLIN_CELEBRATE;
    public static SoundEvent OGRE_AMBIENT;
    public static SoundEvent OGRE_HURT;
    public static SoundEvent OGRE_DEATH;
    public static SoundEvent OGRE_ROAR;
    public static SoundEvent HELL_OGRE_FIRE_BREATH;
    public static SoundEvent MIMIC_OPEN;
    public static SoundEvent MIMIC_DEATH;
    public static SoundEvent DART_HIT;
    public static SoundEvent BLOWGUN_SHOOT;
    public static SoundEvent BACKSTAB_IMPACT;
    public static SoundEvent HYENA_AMBIENT;
    public static SoundEvent HYENA_HURT;
    public static SoundEvent HYENA_DEATH;
    public static SoundEvent WOLF_GROWL;
    public static SoundEvent WOLF_HURT;
    public static SoundEvent WOLF_DEATH;
    public static SoundEvent OWLBEAR_HOWL;
    public static SoundEvent OWLBEAR_SLASH;
    public static SoundEvent OWLBEAR_FLAP;
    public static SoundEvent OWLBEAR_HURT;
    public static SoundEvent OWLBEAR_DEATH;
    public static SoundEvent BABY_OWLBEAR_HURT;
    public static SoundEvent BABY_OWLBEAR_DEATH;
    public static SoundEvent WEREWOLF_AMBIENT;
    public static SoundEvent WEREWOLF_HOWL;
    public static SoundEvent WEREWOLF_BITE;
    public static SoundEvent WEREWOLF_HURT;
    public static SoundEvent WEREWOLF_DEATH;
    public static SoundEvent SCORPION_HURT;
    public static SoundEvent SCORPION_DEATH;
    public static SoundEvent SCORPION_STINGER;
    public static SoundEvent SCORPION_CLAW1;
    public static SoundEvent SCORPION_CLAW2;
    public static SoundEvent GOLEM_AMBIENT;
    public static SoundEvent GOLEM_HURT;
    public static SoundEvent GOLEM_DEATH;
    public static SoundEvent PURPLE_WORM_AMBIENT;
    public static SoundEvent PURPLE_WORM_HURT;
    public static SoundEvent PURPLE_WORM_DEATH;
    public static SoundEvent PURPLE_WORM_CLAW;
    public static SoundEvent PURPLE_WORM_BITE;
    public static SoundEvent PURPLE_WORM_SPLIT;
    public static SoundEvent PURPLE_WORM_SPLIT_VOLLEY;
    public static SoundEvent PURPLE_WORM_BREATH;
    public static SoundEvent ACID_SPLASH;
    public static SoundEvent ACID_PUDDLE;
    public static SoundEvent PURPLE_WORM_EMERGE;
    public static SoundEvent PURPLE_WORM_HIDE;
    public static SoundEvent PURPLE_WORM_ARM_EMERGE;
    public static SoundEvent PURPLE_WORM_ARM_DIG;

    private FangsNClawsSounds() {
    }

    public static void register() {
        GOBLIN_AMBIENT = register("goblin_ambient");
        GOBLIN_HURT = register("goblin_hurt");
        GOBLIN_DEATH = register("goblin_death");
        GOBLIN_CELEBRATE = register("goblin_celebrate");
        OGRE_AMBIENT = register("ogre_ambient");
        OGRE_HURT = register("ogre_hurt");
        OGRE_DEATH = register("ogre_death");
        OGRE_ROAR = register("ogre_roar");
        HELL_OGRE_FIRE_BREATH = register("hell_ogre_fire_breath");
        MIMIC_OPEN = register("mimic_open");
        MIMIC_DEATH = register("mimic_death");
        DART_HIT = register("dart_hit");
        BLOWGUN_SHOOT = register("blowgun_shoot");
        BACKSTAB_IMPACT = register("backstab_impact");
        HYENA_AMBIENT = register("hyena_ambient");
        HYENA_HURT = register("hyena_hurt");
        HYENA_DEATH = register("hyena_death");
        WOLF_GROWL = register("wild_wolf_growl");
        WOLF_HURT = register("wild_wolf_hurt");
        WOLF_DEATH = register("wild_wolf_death");
        OWLBEAR_HOWL = register("owlbear_howl");
        OWLBEAR_SLASH = register("owlbear_slash");
        OWLBEAR_FLAP = register("owlbear_flap");
        OWLBEAR_HURT = register("owlbear_hurt");
        OWLBEAR_DEATH = register("owlbear_death");
        BABY_OWLBEAR_HURT = register("baby_owlbear_hurt");
        BABY_OWLBEAR_DEATH = register("baby_owlbear_death");
        WEREWOLF_AMBIENT = register("werewolf_ambient");
        WEREWOLF_HOWL = register("werewolf_howl");
        WEREWOLF_BITE = register("werewolf_bite");
        WEREWOLF_HURT = register("werewolf_hurt");
        WEREWOLF_DEATH = register("werewolf_death");
        SCORPION_HURT = register("scorpion_hurt");
        SCORPION_DEATH = register("scorpion_death");
        SCORPION_STINGER = register("scorpion_stinger");
        SCORPION_CLAW1 = register("scorpion_claw1");
        SCORPION_CLAW2 = register("scorpion_claw2");
        GOLEM_AMBIENT = register("golem_ambient");
        GOLEM_HURT = register("golem_hurt");
        GOLEM_DEATH = register("golem_death");
        PURPLE_WORM_AMBIENT = register("purple_worm_ambient");
        PURPLE_WORM_HURT = register("purple_worm_hurt");
        PURPLE_WORM_DEATH = register("purple_worm_death");
        PURPLE_WORM_CLAW = register("purple_worm_claw");
        PURPLE_WORM_BITE = register("purple_worm_bite");
        PURPLE_WORM_SPLIT = register("purple_worm_split");
        PURPLE_WORM_SPLIT_VOLLEY = register("purple_worm_split_volley");
        PURPLE_WORM_BREATH = register("purple_worm_breath");
        ACID_SPLASH = register("acid_splash");
        ACID_PUDDLE = register("acid_puddle");
        PURPLE_WORM_EMERGE = register("purple_worm_emerge");
        PURPLE_WORM_HIDE = register("purple_worm_hide");
        PURPLE_WORM_ARM_EMERGE = register("purple_worm_arm_emerge");
        PURPLE_WORM_ARM_DIG = register("purple_worm_arm_dig");
    }

    private static SoundEvent register(String name) {
        Identifier id = MmsDungeons.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
