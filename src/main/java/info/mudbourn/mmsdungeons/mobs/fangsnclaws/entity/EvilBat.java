package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// A bat that sleeps on cave ceilings, wakes when someone comes near, and strikes in short rushes.
public class EvilBat extends Monster {

    private static final EntityDataAccessor<Boolean> RESTING =
        SynchedEntityData.defineId(EvilBat.class, EntityDataSerializers.BOOLEAN);

    private static final double WAKE_RANGE = 6.0;
    private static final double CHASE_SPEED = 0.5;
    private static final double RUSH_SPEED = 0.95;
    private static final double RETREAT_SPEED = 0.6;
    private static final double RETREAT_DISTANCE = 5.0;
    private static final double ROAM_RANGE = 8.0;
    private static final int ROAM_TICKS = 80;
    private static final int STRIKE_TICK = 6;
    private static final int RETREAT_TICKS = 14;
    private static final int COOLDOWN_TICKS = 30;
    private static final int ROOST_SCAN = 7;
    private static final int ROOST_ODDS = 30;

    private Vec3 flyTarget;
    private int roamCooldown;
    private int rushTick;
    private int retreatTick;
    private Vec3 retreatDirection;
    private int attackCooldown;

    public EvilBat(EntityType<? extends EvilBat> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.FOLLOW_RANGE, 28.0)
            .add(Attributes.FLYING_SPEED, 0.28)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 1.5);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RESTING, true);
    }

    public boolean isResting() {
        return this.entityData.get(RESTING);
    }

    private void setResting(boolean resting) {
        this.entityData.set(RESTING, resting);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return source.is(DamageTypes.FLY_INTO_WALL) || super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    private boolean isStriking() {
        return this.rushTick > 0 || this.retreatTick > 0;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && !this.isResting()) {
            Entity attacker = source.getDirectEntity();
            this.retreatDirection = this.awayFrom(attacker != null ? attacker.position() : null);
            this.rushTick = 0;
            this.retreatTick = 1;
            this.attackCooldown = COOLDOWN_TICKS;
        }
        return hurt;
    }

    private Vec3 awayFrom(Vec3 point) {
        Vec3 away = point == null ? Vec3.ZERO : this.position().subtract(point);
        return away.length() > 0.01 ? away.normalize() : new Vec3(0.0, 1.0, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.isResting()) {
            this.tickResting();
        } else if (this.level() instanceof ServerLevel level) {
            this.noPhysics = false;
            this.tickFlying(level);
        }
    }

    private void wake() {
        this.setResting(false);
        this.noPhysics = false;
        this.flyTarget = null;
    }

    private void tickResting() {
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        if (this.level().isClientSide()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.wake();
            return;
        }
        Player nearby = this.level().getNearestPlayer(this, WAKE_RANGE);
        if (nearby != null && !nearby.isSpectator() && !nearby.isCreative()) {
            this.wake();
            this.setTarget(nearby);
            this.roamCooldown = 0;
        } else if (!this.level().getBlockState(this.blockPosition().above()).isSolid()) {
            this.wake();
        }
    }

    private void tickFlying(ServerLevel level) {
        LivingEntity target = this.getTarget();
        boolean hunting = target != null && target.isAlive();
        if (this.isStriking()) {
            if (hunting) {
                this.tickStrike(level, target);
            } else {
                this.rushTick = 0;
                this.retreatTick = 0;
                this.retreatDirection = null;
            }
        } else if (hunting) {
            this.flyTarget = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
            Steering.faceTowardWithPitch(this, this.flyTarget);
            boolean inReach = this.distanceTo(target) <= this.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
            if (inReach && this.attackCooldown == 0) {
                this.rushTick = 1;
                this.retreatTick = 0;
                this.retreatDirection = null;
            } else if (this.hurtTime == 0) {
                Steering.flyToward(this, this.flyTarget, CHASE_SPEED, 0.15);
            }
        } else {
            this.roam(level);
        }
    }

    private void tickStrike(ServerLevel level, LivingEntity target) {
        if (this.rushTick > 0) {
            this.rushTick++;
            Vec3 aim = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
            Steering.faceTowardWithPitch(this, aim);
            Steering.flyToward(this, aim, RUSH_SPEED, 0.35);
            if (this.rushTick == STRIKE_TICK) {
                if (this.distanceTo(target) <= this.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) + 1.0) {
                    super.doHurtTarget(level, target);
                }
                this.retreatDirection = this.awayFrom(target.position());
            }
            if (this.rushTick > STRIKE_TICK) {
                this.rushTick = 0;
                this.retreatTick = 1;
                this.attackCooldown = COOLDOWN_TICKS;
            }
        } else if (this.retreatTick > 0) {
            this.retreatTick++;
            if (this.retreatDirection != null) {
                Vec3 away = this.position().add(this.retreatDirection.scale(RETREAT_DISTANCE));
                Steering.faceTowardWithPitch(this, away);
                Steering.flyToward(this, away, RETREAT_SPEED, 0.3);
            }
            if (this.retreatTick >= RETREAT_TICKS) {
                this.retreatTick = 0;
                this.retreatDirection = null;
            }
        }
    }

    private void roam(ServerLevel level) {
        this.roamCooldown--;
        if (this.flyTarget == null || this.roamCooldown <= 0 || this.position().distanceTo(this.flyTarget) < 1.8) {
            if (this.tryRoost(level)) {
                return;
            }
            this.flyTarget = Steering.roamPoint(level, this, ROAM_RANGE);
            this.roamCooldown = ROAM_TICKS;
        }
        if (this.flyTarget != null) {
            Steering.faceTowardWithPitch(this, this.flyTarget);
        }
        Steering.flyToward(this, this.flyTarget, this.getAttributeValue(Attributes.FLYING_SPEED), 0.12);
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.1, 0.0));
            this.flyTarget = null;
        }
    }

    private boolean tryRoost(ServerLevel level) {
        if (this.random.nextInt(ROOST_ODDS) != 0) {
            return false;
        }
        BlockPos base = this.blockPosition();
        for (int rise = 0; rise <= ROOST_SCAN; rise++) {
            BlockPos gap = base.above(rise);
            BlockPos ceiling = gap.above();
            if (level.getBlockState(gap).isAir() && level.getBlockState(ceiling).isSolid()) {
                double x = Math.floor(this.getX()) + 0.5;
                double z = Math.floor(this.getZ()) + 0.5;
                this.setPos(x, ceiling.getY() - this.getBbHeight(), z);
                this.setDeltaMovement(Vec3.ZERO);
                this.setResting(true);
                this.flyTarget = null;
                return true;
            }
        }
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.1f;
    }
}
