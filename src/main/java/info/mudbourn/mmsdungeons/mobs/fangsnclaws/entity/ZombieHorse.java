package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

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

// A rotting horse that spawns with a Zombie on its back now and then.
public class ZombieHorse extends UndeadHorse {

    public ZombieHorse(EntityType<? extends ZombieHorse> type, Level level) {
        super(type, level);
    }

    @Override
    protected @Nullable Mob createRider(ServerLevelAccessor level) {
        return EntityTypes.ZOMBIE.create(level.getLevel(), EntitySpawnReason.JOCKEY);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_HORSE_DEATH;
    }
}
