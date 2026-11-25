package info.mudbourn.mmsdungeons.mobs.kobolds.entity;

import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.level.Level;

public class KoboldSkeleton extends AbstractKoboldSkeleton {

    public KoboldSkeleton(EntityType<KoboldSkeleton> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new RangedCrossbowAttackGoal<>(this, 1.0, 12.0f));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return KoboldSounds.SKELEBOLD_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return KoboldSounds.SKELEBOLD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KoboldSounds.SKELEBOLD_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.SKELETON_STEP;
    }
}
