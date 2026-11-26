package info.mudbourn.mmsdungeons.mobs.knightquest;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class KnightQuestSounds {

    public static SoundEvent THE_ARCHITECT_OF_CHAOS;

    private KnightQuestSounds() {
    }

    public static void register() {
        THE_ARCHITECT_OF_CHAOS = register("the_architect_of_chaos");
    }

    private static SoundEvent register(String name) {
        Identifier id = MmsDungeons.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
