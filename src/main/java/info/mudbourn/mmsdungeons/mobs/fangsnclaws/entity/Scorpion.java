package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
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
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

// A wall-climbing scorpion that claws, and stings with its tail now and then.
public class Scorpion extends StrikeMob {

    private static final EntityDataAccessor<Boolean> CLIMBING =
        SynchedEntityData.defineId(Scorpion.class, EntityDataSerializers.BOOLEAN);

    private static final double CHASE_SPEED = 1.0;
    private static final double STROLL_SPEED = 0.8;
    private static final float LOOK_RANGE = 8.0f;
    private static final int CLAW_HIT_TICK = 9;
    private static final int STINGER_HIT_TICK = 10;
    private static final int STRIKE_TOTAL_TICKS = 15;
    private static final int STRIKE_COOLDOWN = 10;
    private static final int STINGER_COOLDOWN = 200;
    private static final int NAUSEA_TICKS = 200;
    private static final int JOCKEY_ODDS = 100;
    private static final int BABY_ODDS = 5;

    private int stingerCooldown;
    private boolean stinging;

    public Scorpion(EntityType<? extends Scorpion> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 18.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 2.4);
    }

    // The mount-rider kind this scorpion sometimes spawns under.
    protected EntityType<? extends Mob> jockeyType() {
        return EntityTypes.SKELETON;
    }

    // The variant its young carry, which picks their coat and adult kind.
    protected int babyVariant() {
        return 0;
    }

    // Adds what a hit leaves behind on its target.
    protected void applyHitEffect(LivingEntity target, boolean stinger) {
        if (stinger) {
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_TICKS, 0));
        }
    }

    public boolean isClimbing() {
        return this.entityData.get(CLIMBING);
    }

    @Override
    public boolean onClimbable() {
        return this.isClimbing();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLIMBING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new StrikeGoal(this, CHASE_SPEED));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, STROLL_SPEED));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, LOOK_RANGE));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void startAttack(LivingEntity target) {
        this.stinging = this.stingerCooldown == 0 && this.random.nextBoolean();
        if (this.stinging) {
            this.stingerCooldown = STINGER_COOLDOWN;
            this.playSound(FangsNClawsSounds.SCORPION_STINGER, 1.0f, 1.0f);
            this.beginStrike(target, "attack_stinger", STINGER_HIT_TICK, STRIKE_TOTAL_TICKS, STRIKE_COOLDOWN);
            return;
        }
        boolean first = this.random.nextBoolean();
        this.playSound(first ? FangsNClawsSounds.SCORPION_CLAW1 : FangsNClawsSounds.SCORPION_CLAW2, 1.0f, 1.0f);
        this.beginStrike(target, first ? "attack1" : "attack2", CLAW_HIT_TICK, STRIKE_TOTAL_TICKS, STRIKE_COOLDOWN);
    }

    @Override
    protected void landStrike(ServerLevel level, LivingEntity target) {
        super.landStrike(level, target);
        this.applyHitEffect(target, this.stinging);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.entityData.set(CLIMBING, this.horizontalCollision);
            if (this.stingerCooldown > 0) {
                this.stingerCooldown--;
            }
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        boolean natural = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
        if (natural && this.random.nextInt(BABY_ODDS) == 0) {
            BabyMob.spawnBeside(level, this, FangsNClawsMobs.BABY_SCORPION, this.babyVariant());
        }
        if (natural && this.random.nextInt(JOCKEY_ODDS) == 0) {
            Mob rider = this.jockeyType().create(level.getLevel(), EntitySpawnReason.JOCKEY);
            if (rider != null) {
                rider.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0f);
                rider.finalizeSpawn(level, difficulty, EntitySpawnReason.JOCKEY, null);
                rider.startRiding(this, false, false);
            }
        }
        return data;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SPIDER_STEP, 0.15f, 1.0f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FangsNClawsSounds.SCORPION_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FangsNClawsSounds.SCORPION_DEATH;
    }
}
