package info.mudbourn.mmsdungeons.mobs.luminous.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

// A cold skeleton that sheds soul wisps.
public class RevenantSkeleton extends ColdSkeleton {

    private static final float SOUL_CHANCE = 0.2f;

    public RevenantSkeleton(EntityType<? extends RevenantSkeleton> type, Level level) {
        super(type, level);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level() instanceof ServerLevel level && this.random.nextFloat() < SOUL_CHANCE) {
            level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY(), this.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }
    }
}
