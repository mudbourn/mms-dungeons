package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

// A heavy brawler that sleeps when idle, howls at its prey, and dives from the air when it cannot reach them.
public class Owlbear extends StrikeMob {

    private static final EntityDataAccessor<Boolean> SLEEPING =
        SynchedEntityData.defineId(Owlbear.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLYING =
        SynchedEntityData.defineId(Owlbear.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLAPPING =
        SynchedEntityData.defineId(Owlbear.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DIVING =
        SynchedEntityData.defineId(Owlbear.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BLIZZARD =
        SynchedEntityData.defineId(Owlbear.class, EntityDataSerializers.BOOLEAN);

    private static final double CHASE_SPEED = 1.3;
    private static final double STROLL_SPEED = 0.8;
    private static final float LOOK_RANGE = 8.0f;
    private static final int FIRST_HIT_TICK = 15;
    private static final int SECOND_HIT_TICK = 18;
    private static final int SWING_TOTAL_TICKS = 25;
    private static final int SWING_COOLDOWN = 15;
    private static final double SWING_KNOCKBACK = 1.2;
    private static final int HOWL_TICKS = 30;
    private static final int HOWL_COOLDOWN = 600;
    private static final int NO_PATH_TICKS = 50;
    private static final double HOVER_ABOVE = 6.0;
    private static final double FLY_SPEED = 0.45;
    private static final double FLY_BLEND = 0.12;
    private static final double DIVE_SPEED = 0.9;
    private static final double DIVE_MIN_HEIGHT = 5.0;
    private static final double DIVE_MAX_FLAT = 8.0;
    private static final int DIVE_MAX_TICKS = 40;
    private static final int DIVE_COOLDOWN = 100;
    private static final double DIVE_RADIUS = 3.5;
    private static final float DIVE_DAMAGE = 10.0f;
    private static final int DIVE_SMOKE = 28;
    private static final int FLAP_SOUND_TICKS = 12;
    private static final int SLEEP_IDLE_TICKS = 200;
    private static final int SLEEP_ODDS = 150;
    private static final int SLEEP_MIN_TICKS = 1200;
    private static final int SLEEP_SPREAD = 2400;
    private static final double WAKE_RADIUS = 3.0;
    private static final int BABY_ODDS = 5;

    private int howlTicks;
    private int howlCooldown;
    private int noPathTicks;
    private int diveTicks;
    private int diveCooldown;
    private int flapSoundTicks;

    public Owlbear(EntityType<? extends Owlbear> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    public boolean isSleeping() {
        return this.entityData.get(SLEEPING);
    }

    public boolean isFlying() {
        return this.entityData.get(FLYING);
    }

    public boolean isFlapping() {
        return this.entityData.get(FLAPPING);
    }

    public boolean isDiving() {
        return this.entityData.get(DIVING);
    }

    public boolean isBlizzard() {
        return this.entityData.get(BLIZZARD);
    }

    protected void setSleeping(boolean value) {
        this.entityData.set(SLEEPING, value);
    }

    protected void setBlizzard(boolean value) {
        this.entityData.set(BLIZZARD, value);
    }

    protected void setFlapping(boolean value) {
        this.entityData.set(FLAPPING, value);
    }

    protected EntityType<? extends BabyMob> babyType() {
        return FangsNClawsMobs.BABY_OWLBEAR;
    }

    // Whether this kind takes to the air when its prey is out of reach.
    protected boolean divesWhenBlocked() {
        return true;
    }

    @Override
    public boolean isBusy() {
        return super.isBusy() || this.howlTicks > 0 || this.isFlying() || this.isSleeping();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SLEEPING, false);
        builder.define(FLYING, false);
        builder.define(FLAPPING, false);
        builder.define(DIVING, false);
        builder.define(BLIZZARD, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SleepGoal());
        this.goalSelector.addGoal(2, new StrikeGoal(this, CHASE_SPEED));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, STROLL_SPEED));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, LOOK_RANGE));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return !Owlbear.this.isSleeping() && super.canUse() && !this.isSneaking();
            }

            @Override
            public boolean canContinueToUse() {
                return super.canContinueToUse() && !this.isSneaking();
            }

            private boolean isSneaking() {
                return this.target instanceof Player player && player.isCrouching();
            }
        });
    }

    @Override
    public void startAttack(LivingEntity target) {
        boolean first = this.useFirstSwing();
        this.beginStrike(
            target,
            first ? "attack1" : "attack2",
            first ? FIRST_HIT_TICK : SECOND_HIT_TICK,
            SWING_TOTAL_TICKS,
            SWING_COOLDOWN
        );
    }

    protected boolean useFirstSwing() {
        return this.random.nextBoolean();
    }

    @Override
    protected void landStrike(ServerLevel level, LivingEntity target) {
        this.playSound(FangsNClawsSounds.OWLBEAR_SLASH, 1.0f, 0.9f + this.random.nextFloat() * 0.2f);
        super.landStrike(level, target);
        this.shove(target, SWING_KNOCKBACK, this.getX(), this.getZ());
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        boolean natural = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
        if (natural && this.random.nextInt(BABY_ODDS) == 0) {
            BabyMob.spawnBeside(level, this, this.babyType(), 0);
        }
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && this.isSleeping()) {
            this.setSleeping(false);
        }
        return hurt;
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
        if (this.howlCooldown > 0) {
            this.howlCooldown--;
        }
        if (this.howlTicks > 0) {
            this.howlTicks--;
        }
        if (this.diveCooldown > 0) {
            this.diveCooldown--;
        }
        this.tickHowl();
        this.tickFlight(level);
        this.tickFlapSound();
    }

    private void tickHowl() {
        LivingEntity target = this.getTarget();
        boolean ready = target != null && target.isAlive() && this.howlCooldown == 0 && !this.isBusy();
        if (ready && this.onGround()) {
            GeoTriggers.trigger(this, "howl");
            this.playSound(FangsNClawsSounds.OWLBEAR_HOWL, 1.2f, 0.9f + this.random.nextFloat() * 0.2f);
            this.howlTicks = HOWL_TICKS;
            this.howlCooldown = HOWL_COOLDOWN;
            this.getNavigation().stop();
        }
    }

    private void tickFlapSound() {
        if (!this.isFlapping()) {
            this.flapSoundTicks = 0;
            return;
        }
        if (++this.flapSoundTicks >= FLAP_SOUND_TICKS) {
            this.flapSoundTicks = 0;
            this.playSound(FangsNClawsSounds.OWLBEAR_FLAP, 0.9f, 0.9f + this.random.nextFloat() * 0.2f);
        }
    }

    private void tickFlight(ServerLevel level) {
        LivingEntity target = this.getTarget();
        boolean live = target != null && target.isAlive();
        if (!this.isFlying()) {
            this.trackPath(target, live);
            return;
        }
        this.setNoGravity(true);
        this.resetFallDistance();
        if (!live) {
            this.endFlight();
            return;
        }
        this.flyAgainst(level, target);
    }

    private void trackPath(LivingEntity target, boolean live) {
        boolean blocked = live
            && this.divesWhenBlocked()
            && this.onGround()
            && this.getNavigation().isDone()
            && this.distanceTo(target) > this.reach();
        this.noPathTicks = blocked ? this.noPathTicks + 1 : 0;
        if (this.noPathTicks >= NO_PATH_TICKS) {
            this.startFlight();
        }
    }

    protected void startFlight() {
        this.entityData.set(FLYING, true);
        this.noPathTicks = 0;
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.5, 0.0));
    }

    protected void endFlight() {
        this.entityData.set(FLYING, false);
        this.entityData.set(DIVING, false);
        this.setFlapping(false);
        this.setBlizzard(false);
        this.noPathTicks = 0;
        this.diveTicks = 0;
        this.setNoGravity(false);
    }

    // Moves a flying owlbear against its target for one tick.
    protected void flyAgainst(ServerLevel level, LivingEntity target) {
        if (this.isDiving()) {
            this.dive(level, target);
            return;
        }
        Vec3 above = new Vec3(target.getX(), target.getY() + HOVER_ABOVE, target.getZ());
        Steering.flyToward(this, above, FLY_SPEED, FLY_BLEND);
        Steering.faceToward(this, target.position());
        this.setFlapping(this.getY() < above.y - 1.5 || this.getDeltaMovement().y > 0.02);
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        boolean high = this.getY() - target.getY() >= DIVE_MIN_HEIGHT;
        if (this.diveCooldown == 0 && high && Math.sqrt(dx * dx + dz * dz) <= DIVE_MAX_FLAT) {
            this.entityData.set(DIVING, true);
            this.setFlapping(false);
            this.diveTicks = 0;
        }
    }

    private void dive(ServerLevel level, LivingEntity target) {
        this.diveTicks++;
        Steering.faceTowardWithPitch(this, target.position());
        Steering.flyToward(this, target.position(), DIVE_SPEED, 0.5);
        boolean landed = this.onGround() || this.horizontalCollision || this.distanceTo(target) < 2.0;
        if (landed || this.diveTicks > DIVE_MAX_TICKS) {
            this.land(level);
        }
    }

    private void land(ServerLevel level) {
        GeoTriggers.trigger(this, "fly_dive_attack_end");
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 1.2f);
        level.sendParticles(
            ParticleTypes.POOF,
            this.getX(),
            this.getY() + 0.2,
            this.getZ(),
            DIVE_SMOKE,
            1.5,
            0.3,
            1.5,
            0.05
        );
        for (LivingEntity victim : level.getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox().inflate(DIVE_RADIUS),
            entity -> entity != this && !(entity instanceof Owlbear)
        )) {
            victim.hurtServer(level, this.damageSources().mobAttack(this), DIVE_DAMAGE);
            this.shove(victim, SWING_KNOCKBACK, this.getX(), this.getZ());
        }
        this.diveCooldown = DIVE_COOLDOWN;
        this.setXRot(0.0f);
        this.endFlight();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WARDEN_STEP, 0.5f, 1.0f);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FangsNClawsSounds.OWLBEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FangsNClawsSounds.OWLBEAR_DEATH;
    }

    private class SleepGoal extends Goal {

        private int idleTicks;
        private int sleepTicks;

        SleepGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Owlbear owlbear = Owlbear.this;
            if (owlbear.getTarget() != null || owlbear.isFlying() || !owlbear.onGround() || owlbear.isInWater()) {
                this.idleTicks = 0;
                return false;
            }
            return ++this.idleTicks >= SLEEP_IDLE_TICKS && owlbear.random.nextInt(SLEEP_ODDS) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            Owlbear owlbear = Owlbear.this;
            Player near = owlbear.level().getNearestPlayer(owlbear, WAKE_RADIUS);
            boolean disturbed = near != null && !near.isSpectator() && !near.isCreative();
            return this.sleepTicks > 0 && owlbear.getTarget() == null && !disturbed && owlbear.isSleeping();
        }

        @Override
        public void start() {
            Owlbear.this.setSleeping(true);
            Owlbear.this.getNavigation().stop();
            this.sleepTicks = SLEEP_MIN_TICKS + Owlbear.this.random.nextInt(SLEEP_SPREAD);
        }

        @Override
        public void tick() {
            this.sleepTicks--;
            Owlbear.this.getNavigation().stop();
        }

        @Override
        public void stop() {
            Owlbear.this.setSleeping(false);
            this.idleTicks = 0;
        }
    }
}
