package info.mudbourn.mmsdungeons.mobs.kobolds;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class KoboldSounds {

    public static SoundEvent TRADE;
    public static SoundEvent PURR;
    public static SoundEvent DEATH;
    public static SoundEvent HURT;
    public static SoundEvent IDLE;
    public static SoundEvent YIP;
    public static SoundEvent MUSIC_KOBBLESTONE;
    public static SoundEvent SKELEBOLD_IDLE;
    public static SoundEvent SKELEBOLD_HURT;
    public static SoundEvent SKELEBOLD_DEATH;
    public static SoundEvent WITHERBOLD_IDLE;
    public static SoundEvent WITHERBOLD_HURT;
    public static SoundEvent WITHERBOLD_DEATH;
    public static SoundEvent ZOMBOLD_IDLE;
    public static SoundEvent ZOMBOLD_HURT;
    public static SoundEvent ZOMBOLD_DEATH;

    private KoboldSounds() {
    }

    public static void register() {
        TRADE = register("kobold_trade");
        PURR = register("kobold_purr");
        DEATH = register("kobold_death");
        HURT = register("kobold_hurt");
        IDLE = register("kobold_idle");
        YIP = register("kobold_yip");
        MUSIC_KOBBLESTONE = register("music_kobblestone");
        SKELEBOLD_IDLE = register("skelebold_idle");
        SKELEBOLD_HURT = register("skelebold_hurt");
        SKELEBOLD_DEATH = register("skelebold_death");
        WITHERBOLD_IDLE = register("witherbold_idle");
        WITHERBOLD_HURT = register("witherbold_hurt");
        WITHERBOLD_DEATH = register("witherbold_death");
        ZOMBOLD_IDLE = register("zombold_idle");
        ZOMBOLD_HURT = register("zombold_hurt");
        ZOMBOLD_DEATH = register("zombold_death");
    }

    private static SoundEvent register(String name) {
        Identifier id = MmsDungeons.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
