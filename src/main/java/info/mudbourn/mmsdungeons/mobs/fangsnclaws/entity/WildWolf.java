package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsArmor;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

// A pack wolf that bites, with a coat that varies from wolf to wolf.
public class WildWolf extends StrikeMob {

    public static final int VARIANT_COUNT = 5;

    private static final EntityDataAccessor<Integer> VARIANT =
        SynchedEntityData.defineId(WildWolf.class, EntityDataSerializers.INT);

    private static final double CHASE_SPEED = 1.2;
    private static final double STROLL_SPEED = 0.8;
    private static final float LEAP_HEIGHT = 0.4f;
    private static final float LOOK_RANGE = 8.0f;
    private static final int BITE_HIT_TICK = 7;
    private static final int BITE_TOTAL_TICKS = 15;
    private static final int BITE_COOLDOWN = 10;
    private static final int BABY_ODDS = 5;
    private static final int[] COAT_WEIGHTS = {30, 55, 75, 92};

    public WildWolf(EntityType<? extends WildWolf> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.FOLLOW_RANGE, 20.0)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 2.2);
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && !(target instanceof Player player && FangsNClawsArmor.hasFurHelmet(player));
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, Mth.clamp(variant, 0, VARIANT_COUNT - 1));
    }

    protected EntityType<? extends BabyMob> babyType() {
        return FangsNClawsMobs.BABY_WILD_WOLF;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, LEAP_HEIGHT));
        this.goalSelector.addGoal(2, new StrikeGoal(this, CHASE_SPEED));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, STROLL_SPEED));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, LOOK_RANGE));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.registerTargets();
    }

    protected void registerTargets() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Sheep.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Chicken.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Skeleton.class, false));
    }

    @Override
    public void startAttack(LivingEntity target) {
        this.beginStrike(target, "attack", BITE_HIT_TICK, BITE_TOTAL_TICKS, BITE_COOLDOWN);
        this.playSound(this.getAmbientSound(), 1.0f, this.getVoicePitch());
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        int roll = this.random.nextInt(100);
        int variant = 0;
        while (variant < COAT_WEIGHTS.length && roll >= COAT_WEIGHTS[variant]) {
            variant++;
        }
        this.setVariant(variant);
        boolean natural = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
        if (natural && this.random.nextInt(BABY_ODDS) == 0) {
            BabyMob.spawnBeside(level, this, this.babyType(), variant);
        }
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return FangsNClawsSounds.WOLF_GROWL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FangsNClawsSounds.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FangsNClawsSounds.WOLF_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WOLF_STEP.value(), 0.15f, 1.0f);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVariant(input.getIntOr("Variant", 0));
    }
}
