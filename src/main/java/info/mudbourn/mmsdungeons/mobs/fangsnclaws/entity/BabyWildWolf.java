package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

// A wolf pup that grows into a wolf with the same coat.
public class BabyWildWolf extends BabyMob {

    public BabyWildWolf(EntityType<? extends BabyWildWolf> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected EntityType<? extends Mob> adultType() {
        return FangsNClawsMobs.WILD_WOLF;
    }

    @Override
    protected void onGrown(Mob adult) {
        if (adult instanceof WildWolf wolf) {
            wolf.setVariant(this.getVariant());
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WOLF_AMBIENT_BABY.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT_BABY.value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH_BABY.value();
    }
}
