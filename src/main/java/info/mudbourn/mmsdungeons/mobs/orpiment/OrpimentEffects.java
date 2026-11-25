package info.mudbourn.mmsdungeons.mobs.orpiment;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.effect.PossessedEffect;
import info.mudbourn.mmsdungeons.mobs.orpiment.effect.TarredEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;

public final class OrpimentEffects {

    public static Holder<MobEffect> TARRED;
    public static Holder<MobEffect> POSSESSED;

    private OrpimentEffects() {
    }

    public static void register() {
        TARRED = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            MmsDungeons.id("tarred"),
            new TarredEffect()
                .addAttributeModifier(Attributes.MOVEMENT_SPEED, MmsDungeons.id("tarred_speed"), -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .addAttributeModifier(Attributes.JUMP_STRENGTH, MmsDungeons.id("tarred_jump"), -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .addAttributeModifier(Attributes.WATER_MOVEMENT_EFFICIENCY, MmsDungeons.id("tarred_swim"), -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .addAttributeModifier(Attributes.ATTACK_SPEED, MmsDungeons.id("tarred_attack"), -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, MmsDungeons.id("tarred_knockback"), 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
        );
        POSSESSED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MmsDungeons.id("possessed"), new PossessedEffect());
    }
}
