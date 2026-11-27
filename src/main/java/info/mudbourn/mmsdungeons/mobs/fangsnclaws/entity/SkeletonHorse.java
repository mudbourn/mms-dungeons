package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

// A bare-boned horse that spawns with a Skeleton or Silver Skeleton on its back now and then.
public class SkeletonHorse extends UndeadHorse {

    public SkeletonHorse(EntityType<? extends SkeletonHorse> type, Level level) {
        super(type, level);
    }

    @Override
    protected @Nullable Mob createRider(ServerLevelAccessor level) {
        boolean plain = this.random.nextBoolean();
        EntityType<? extends Mob> kind = plain ? EntityTypes.SKELETON : FangsNClawsMobs.SILVER_SKELETON;
        return kind.create(level.getLevel(), EntitySpawnReason.JOCKEY);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_HORSE_DEATH;
    }
}
