package info.mudbourn.mmsdungeons.mobs.fangsnclaws.effect;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

// Eats away half of the target's armor and armor toughness.
public class CorrodedEffect extends MobEffect {

    private static final double LOSS = -0.5;

    public CorrodedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF76F154);
        this.addAttributeModifier(
            Attributes.ARMOR,
            MmsDungeons.id("corroded_armor"),
            LOSS,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        this.addAttributeModifier(
            Attributes.ARMOR_TOUGHNESS,
            MmsDungeons.id("corroded_toughness"),
            LOSS,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
