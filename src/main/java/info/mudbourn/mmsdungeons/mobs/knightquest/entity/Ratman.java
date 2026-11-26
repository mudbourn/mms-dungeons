package info.mudbourn.mmsdungeons.mobs.knightquest.entity;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A skeletal archer in one of four hoods that, below half health, either fires a ring of arrows or vanishes in smoke.
public class Ratman extends AbstractSkeleton {

    private static final EntityDataAccessor<Boolean> ARROW_RING = SynchedEntityData.defineId(Ratman.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Ratman.class, EntityDataSerializers.INT);

    private static final int VARIANTS = 4;
    private static final int RING_TICKS = 10;
    private static final int RING_STEP = 36;
    private static final int RING_START = 50;
    private static final double SMOKE_RADIUS = 2.5;
    private static final int STATUS_TICKS = 100;

    private boolean summoned;
    private int ringTicks;
    private int ringAngle = RING_START;

    public Ratman(EntityType<? extends Ratman> type, Level level) {
        super(type, level);
        if (!level.isClientSide()) {
            this.entityData.set(ARROW_RING, this.random.nextBoolean());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 22.0)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.ATTACK_SPEED, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ARROW_RING, false);
        builder.define(VARIANT, 1);
    }

    // Which hood the Ratman wears, from 1 to 4.
    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    @Override
    public void reassessWeaponGoal() {
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(2, new RangedBowAttackGoal<>(this, 0.7, 10, 15.0f));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        this.entityData.set(VARIANT, this.random.nextInt(VARIANTS) + 1);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level) || this.summoned || this.getHealth() >= this.getMaxHealth() * 0.5) {
            return;
        }
        if (!this.entityData.get(ARROW_RING)) {
            this.smokeBomb(level);
            this.summoned = true;
            return;
        }
        if (this.ringTicks < RING_TICKS && this.ringTicks % 2 == 0) {
            for (int i = 0; i < 2; i++) {
                this.ringAngle += RING_STEP;
                this.shootRingArrow(level, this.ringAngle);
            }
        }
        this.ringTicks++;
        if (this.ringTicks >= RING_TICKS) {
            this.summoned = true;
        }
    }

    private void shootRingArrow(ServerLevel level, int degrees) {
        double x = Math.cos(Math.toRadians(degrees));
        double z = Math.sin(Math.toRadians(degrees));
        Arrow arrow = new Arrow(level, this, new ItemStack(Items.ARROW), new ItemStack(Items.CROSSBOW));
        arrow.setPos(this.getX() + x, this.getEyeY() - 1.5, this.getZ() + z);
        arrow.setDeltaMovement(new Vec3(x, 0.3, z));
        switch (this.getVariant()) {
            case 1 -> arrow.addEffect(new MobEffectInstance(MobEffects.POISON, STATUS_TICKS, 0));
            case 2 -> arrow.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, STATUS_TICKS, 0));
            case 3 -> arrow.addEffect(new MobEffectInstance(MobEffects.WITHER, STATUS_TICKS, 0));
            default -> arrow.igniteForSeconds(10.0f);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.DISPENSER_LAUNCH, SoundSource.HOSTILE, 0.75f, 1.0f);
        level.addFreshEntity(arrow);
    }

    private void smokeBomb(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.HOSTILE, 1.0f, 1.0f);
        level.sendParticles(
            ParticleTypes.CAMPFIRE_COSY_SMOKE,
            this.getX(),
            this.getY() + 1.0,
            this.getZ(),
            160,
            SMOKE_RADIUS * 0.5,
            SMOKE_RADIUS * 0.5,
            SMOKE_RADIUS * 0.5,
            0.0
        );
        this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, STATUS_TICKS, 1));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("ArrowRing", this.entityData.get(ARROW_RING));
        output.putInt("Variant", this.getVariant());
        output.putBoolean("Summoned", this.summoned);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(ARROW_RING, input.getBooleanOr("ArrowRing", false));
        this.entityData.set(VARIANT, input.getIntOr("Variant", 1));
        this.summoned = input.getBooleanOr("Summoned", false);
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.WOLF_STEP.value();
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.AXOLOTL_SWIM;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WOLF_STEP.value(), 0.15f, 1.0f);
    }
}
