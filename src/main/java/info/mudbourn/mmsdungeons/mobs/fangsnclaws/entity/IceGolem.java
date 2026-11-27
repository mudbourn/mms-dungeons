package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

// A Golem of packed snow whose slam freezes and whose uppercut throws its prey into the air.
public class IceGolem extends Golem {

    private static final int FREEZE_TICKS = 140;
    private static final int SLOW_TICKS = 100;
    private static final int SLOW_LEVEL = 1;
    private static final float UPPERCUT_DAMAGE_FACTOR = 0.8f;
    private static final double UPPERCUT_LIFT = 1.5;

    public IceGolem(EntityType<? extends IceGolem> type, Level level) {
        super(type, level);
    }

    @Override
    protected String heavyAnimation() {
        return "attack_uppercut";
    }

    @Override
    protected void landSlam(ServerLevel level, LivingEntity target) {
        super.landSlam(level, target);
        target.setTicksFrozen(target.getTicksFrozen() + FREEZE_TICKS);
    }

    @Override
    protected void landHeavy(ServerLevel level, LivingEntity target) {
        float damage = (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * UPPERCUT_DAMAGE_FACTOR);
        target.hurtServer(level, this.damageSources().mobAttack(this), damage);
        target.push(0.0, UPPERCUT_LIFT, 0.0);
        target.needsSync = true;
        target.setTicksFrozen(target.getTicksFrozen() + FREEZE_TICKS);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, SLOW_LEVEL));
    }
}
