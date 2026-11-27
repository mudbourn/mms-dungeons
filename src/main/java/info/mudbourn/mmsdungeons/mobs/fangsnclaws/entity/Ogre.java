package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A towering brute that charges, swings, and slams the ground to cut off a retreat.
public class Ogre extends Monster {

    private static final EntityDataAccessor<Boolean> RUNNING =
        SynchedEntityData.defineId(Ogre.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SIAMESE =
        SynchedEntityData.defineId(Ogre.class, EntityDataSerializers.BOOLEAN);

    private static final int HIT_TICK = 15;
    private static final int ATTACK_TICKS = 20;
    private static final int SLAM_HIT_TICK = 20;
    private static final int SLAM_TICKS = 32;
    private static final double SLAM_RADIUS = 2.5;
    private static final float SIAMESE_CHANCE = 0.3f;
    private static final double ATTACK_REACH = 5.0;
    private static final double DUST_SPEED = 0.05;

    private Entity pendingTarget;
    private int attackTick;
    private int slamTick;
    private Vec3 slamPos;
    private double lastX;
    private double lastZ;

    public Ogre(EntityType<? extends Ogre> type, Level level) {
        super(type, level);
        this.moveControl = new OgreMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 60.0)
            .add(Attributes.ARMOR, 5.0)
            .add(Attributes.MOVEMENT_SPEED, 0.15)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.FOLLOW_RANGE, 20.0)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 3.5)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RUNNING, false);
        builder.define(SIAMESE, false);
    }

    public boolean isRunning() {
        return this.entityData.get(RUNNING);
    }

    public void setRunning(boolean running) {
        this.entityData.set(RUNNING, running);
    }

    public boolean isSiamese() {
        return this.entityData.get(SIAMESE);
    }

    protected void setSiamese(boolean siamese) {
        this.entityData.set(SIAMESE, siamese);
    }

    // Whether a swing, slam or special attack is under way.
    public boolean isBusy() {
        return this.attackTick > 0 || this.slamTick > 0;
    }

    // Whether the ogre must stand still for its current attack.
    public boolean isRooted() {
        return false;
    }

    // Starts a special attack on a target in view, returning whether one began.
    public boolean useSpecialAttack(LivingEntity target) {
        return false;
    }

    protected double slamDamage() {
        return 8.0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OgreAttackGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        this.setSiamese(this.random.nextFloat() < SIAMESE_CHANCE);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    // Starts a swing whose blow lands partway through it.
    public void startAttack(Entity target) {
        GeoTriggers.trigger(this, "attack");
        this.pendingTarget = target;
        this.attackTick = 1;
    }

    // Starts a slam that strikes the ground at a point.
    public void startSlam(Vec3 impact) {
        GeoTriggers.trigger(this, "slam_attack");
        this.slamPos = impact;
        this.slamTick = 1;
    }

    @Override
    public void tick() {
        this.lastX = this.getX();
        this.lastZ = this.getZ();
        super.tick();
        if (this.level() instanceof ServerLevel level) {
            this.tickSwing(level);
            this.tickSlam(level);
        } else if (this.onGround() && this.isRunning()) {
            this.kickUpDust();
        }
    }

    private void tickSwing(ServerLevel level) {
        if (this.attackTick <= 0) {
            return;
        }
        this.attackTick++;
        if (this.attackTick == HIT_TICK) {
            if (this.pendingTarget != null
                && this.pendingTarget.isAlive()
                && this.distanceTo(this.pendingTarget) <= ATTACK_REACH
                && this.hasLineOfSight(this.pendingTarget)) {
                super.doHurtTarget(level, this.pendingTarget);
            }
            this.pendingTarget = null;
        }
        if (this.attackTick >= ATTACK_TICKS) {
            this.attackTick = 0;
        }
    }

    private void tickSlam(ServerLevel level) {
        if (this.slamTick <= 0) {
            return;
        }
        this.slamTick++;
        if (this.slamTick == SLAM_HIT_TICK) {
            this.slamImpact(level);
        }
        if (this.slamTick >= SLAM_TICKS) {
            this.slamTick = 0;
            this.slamPos = null;
        }
    }

    private void slamImpact(ServerLevel level) {
        if (this.slamPos == null) {
            return;
        }
        if (level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            this.shatterGround(level);
        }
        AABB area = new AABB(this.slamPos, this.slamPos).inflate(SLAM_RADIUS);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (victim != this) {
                victim.hurtServer(level, this.damageSources().mobAttack(this), (float) this.slamDamage());
            }
        }
        level.playSound(
            null,
            this.slamPos.x,
            this.slamPos.y,
            this.slamPos.z,
            SoundEvents.GENERIC_EXPLODE.value(),
            SoundSource.HOSTILE,
            1.0f,
            0.55f + this.random.nextFloat() * 0.2f
        );
        for (int i = 0; i < 35; i++) {
            level.sendParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                this.slamPos.x,
                this.slamPos.y + 0.3,
                this.slamPos.z,
                0,
                (this.random.nextDouble() - 0.5) * 2.0,
                this.random.nextDouble() * 0.8 + 0.2,
                (this.random.nextDouble() - 0.5) * 2.0,
                0.15
            );
        }
    }

    private void shatterGround(ServerLevel level) {
        int reach = (int) Math.ceil(SLAM_RADIUS);
        BlockPos center = BlockPos.containing(this.slamPos);
        BlockPos low = center.offset(-reach, -1, -reach);
        BlockPos high = center.offset(reach, reach, reach);
        for (BlockPos pos : BlockPos.betweenClosed(low, high)) {
            double radius = SLAM_RADIUS * (0.7 + this.random.nextFloat() * 0.6);
            BlockState state = level.getBlockState(pos);
            boolean breakable = !state.isAir() && state.getDestroySpeed(level, pos) >= 0.0f && !state.hasBlockEntity();
            if (breakable && Math.sqrt(center.distSqr(pos)) <= radius) {
                level.destroyBlock(pos.immutable(), true, this);
            }
        }
    }

    private void kickUpDust() {
        double dx = this.getX() - this.lastX;
        double dz = this.getZ() - this.lastZ;
        if (Math.sqrt(dx * dx + dz * dz) <= DUST_SPEED) {
            return;
        }
        BlockPos underfoot = BlockPos.containing(this.getX(), this.getY() - 0.2, this.getZ());
        BlockState ground = this.level().getBlockState(underfoot);
        if (ground.getRenderShape() != RenderShape.INVISIBLE) {
            this.level().addParticle(
                new BlockParticleOption(ParticleTypes.BLOCK, ground),
                this.getX() + (this.random.nextFloat() - 0.5f) * this.getBbWidth(),
                this.getBoundingBox().minY + 0.1,
                this.getZ() + (this.random.nextFloat() - 0.5f) * this.getBbWidth(),
                -this.getDeltaMovement().x * 4.0,
                1.5,
                -this.getDeltaMovement().z * 4.0
            );
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Siamese", this.isSiamese());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setSiamese(input.getBooleanOr("Siamese", false));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return FangsNClawsSounds.OGRE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FangsNClawsSounds.OGRE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FangsNClawsSounds.OGRE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WARDEN_STEP, 2.0f, 0.9f + this.random.nextFloat() * 0.2f);
    }
}
