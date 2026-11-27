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

// A scorpion hatchling that grows into the scorpion of its coat.
public class BabyScorpion extends BabyMob {

    public BabyScorpion(EntityType<? extends BabyScorpion> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected EntityType<? extends Mob> adultType() {
        return switch (this.getVariant()) {
            case 1 -> FangsNClawsMobs.DESERT_SCORPION;
            case 2 -> FangsNClawsMobs.FROST_SCORPION;
            case 3 -> FangsNClawsMobs.NETHER_SCORPION;
            default -> FangsNClawsMobs.SCORPION;
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FangsNClawsSounds.SCORPION_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FangsNClawsSounds.SCORPION_DEATH;
    }
}
