package info.mudbourn.mmsdungeons.mobs.kobolds.entity;

import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class KoboldZombie extends AbstractKoboldZombie {

    public KoboldZombie(EntityType<KoboldZombie> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return KoboldSounds.ZOMBOLD_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return KoboldSounds.ZOMBOLD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KoboldSounds.ZOMBOLD_DEATH;
    }
}
