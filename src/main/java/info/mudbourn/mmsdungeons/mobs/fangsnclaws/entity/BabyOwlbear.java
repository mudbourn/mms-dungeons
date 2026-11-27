package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

// An owlbear cub that grows into an owlbear.
public class BabyOwlbear extends BabyMob {

    public BabyOwlbear(EntityType<? extends BabyOwlbear> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 12.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected EntityType<? extends Mob> adultType() {
        return FangsNClawsMobs.OWLBEAR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FangsNClawsSounds.BABY_OWLBEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FangsNClawsSounds.BABY_OWLBEAR_DEATH;
    }
}
