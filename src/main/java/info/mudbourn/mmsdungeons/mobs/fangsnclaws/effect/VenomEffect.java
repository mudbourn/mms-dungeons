package info.mudbourn.mmsdungeons.mobs.fangsnclaws.effect;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

// Deals no damage: slows by 15% and makes a venomed mob drop its target.
public class VenomEffect extends MobEffect {

    private static final double SLOWDOWN = -0.15;

    public VenomEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF9400D3);
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            MmsDungeons.id("venom_slowness"),
            SLOWDOWN,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob && mob.getTarget() != null) {
            mob.setTarget(null);
        }
        return true;
    }
}
