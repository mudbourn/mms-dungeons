package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A pale spirit that drifts through the cave and passes through walls once it hunts.
public class Ghost extends Monster {

    private static final EntityDataAccessor<Boolean> ANGRY =
        SynchedEntityData.defineId(Ghost.class, EntityDataSerializers.BOOLEAN);

    private static final double HOVER_HEIGHT = 1.2;
    private static final double HOVER_BOB = 0.4;
    private static final double CHASE_SPEED = 0.2;
    private static final double ROAM_RANGE = 10.0;
    private static final int GROUND_SCAN = 12;
    private static final int ROAM_TICKS = 80;

    private Vec3 flyTarget;
    private int roamCooldown;

    public Ghost(EntityType<? extends Ghost> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 16.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.FOLLOW_RANGE, 20.0)
            .add(Attributes.FLYING_SPEED, 0.14)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 1.8);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANGRY, false);
    }

    public boolean isAngry() {
        return this.entityData.get(ANGRY);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        if (this.level() instanceof ServerLevel level) {
            LivingEntity target = this.getTarget();
            boolean hunting = target != null && target.isAlive();
            this.entityData.set(ANGRY, hunting);
            this.noPhysics = hunting || !level.noCollision(this, this.getBoundingBox().deflate(0.05));
            if (hunting) {
                this.hunt(level, target);
            } else {
                this.roam(level);
            }
        }
    }

    private void hunt(ServerLevel level, LivingEntity target) {
        this.flyTarget = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
        Steering.faceTowardWithPitch(this, this.flyTarget);
        if (this.distanceTo(target) <= this.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE)) {
            this.doHurtTarget(level, target);
        } else if (this.hurtTime == 0) {
            Steering.flyToward(this, this.flyTarget, CHASE_SPEED, 0.18);
        }
    }

    private void roam(ServerLevel level) {
        double hover = this.groundY(level) + HOVER_HEIGHT + Mth.sin(this.tickCount * 0.08f) * HOVER_BOB;
        this.roamCooldown--;
        if (this.flyTarget == null || this.roamCooldown <= 0 || this.position().distanceTo(this.flyTarget) < 1.5) {
            this.flyTarget = this.pickRoamTarget(level, hover);
            this.roamCooldown = ROAM_TICKS;
        }
        if (this.flyTarget != null) {
            Steering.faceTowardWithPitch(this, this.flyTarget);
            Steering.flyToward(this, this.flyTarget, this.getAttributeValue(Attributes.FLYING_SPEED), 0.1);
        }
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, (hover - this.getY()) * 0.06, 0.0));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.12, 0.0));
            this.flyTarget = null;
        }
    }

    private double groundY(ServerLevel level) {
        return Steering.groundY(level, this, GROUND_SCAN, this.getY() - HOVER_HEIGHT);
    }

    private @Nullable Vec3 pickRoamTarget(ServerLevel level, double hover) {
        double x = this.getX() + (this.random.nextDouble() * 2.0 - 1.0) * ROAM_RANGE;
        double z = this.getZ() + (this.random.nextDouble() * 2.0 - 1.0) * ROAM_RANGE;
        double y = Mth.clamp(hover + (this.random.nextDouble() * 2.0 - 1.0), level.getMinY() + 2, level.getMaxY() - 2);
        return level.getBlockState(BlockPos.containing(x, y, z)).isSolid() ? null : new Vec3(x, y, z);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() && this.tickCount % 15 == 0) {
            this.level().addParticle(
                ParticleTypes.SOUL,
                this.getRandomX(1.0),
                this.getRandomY(),
                this.getRandomZ(1.0),
                0.0,
                0.02,
                0.0
            );
        }
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3f;
    }
}
