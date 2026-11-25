package info.mudbourn.mmsdungeons.mobs.kobolds;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;

public final class KoboldPotions {

    public static Holder<Potion> AMETHYST;

    private KoboldPotions() {
    }

    public static void register() {
        AMETHYST = Registry.registerForHolder(
            BuiltInRegistries.POTION,
            MmsDungeons.id("kobold_amethyst"),
            new Potion(
                "kobold_amethyst",
                new MobEffectInstance(MobEffects.INSTANT_HEALTH, 0, 0, false, true),
                new MobEffectInstance(MobEffects.REGENERATION, 1200, 0, false, true)
            )
        );
    }
}
