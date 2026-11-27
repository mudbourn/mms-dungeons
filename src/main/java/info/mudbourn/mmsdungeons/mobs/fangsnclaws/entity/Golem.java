package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// A stone giant that sleeps until a player comes close, slams and flings its prey, and falls open at half health.
public class Golem extends StrikeMob {

    private static final EntityDataAccessor<Integer> STATE =
        SynchedEntityData.defineId(Golem.class, EntityDataSerializers.INT);

    public static final int DORMANT = 0;
    public static final int AWAKE = 1;
    public static final int DOWN = 2;

    private static final double CHASE_SPEED = 1.0;
    private static final double STROLL_SPEED = 0.6;
    private static final float LOOK_RANGE = 8.0f;
    private static final double WAKE_RADIUS = 7.0;
    private static final int WAKE_CHECK_TICKS = 10;
    private static final int WAKE_UP_TICKS = 70;
    private static final int FALL_TICKS = 20;
    private static final int DOWN_TICKS = 100;
    private static final int GET_UP_TICKS = 30;
    private static final float FALL_HEALTH = 0.5f;
    private static final float DOWN_DAMAGE_FACTOR = 2.0f;
    private static final float SHOVEL_DAMAGE_FACTOR = 2.0f;
    private static final int SLAM_HIT_TICK = 15;
    private static final int SLAM_TOTAL_TICKS = 30;
    private static final int SLAM_COOLDOWN = 20;
    private static final double SLAM_KNOCKBACK = 1.2;
    private static final int HEAVY_HIT_TICK = 20;
    private static final int HEAVY_TOTAL_TICKS = 30;
    private static final int HEAVY_COOLDOWN = 40;
    private static final int HEAVY_ODDS = 3;
    private static final float HEAVY_DAMAGE_FACTOR = 0.7f;
    private static final double FLING_AWAY = 1.6;
    private static final double FLING_UP = 1.1;

    private int wakeTicks;
    private int downTicks;
    private int getUpTicks;
    private boolean fallen;
    private boolean heavy;

    public Golem(EntityType<? extends Golem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 150.0)
            .add(Attributes.ARMOR, 12.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 4.5)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    public boolean isDormant() {
        return this.getState() == DORMANT;
    }

    public boolean isDown() {
        return this.getState() == DOWN;
    }

    private void setState(int state) {
        this.entityData.set(STATE, state);
    }

    // The animation of the blow that flings its target.
    protected String heavyAnimation() {
        return "attack_throw";
    }

    protected void landSlam(ServerLevel level, LivingEntity target) {
        super.landStrike(level, target);
        this.shove(target, SLAM_KNOCKBACK, this.getX(), this.getZ());
    }

    protected void landHeavy(ServerLevel level, LivingEntity target) {
        float damage = (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * HEAVY_DAMAGE_FACTOR);
        target.hurtServer(level, this.damageSources().mobAttack(this), damage);
        this.shove(target, FLING_AWAY, this.getX(), this.getZ());
        target.push(0.0, FLING_UP, 0.0);
        target.needsSync = true;
    }

    @Override
    public boolean isBusy() {
        return super.isBusy() || this.wakeTicks > 0 || this.getUpTicks > 0 || this.getState() != AWAKE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, AWAKE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new StrikeGoal(this, CHASE_SPEED));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, STROLL_SPEED));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, LOOK_RANGE));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void startAttack(LivingEntity target) {
        this.heavy = this.random.nextInt(HEAVY_ODDS) == 0;
        if (this.heavy) {
            this.beginStrike(target, this.heavyAnimation(), HEAVY_HIT_TICK, HEAVY_TOTAL_TICKS, HEAVY_COOLDOWN);
        } else {
            this.beginStrike(target, "attack", SLAM_HIT_TICK, SLAM_TOTAL_TICKS, SLAM_COOLDOWN);
        }
    }

    @Override
    protected void landStrike(ServerLevel level, LivingEntity target) {
        if (this.heavy) {
            this.landHeavy(level, target);
        } else {
            this.landSlam(level, target);
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) {
            this.setState(DORMANT);
        }
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float scaled = amount;
        if (this.isDown()) {
            scaled *= DOWN_DAMAGE_FACTOR;
        }
        if (source.getDirectEntity() instanceof LivingEntity attacker
            && attacker.getMainHandItem().is(ItemTags.SHOVELS)) {
            scaled *= SHOVEL_DAMAGE_FACTOR;
        }
        boolean hurt = super.hurtServer(level, source, scaled);
        if (!hurt) {
            return false;
        }
        if (this.isDormant()) {
            this.wake();
        } else if (this.shouldFall()) {
            this.fall();
        }
        return true;
    }

    private boolean shouldFall() {
        return this.isAlive()
            && this.getState() == AWAKE
            && !this.fallen
            && this.getHealth() <= this.getMaxHealth() * FALL_HEALTH;
    }

    private void wake() {
        this.setState(AWAKE);
        this.wakeTicks = WAKE_UP_TICKS;
        GeoTriggers.trigger(this, "wake_up");
    }

    private void fall() {
        this.fallen = true;
        this.setState(DOWN);
        this.downTicks = FALL_TICKS + DOWN_TICKS;
        this.getNavigation().stop();
        GeoTriggers.trigger(this, "fall");
    }

    private void getUp() {
        this.setState(AWAKE);
        this.getUpTicks = GET_UP_TICKS;
        GeoTriggers.trigger(this, "get_up");
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.wakeTicks > 0) {
            this.wakeTicks--;
        }
        if (this.getUpTicks > 0) {
            this.getUpTicks--;
        }
        if (this.isDown() && --this.downTicks <= 0) {
            this.getUp();
        }
        if (this.isDormant() && this.tickCount % WAKE_CHECK_TICKS == 0) {
            Player near = level.getNearestPlayer(this, WAKE_RADIUS);
            if (near != null && !near.isSpectator() && !near.isCrouching()) {
                this.wake();
            }
        }
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isBusy();
    }

    @Override
    public boolean isPushable() {
        return !this.isDormant() && super.isPushable();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0f, 0.8f);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isDormant() ? null : FangsNClawsSounds.GOLEM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FangsNClawsSounds.GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FangsNClawsSounds.GOLEM_DEATH;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("State", this.getState());
        output.putBoolean("Fallen", this.fallen);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setState(input.getIntOr("State", AWAKE) == DORMANT ? DORMANT : AWAKE);
        this.fallen = input.getBooleanOr("Fallen", false);
    }
}
