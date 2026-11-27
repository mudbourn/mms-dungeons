package info.mudbourn.mmsdungeons.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.effect.CorrodedEffect;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.effect.FlamebrandEffect;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.effect.MithridaticEffect;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.effect.VenomEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

// The status effects Fangs 'n Claws mobs apply.
public final class FangsNClawsEffects {

    public static Holder<MobEffect> VENOM;
    public static Holder<MobEffect> FLAMEBRAND;
    public static Holder<MobEffect> HELLFIRE_FLAMEBRAND;
    public static Holder<MobEffect> MITHRIDATIC;
    public static Holder<MobEffect> CORRODED;

    private static final Identifier BLEED = Identifier.fromNamespaceAndPath("spell_engine", "bleed");

    private FangsNClawsEffects() {
    }

    public static void register() {
        VENOM = effect("venom", new VenomEffect());
        FLAMEBRAND = effect("flamebrand", new FlamebrandEffect(0xFFFF5000, 3.5f, 8.0f));
        HELLFIRE_FLAMEBRAND = effect("hellfire_flamebrand", new FlamebrandEffect(0xFFFF0000, 5.0f, 12.0f));
        MITHRIDATIC = effect("mithridatic", new MithridaticEffect());
        CORRODED = effect("corroded", new CorrodedEffect());
    }

    // Applies Venom to a target unless it is Mithridatic.
    public static void venom(LivingEntity target, int ticks) {
        if (!target.hasEffect(MITHRIDATIC)) {
            target.addEffect(new MobEffectInstance(VENOM, ticks, 0));
        }
    }

    // Ends Venom on a target, for the scorpion armor that cures it.
    public static void cureVenom(LivingEntity target) {
        target.removeEffect(VENOM);
    }

    // Applies Spell Engine's Bleed to a target, when that mod is loaded.
    public static void bleed(LivingEntity target, int ticks, int amplifier) {
        BuiltInRegistries.MOB_EFFECT.get(BLEED).ifPresent(
            (Holder<MobEffect> effect) -> target.addEffect(new MobEffectInstance(effect, ticks, amplifier))
        );
    }

    // Adds a Flamebrand stack to a target.
    public static void flamebrand(LivingEntity target, LivingEntity source) {
        FlamebrandEffect.stack(target, source, FLAMEBRAND);
    }

    // Adds a Hellfire Flamebrand stack to a target.
    public static void hellfireFlamebrand(LivingEntity target, LivingEntity source) {
        FlamebrandEffect.stack(target, source, HELLFIRE_FLAMEBRAND);
    }

    private static Holder<MobEffect> effect(String name, MobEffect effect) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MmsDungeons.id(name), effect);
    }
}
