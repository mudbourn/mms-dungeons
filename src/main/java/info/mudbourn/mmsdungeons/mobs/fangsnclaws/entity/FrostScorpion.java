package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

// An icy scorpion whose hits slow their target, with a Stray for a rider.
public class FrostScorpion extends Scorpion {

    private static final int SLOW_TICKS = 100;

    public FrostScorpion(EntityType<? extends FrostScorpion> type, Level level) {
        super(type, level);
    }

    @Override
    protected EntityType<? extends Mob> jockeyType() {
        return EntityTypes.STRAY;
    }

    @Override
    protected int babyVariant() {
        return 2;
    }

    @Override
    protected void applyHitEffect(LivingEntity target, boolean stinger) {
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, 0));
    }
}
