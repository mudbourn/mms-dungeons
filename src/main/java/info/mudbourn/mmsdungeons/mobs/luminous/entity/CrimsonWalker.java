package info.mudbourn.mmsdungeons.mobs.luminous.entity;

import info.mudbourn.mmsdungeons.mobs.luminous.LuminousEffects;
import info.mudbourn.mmsdungeons.mobs.luminous.LuminousSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// A fire-proof zombie that growls, and bursts into a bleeding cloud a moment after it dies.
public class CrimsonWalker extends Zombie {

    private static final DustParticleOptions BLOOD = new DustParticleOptions(0xFF940000, 2.0f);
    private static final int BURST_TICKS = 8;
    private static final int BONUS_XP = 5;
    private static final double BURST_RADIUS = 2.5;
    private static final int HEMORRHAGE_TICKS = 240;

    public CrimsonWalker(EntityType<? extends CrimsonWalker> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return LuminousSounds.CRIMSON_GROWL;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return !source.is(DamageTypes.IN_FIRE) && super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            ExperienceOrb.award(level, this.position(), BONUS_XP);
        }
    }

    @Override
    protected void tickDeath() {
        super.tickDeath();
        if (this.deathTime == BURST_TICKS && this.level() instanceof ServerLevel level) {
            this.burst(level);
        }
    }

    private void burst(ServerLevel level) {
        Vec3 center = this.position();
        level.sendParticles(BLOOD, center.x, center.y + 1.0, center.z, 100, 0.8, 0.8, 0.8, 3.0);
        level.sendParticles(BLOOD, center.x, center.y + 1.0, center.z, 50, 0.5, 0.5, 0.5, 3.0);
        level.playSound(null, this.blockPosition(), LuminousSounds.CRIMSON_EXPLOSION, SoundSource.BLOCKS, 0.3f, 1.0f);
        AABB area = new AABB(center, center).inflate(BURST_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target != this) {
                target.addEffect(new MobEffectInstance(LuminousEffects.HEMORRHAGE, HEMORRHAGE_TICKS, 0, false, true));
            }
        }
    }
}
