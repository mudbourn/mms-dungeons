package info.mudbourn.mmsdungeons.mobs.luminous;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class LuminousSounds {

    public static SoundEvent CRIMSON_GROWL;
    public static SoundEvent CRIMSON_EXPLOSION;

    private LuminousSounds() {
    }

    public static void register() {
        CRIMSON_GROWL = register("crimson_growl");
        CRIMSON_EXPLOSION = register("crimson_explosion");
    }

    private static SoundEvent register(String name) {
        Identifier id = MmsDungeons.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
