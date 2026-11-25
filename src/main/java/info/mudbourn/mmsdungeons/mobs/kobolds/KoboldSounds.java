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
    }

    private static SoundEvent register(String name) {
        Identifier id = MmsDungeons.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
