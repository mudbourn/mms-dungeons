package info.mudbourn.mmsdungeons.mobs.luminous;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.luminous.effect.HemorrhageEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

public final class LuminousEffects {

    public static Holder<MobEffect> HEMORRHAGE;

    private LuminousEffects() {
    }

    public static void register() {
        HEMORRHAGE = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            MmsDungeons.id("hemorrhage"),
            new HemorrhageEffect()
        );
    }
}
