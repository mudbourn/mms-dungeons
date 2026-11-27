package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

// A claw the Purple Worm sends up from the ground under its prey: it rises, swipes, then sinks away.
public class PurpleWormArm extends Monster {

    private static final EntityDataAccessor<Integer> ARM_STATE =
        SynchedEntityData.defineId(PurpleWormArm.class, EntityDataSerializers.INT);

    public static final int IDLE = 0;
    public static final int SPAWN = 1;
    public static final int ATTACK_LEFT = 2;
    public static final int ATTACK_RIGHT = 3;
    public static final int DESPAWN = 4;

    private static final int SPAWN_TICKS = 20;
    private static final int ATTACK_TICKS = 30;
    private static final int ATTACK_HIT_TICK = 15;
    private static final int DESPAWN_TICKS = 20;
    private static final int LIFETIME_TICKS = 200;
    private static final double ATTACK_RANGE = 6.0;
    private static final float ATTACK_DAMAGE = 12.0f;
    private static final double ATTACK_KNOCKBACK = 1.6;
    private static final double ATTACK_LIFT = 0.5;
    private static final int DIRT_PARTICLES = 24;

    private int phaseTick;
    private boolean struck;

    public PurpleWormArm(EntityType<? extends PurpleWormArm> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public int getArmState() {
        return this.entityData.get(ARM_STATE);
    }

    private void setArmState(int state) {
        this.entityData.set(ARM_STATE, state);
        this.phaseTick = 0;
    }

    // Starts the arm's rise from the ground.
    public void emerge() {
        this.setArmState(SPAWN);
        this.playSound(FangsNClawsSounds.PURPLE_WORM_ARM_EMERGE, 1.0f, 1.0f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ARM_STATE, IDLE);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.phaseTick++;
        switch (this.getArmState()) {
            case SPAWN -> this.tickSpawn();
            case IDLE -> this.tickIdle(level);
            case ATTACK_LEFT, ATTACK_RIGHT -> this.tickAttack(level);
            case DESPAWN -> this.tickDespawn();
        }
    }

    private void tickSpawn() {
        if (this.phaseTick >= SPAWN_TICKS) {
            this.setArmState(IDLE);
        }
    }

    private void tickIdle(ServerLevel level) {
        if (this.tickCount >= LIFETIME_TICKS) {
            this.playSound(FangsNClawsSounds.PURPLE_WORM_ARM_DIG, 1.0f, 1.0f);
            this.setArmState(DESPAWN);
            return;
        }
        LivingEntity prey = this.findPrey(level);
        if (prey == null) {
            return;
        }
        this.faceToward(prey);
        this.struck = false;
        boolean right = this.random.nextBoolean();
        this.setArmState(right ? ATTACK_RIGHT : ATTACK_LEFT);
    }

    private void tickAttack(ServerLevel level) {
        if (!this.struck && this.phaseTick >= ATTACK_HIT_TICK) {
            this.struck = true;
            this.clawHit(level);
        }
        if (this.phaseTick >= ATTACK_TICKS) {
            this.setArmState(IDLE);
        }
    }

    private void tickDespawn() {
        if (this.phaseTick >= DESPAWN_TICKS) {
            this.discard();
        }
    }

    private LivingEntity findPrey(ServerLevel level) {
        AABB reach = this.getBoundingBox().inflate(ATTACK_RANGE, 1.0, ATTACK_RANGE);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, reach, this::isPrey)) {
            double distance = candidate.distanceToSqr(this);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private boolean isPrey(LivingEntity entity) {
        if (!entity.isAlive() || PurpleWorm.isKin(entity) || entity instanceof Monster) {
            return false;
        }
        return !(entity instanceof Player player) || !(player.isSpectator() || player.isCreative());
    }

    private void faceToward(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float yaw = (float) (Math.atan2(-dx, dz) * 180.0 / Math.PI);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private void clawHit(ServerLevel level) {
        level.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
            this.getX(),
            this.getY() + 0.2,
            this.getZ(),
            DIRT_PARTICLES,
            0.8,
            0.2,
            0.8,
            0.1
        );
        AABB reach = this.getBoundingBox().inflate(ATTACK_RANGE, 1.0, ATTACK_RANGE);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, reach, this::isPrey)) {
            if (victim.hurtServer(level, this.damageSources().mobAttack(this), ATTACK_DAMAGE)) {
                double dx = victim.getX() - this.getX();
                double dz = victim.getZ() - this.getZ();
                double length = Math.max(Math.sqrt(dx * dx + dz * dz), 1.0E-4);
                victim.push(dx / length * ATTACK_KNOCKBACK, ATTACK_LIFT, dz / length * ATTACK_KNOCKBACK);
                victim.needsSync = true;
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return this.getArmState() != DESPAWN && super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
