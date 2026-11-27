package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// A goblin that blows poisonous darts from range and runs from whoever hurts it.
public class DartGoblin extends Monster {

    private static final double SHOOT_RANGE = 14.0;
    private static final double CHASE_SPEED = 0.38;
    private static final double FLEE_SPEED = 1.5;
    private static final double FLEE_STOP_DISTANCE = 10.0;
    private static final int SHOOT_TICK = 13;
    private static final int ATTACK_TICKS = 20;
    private static final int COOLDOWN_TICKS = 25;
    private static final int FLEE_TICKS = 40;
    private static final float DART_SPEED = 1.6f;
    private static final float DART_SPREAD = 8.0f;
    private static final double ARC = 0.2;

    private int attackTick;
    private int cooldown;
    private int fleeTick;
    private Vec3 fleeFrom;
    private LivingEntity pendingTarget;

    public DartGoblin(EntityType<? extends DartGoblin> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 8.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.FOLLOW_RANGE, 20.0);
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
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt) {
            Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
            Vec3 behind = this.position().add(this.getDeltaMovement().scale(-5.0));
            this.fleeFrom = attacker != null ? attacker.position() : behind;
            this.fleeTick = FLEE_TICKS;
            this.attackTick = 0;
            this.pendingTarget = null;
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (this.attackTick > 0) {
            this.tickShot(level);
        } else if (this.fleeTick > 0) {
            this.fleeTick--;
            this.tickFlee();
        } else {
            this.tickCombat();
        }
    }

    private void tickShot(ServerLevel level) {
        this.attackTick++;
        if (this.attackTick == SHOOT_TICK && this.pendingTarget != null && this.pendingTarget.isAlive()) {
            this.shoot(level, this.pendingTarget);
            this.pendingTarget = null;
        }
        if (this.attackTick >= ATTACK_TICKS) {
            this.attackTick = 0;
            this.cooldown = COOLDOWN_TICKS;
        }
    }

    private void shoot(ServerLevel level, LivingEntity target) {
        PoisonousDart dart = new PoisonousDart(level, this);
        dart.setPos(this.getX(), this.getEyeY() - 0.1, this.getZ());
        double dx = target.getX() - this.getX();
        double dy = target.getY(1.0 / 3.0) - this.getEyeY();
        double dz = target.getZ() - this.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        dart.shoot(dx, dy + flat * ARC, dz, DART_SPEED, DART_SPREAD);
        level.addFreshEntity(dart);
    }

    private void tickFlee() {
        if (this.fleeFrom == null) {
            return;
        }
        if (this.position().distanceTo(this.fleeFrom) >= FLEE_STOP_DISTANCE) {
            this.fleeTick = 0;
            this.fleeFrom = null;
            this.getNavigation().stop();
            return;
        }
        Vec3 goal = this.position().add(this.position().subtract(this.fleeFrom).normalize().scale(8.0));
        this.getNavigation().moveTo(goal.x, goal.y, goal.z, FLEE_SPEED);
    }

    private void tickCombat() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (this.distanceTo(target) > SHOOT_RANGE || !this.hasLineOfSight(target)) {
            this.getNavigation().moveTo(target, CHASE_SPEED);
            return;
        }
        this.getNavigation().stop();
        Steering.faceToward(this, target.position());
        if (this.cooldown == 0 && this.attackTick == 0) {
            GeoTriggers.trigger(this, "attack");
            this.pendingTarget = target;
            this.attackTick = 1;
        }
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
