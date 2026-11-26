package info.mudbourn.mmsdungeons.mobs.knightquest.entity.netherman;

import info.mudbourn.mmsdungeons.mobs.knightquest.KnightQuestMobs;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Netherman;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.NethermanClone;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

// One timed cast: teleport, root, wind up, release, strike, then repeat.
public class NethermanCastGoal extends Goal {

    public enum Spell {
        FLAME(1, 200, Netherman.ACTION_FLAME, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundEvents.FIRECHARGE_USE, ParticleTypes.FLAME),
        ICE(2, 200, Netherman.ACTION_FLAME, SoundEvents.POWDER_SNOW_BREAK, SoundEvents.SNOW_GOLEM_AMBIENT, ParticleTypes.SNOWFLAKE),
        CLONES(2, 300, Netherman.ACTION_CLONES, SoundEvents.POWDER_SNOW_BREAK, SoundEvents.EVOKER_PREPARE_SUMMON, ParticleTypes.SNOWFLAKE),
        DARKNESS(3, 200, Netherman.ACTION_DARKNESS, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundEvents.FIRECHARGE_USE, ParticleTypes.SMOKE);

        private final int phase;
        private final int period;
        private final int action;
        private final SoundEvent windup;
        private final SoundEvent release;
        private final ParticleOptions particle;

        Spell(int phase, int period, int action, SoundEvent windup, SoundEvent release, ParticleOptions particle) {
            this.phase = phase;
            this.period = period;
            this.action = action;
            this.windup = windup;
            this.release = release;
            this.particle = particle;
        }
    }

    private static final double RANGE_SQUARED = 4096.0;
    private static final int TELEPORT_AT = 35;
    private static final int ROOT_AT = 30;
    private static final int WINDUP_AT = 20;
    private static final int RELEASE_AT = 10;
    private static final int STRIKE_AT = 5;
    private static final double TELEPORT_MIN = 10.0;
    private static final double TELEPORT_MAX = 30.0;
    private static final int CLONE_COUNT = 4;
    private static final double CLONE_RANGE = 30.0;
    private static final int BURN_MIN_SECONDS = 3;
    private static final int BURN_MAX_SECONDS = 7;
    private static final int FREEZE_TICKS = 300;

    private final Netherman netherman;
    private final Spell spell;
    private int chargeTime;

    public NethermanCastGoal(Netherman netherman, Spell spell) {
        this.netherman = netherman;
        this.spell = spell;
    }

    @Override
    public boolean canUse() {
        return this.netherman.getTarget() != null && this.netherman.isFormReady(this.spell.phase);
    }

    @Override
    public void start() {
        this.chargeTime = this.spell.period;
    }

    @Override
    public void stop() {
        this.chargeTime = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.netherman.getTarget();
        if (target == null || this.netherman.distanceToSqr(target) >= RANGE_SQUARED || !this.netherman.hasLineOfSight(target)) {
            this.chargeTime = this.spell.period;
            return;
        }
        if (this.chargeTime > 0) {
            this.chargeTime--;
        }
        if (!(this.netherman.level() instanceof ServerLevel level)) {
            return;
        }
        switch (this.chargeTime) {
            case TELEPORT_AT -> this.netherman.teleportNear(target, TELEPORT_MIN, TELEPORT_MAX);
            case ROOT_AT -> this.netherman.beginAction(this.spell.action);
            case WINDUP_AT -> this.sound(level, this.spell.windup);
            case RELEASE_AT -> this.release(level);
            case STRIKE_AT -> this.strike(level, target);
            case 0 -> {
                this.chargeTime = this.spell.period;
                this.netherman.teleportNear(target, TELEPORT_MIN, TELEPORT_MAX);
            }
            default -> {
            }
        }
    }

    private void sound(ServerLevel level, SoundEvent sound) {
        if (!this.netherman.isSilent()) {
            level.playSound(null, this.netherman.blockPosition(), sound, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    private void release(ServerLevel level) {
        this.sound(level, this.spell.release);
        if (this.spell == Spell.CLONES) {
            this.spawnClones(level);
            return;
        }
        Vec3 look = this.netherman.getLookAngle();
        Vec3 from = this.netherman.getEyePosition().add(look);
        level.sendParticles(this.spell.particle, from.x, from.y, from.z, 20, 0.2, 0.2, 0.2, 0.1);
    }

    private void strike(ServerLevel level, LivingEntity target) {
        switch (this.spell) {
            case FLAME -> target.igniteForSeconds(this.netherman.getRandom().nextIntBetweenInclusive(BURN_MIN_SECONDS, BURN_MAX_SECONDS - 1));
            case ICE -> target.setTicksFrozen(target.getTicksFrozen() + FREEZE_TICKS);
            case DARKNESS -> target.addEffect(
                new MobEffectInstance(
                    MobEffects.DARKNESS,
                    this.netherman.getRandom().nextIntBetweenInclusive(BURN_MIN_SECONDS, BURN_MAX_SECONDS - 1) * 20,
                    0,
                    false,
                    false,
                    false
                )
            );
            default -> {
                return;
            }
        }
        level.sendParticles(this.spell.particle, target.getX(), target.getEyeY(), target.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
    }

    private void spawnClones(ServerLevel level) {
        for (int i = 0; i < CLONE_COUNT; i++) {
            NethermanClone clone = KnightQuestMobs.NETHERMAN_CLONE.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (clone == null) {
                continue;
            }
            Vec3 spot = NethermanSpots.ringSpot(level, clone, this.netherman, 0.0, CLONE_RANGE);
            if (spot == null) {
                clone.discard();
                continue;
            }
            clone.snapTo(spot.x, spot.y, spot.z, this.netherman.getRandom().nextFloat() * 360.0f, 0.0f);
            level.addFreshEntity(clone);
            level.sendParticles(ParticleTypes.SNOWFLAKE, spot.x, spot.y + 1.0, spot.z, 20, 0.5, 1.0, 0.5, 0.1);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
}
