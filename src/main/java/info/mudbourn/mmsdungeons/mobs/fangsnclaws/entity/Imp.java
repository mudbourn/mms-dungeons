package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEffects;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsGear;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// A fire-proof pitchfork imp that hovers around its target and dashes in for a branding hit.
public class Imp extends Monster {

    private static final EntityDataAccessor<Boolean> FLYING =
        SynchedEntityData.defineId(Imp.class, EntityDataSerializers.BOOLEAN);

    private static final double HOVER_HEIGHT = 1.3;
    private static final double HOVER_BOB = 0.6;
    private static final double HOVER_MAX_RISE = 0.3;
    private static final double CHASE_SPEED = 0.26;
    private static final double ORBIT_SPEED = 0.24;
    private static final double FLEE_SPEED = 0.22;
    private static final double ORBIT_RADIUS = 3.5;
    private static final double ORBIT_STEP = 0.02;
    private static final double CHASE_RANGE = 6.5;
    private static final double DASH_SPEED = 0.9;
    private static final double DASH_MIN = 1.5;
    private static final double DASH_MAX = 4.0;
    private static final int DASH_LAUNCH_TICK = 8;
    private static final int DASH_TICKS = 15;
    private static final int DASH_COOLDOWN = 55;
    private static final int FLEE_TICKS = 15;
    private static final int ORBIT_FLIP_MIN = 80;
    private static final int ORBIT_FLIP_SPREAD = 120;

    private LivingEntity pendingTarget;
    private int dashTick;
    private boolean dashHit;
    private int dashCooldown;
    private int fleeTick;
    private float orbitAngle;
    private int orbitDirection;
    private int orbitFlipTick;

    public Imp(EntityType<? extends Imp> type, Level level) {
        super(type, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(FangsNClawsGear.FIRE_PITCHFORK));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        this.orbitAngle = (float) (this.random.nextDouble() * Math.PI * 2.0);
        this.orbitDirection = this.random.nextBoolean() ? 1 : -1;
        this.orbitFlipTick = ORBIT_FLIP_MIN + this.random.nextInt(ORBIT_FLIP_SPREAD);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 8.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
    }

    public boolean isFlying() {
        return this.entityData.get(FLYING);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return !this.isFlying() && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.dashCooldown > 0) {
            this.dashCooldown--;
        }
        this.tickDash(level);
        this.tickHover(level);
        this.tickCombat();
    }

    private void tickDash(ServerLevel level) {
        if (this.dashTick <= 0) {
            return;
        }
        this.dashTick++;
        boolean live = this.pendingTarget != null && this.pendingTarget.isAlive();
        if (this.dashTick == DASH_LAUNCH_TICK && live) {
            double dx = this.pendingTarget.getX() - this.getX();
            double dz = this.pendingTarget.getZ() - this.getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (flat > 0.01) {
                this.setDeltaMovement(dx / flat * DASH_SPEED, 0.12, dz / flat * DASH_SPEED);
            }
            this.getNavigation().stop();
        }
        boolean touching = live && this.getBoundingBox().inflate(0.15).intersects(this.pendingTarget.getBoundingBox());
        if (this.dashTick >= DASH_LAUNCH_TICK && !this.dashHit && touching) {
            super.doHurtTarget(level, this.pendingTarget);
            FangsNClawsEffects.flamebrand(this.pendingTarget, this);
            this.dashHit = true;
            this.pendingTarget = null;
        }
        if (this.dashTick >= DASH_TICKS) {
            this.dashTick = 0;
            this.dashHit = false;
            this.pendingTarget = null;
            this.fleeTick = FLEE_TICKS;
        }
    }

    private void tickHover(ServerLevel level) {
        boolean engaged = this.getTarget() != null && this.getTarget().isAlive();
        if (engaged != this.isFlying()) {
            this.entityData.set(FLYING, engaged);
            Vec3 motion = this.getDeltaMovement();
            if (!engaged && motion.y < 0.0) {
                this.setDeltaMovement(motion.x, 0.0, motion.z);
            }
            this.resetFallDistance();
        }
        this.setNoGravity(engaged);
        if (!engaged) {
            return;
        }
        this.resetFallDistance();
        double bob = Mth.sin(this.tickCount * 0.08f) * HOVER_BOB;
        double hover = Steering.groundY(level, this, 10, this.getY()) + HOVER_HEIGHT + bob;
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, (hover - this.getY()) * 0.06, 0.0));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.12, 0.0));
        }
        Vec3 motion = this.getDeltaMovement();
        if (Math.abs(motion.y) > HOVER_MAX_RISE) {
            this.setDeltaMovement(motion.x, Mth.clamp(motion.y, -HOVER_MAX_RISE, HOVER_MAX_RISE), motion.z);
        }
    }

    private void tickCombat() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.dashTick > 0) {
            return;
        }
        if (this.fleeTick > 0) {
            this.fleeTick--;
            Vec3 away = this.position().subtract(target.position());
            Vec3 step = away.length() > 0.01 ? away.normalize().scale(1.5) : new Vec3(1.0, 0.0, 0.0);
            Vec3 goal = this.position().add(step);
            Steering.faceToward(this, goal);
            Steering.walkToward(this, goal, FLEE_SPEED, 0.22);
            return;
        }
        double distance = this.distanceTo(target);
        if (this.dashCooldown == 0 && distance >= DASH_MIN && distance <= DASH_MAX) {
            GeoTriggers.trigger(this, "attackFlying");
            this.pendingTarget = target;
            this.dashTick = 1;
            this.dashCooldown = DASH_COOLDOWN;
            return;
        }
        this.orbit(target, distance);
    }

    private void orbit(LivingEntity target, double distance) {
        if (distance > CHASE_RANGE) {
            Steering.faceToward(this, target.position());
            Steering.walkToward(this, target.position(), CHASE_SPEED, 0.2);
            return;
        }
        if (--this.orbitFlipTick <= 0) {
            this.orbitDirection = -this.orbitDirection;
            this.orbitFlipTick = ORBIT_FLIP_MIN + this.random.nextInt(ORBIT_FLIP_SPREAD);
        }
        this.orbitAngle += (float) (ORBIT_STEP * this.orbitDirection);
        Vec3 ring = new Vec3(
            target.getX() + Math.cos(this.orbitAngle) * ORBIT_RADIUS,
            target.getY(),
            target.getZ() + Math.sin(this.orbitAngle) * ORBIT_RADIUS
        );
        Steering.faceToward(this, target.position());
        Steering.walkToward(this, ring, ORBIT_SPEED, 0.15);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return FangsNClawsSounds.GOBLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FangsNClawsSounds.GOBLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FangsNClawsSounds.GOBLIN_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8f;
    }
}
