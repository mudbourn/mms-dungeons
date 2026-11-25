package info.mudbourn.mmsdungeons.mobs.orpiment.entity;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentBlocks;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentItems;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentMobs;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentParticles;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A large flightless bird that can be tamed with bird feed, saddled and ridden in dashes that drain its stamina.
public class Craw extends AbstractHorse {

    public static final int MAX_STAMINA = 3300;
    public static final int VARIANT_COUNT = 7;

    private static final int[] VARIANT_WEIGHTS = {30, 15, 15, 15, 10, 10, 5};
    private static final int TIRED_LOCKOUT_TICKS = 300;
    private static final int DASH_STAMINA_COST = 300;
    private static final int DASH_COOLDOWN_TICKS = 20;
    private static final int DASH_BOOST_TICKS = 60;
    private static final double DASH_BOOST = 0.6;
    private static final byte TIRED_EVENT = 90;
    private static final Identifier DASH_MODIFIER = MmsDungeons.id("craw_dash");

    private static final EntityDataAccessor<Integer> STAMINA = SynchedEntityData.defineId(Craw.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DASH_COOLDOWN = SynchedEntityData.defineId(Craw.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Craw.class, EntityDataSerializers.INT);

    public float flap;
    public float flapSpeed;
    public float oFlap;
    public float oFlapSpeed;
    public float flapping = 1.0f;

    private boolean staminaDepletedNotified;
    private int tiredLockout;
    private int dashBoostTicks;

    public Craw(EntityType<? extends Craw> type, Level level) {
        super(type, level);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractHorse.createBaseHorseAttributes()
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.ARMOR, 0.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    private static int rollVariant(RandomSource random) {
        int roll = random.nextInt(100);
        int cumulative = 0;
        for (int i = 0; i < VARIANT_WEIGHTS.length; i++) {
            cumulative += VARIANT_WEIGHTS[i];
            if (roll < cumulative) {
                return i;
            }
        }
        return VARIANT_WEIGHTS.length - 1;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STAMINA, MAX_STAMINA);
        builder.define(DASH_COOLDOWN, 0);
        builder.define(VARIANT, 0);
    }

    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, this::isFood, false));
        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.5f));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return OrpimentSounds.CRAW_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return OrpimentSounds.CRAW_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(OrpimentSounds.CRAW_STEP, 0.15f, 1.0f);
    }

    @Override
    public boolean canEatGrass() {
        return false;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id != TIRED_EVENT) {
            super.handleEntityEvent(id);
            return;
        }
        for (int i = 0; i < 25; i++) {
            this.level().addParticle(
                ParticleTypes.SMOKE,
                this.getRandomX(1.4),
                this.getRandomY() + 0.6,
                this.getRandomZ(1.4),
                this.random.nextGaussian() * 0.12,
                this.random.nextGaussian() * 0.08 + 0.02,
                this.random.nextGaussian() * 0.12
            );
        }
    }

    public int getStamina() {
        return this.entityData.get(STAMINA);
    }

    public void setStamina(int value) {
        this.entityData.set(STAMINA, Mth.clamp(value, 0, MAX_STAMINA));
    }

    private void addStamina(int amount) {
        this.setStamina(this.getStamina() + amount);
        if (this.getStamina() > 0) {
            this.staminaDepletedNotified = false;
        }
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, Mth.clamp(variant, 0, VARIANT_COUNT - 1));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Stamina", this.getStamina());
        output.putInt("Variant", this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setStamina(input.getIntOr("Stamina", MAX_STAMINA));
        this.setVariant(input.getIntOr("Variant", 0));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.isVehicle() && this.isUnderWater()) {
            this.ejectPassengers();
        }
        if (this.isVehicle()) {
            this.tickRiddenStamina();
        } else if (this.tiredLockout > 0) {
            this.tiredLockout--;
        } else if (this.getStamina() < MAX_STAMINA) {
            this.addStamina(1);
        }
        if (this.dashBoostTicks > 0 && --this.dashBoostTicks == 0) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(DASH_MODIFIER);
        }
        int cooldown = this.entityData.get(DASH_COOLDOWN);
        if (cooldown > 0) {
            this.entityData.set(DASH_COOLDOWN, cooldown - 1);
        }
    }

    private void tickRiddenStamina() {
        if (this.getStamina() > 0) {
            this.setStamina(this.getStamina() - 1);
        }
        if (this.getStamina() > 0) {
            return;
        }
        if (!this.staminaDepletedNotified) {
            this.staminaDepletedNotified = true;
            this.tiredLockout = TIRED_LOCKOUT_TICKS;
            this.playSound(OrpimentSounds.CRAW_TIRED, 1.0f, 1.0f);
            this.level().broadcastEntityEvent(this, TIRED_EVENT);
        }
        this.ejectPassengers();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.oFlap = this.flap;
        this.oFlapSpeed = this.flapSpeed;
        this.flapSpeed = Mth.clamp(this.flapSpeed + (this.onGround() ? -1.0f : 4.0f) * 0.3f, 0.0f, 1.0f);
        if (!this.onGround() && this.flapping < 1.0f) {
            this.flapping = 1.0f;
        }
        this.flapping *= 0.9f;
        if (!this.onGround() && !this.isInWater() && !this.isInLava() && this.getDeltaMovement().y < 0.0) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6, 1.0));
        }
        this.flap += this.flapping * 2.0f;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canJump() {
        return super.canJump() && this.entityData.get(DASH_COOLDOWN) <= 0 && this.getStamina() >= DASH_STAMINA_COST;
    }

    @Override
    public void handleStartJump(int scale) {
        this.allowStandSliding = true;
        this.standIfPossible();
    }

    @Override
    protected void playJumpSound() {
    }

    @Override
    protected void executeRidersJump(float power, Vec3 input) {
        float yaw = this.getYRot() * (float) (Math.PI / 180.0);
        double strength = 1.8 * power;
        this.setDeltaMovement(-Mth.sin(yaw) * strength, this.getDeltaMovement().y, Mth.cos(yaw) * strength);
        this.needsSync = true;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), OrpimentSounds.CRAW_DASH, SoundSource.PLAYERS, 1.0f, 1.0f);
        if (!this.level().isClientSide()) {
            this.setStamina(this.getStamina() - DASH_STAMINA_COST);
            this.entityData.set(DASH_COOLDOWN, DASH_COOLDOWN_TICKS);
            AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            speed.removeModifier(DASH_MODIFIER);
            speed.addTransientModifier(new AttributeModifier(DASH_MODIFIER, DASH_BOOST, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            this.dashBoostTicks = DASH_BOOST_TICKS;
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof SpawnEggItem) {
            return super.mobInteract(player, hand);
        }
        boolean wantsInventory = this.isTamed() && !this.isBaby() && player.isSecondaryUseActive();
        if (this.level().isClientSide()) {
            boolean canInteract = wantsInventory || this.isTamed() && this.isOwnedBy(player) || this.isFood(stack) || stack.is(Items.SADDLE);
            return canInteract ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        if (wantsInventory) {
            this.openCustomInventoryScreen(player);
            return InteractionResult.CONSUME;
        }
        if (this.isFood(stack)) {
            return this.isBaby() ? this.feedBaby(player, stack) : this.feed(player, stack);
        }
        if (!this.isTamed()) {
            return InteractionResult.PASS;
        }
        if (stack.is(Items.SADDLE) && !this.isSaddled() && !this.isBaby() && this.isOwnedBy(player)) {
            this.setItemSlot(EquipmentSlot.SADDLE, stack.consumeAndReturn(1, player));
            this.playSound(SoundEvents.HORSE_SADDLE.value(), 0.5f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        if (this.isOwnedBy(player) && !this.isVehicle() && !this.isBaby()) {
            if (this.getStamina() > 0) {
                this.doPlayerRide(player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private boolean isOwnedBy(Player player) {
        return player.equals(this.getOwner());
    }

    private InteractionResult feedBaby(Player player, ItemStack stack) {
        stack.consume(1, player);
        this.ageUp(60);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 6, 0.2, 0.2, 0.2, 0.0);
        }
        this.gameEvent(GameEvent.EAT);
        this.setPersistenceRequired();
        return InteractionResult.SUCCESS;
    }

    private InteractionResult feed(Player player, ItemStack stack) {
        boolean plainFeed = false;
        if (!this.isTamed()) {
            if (this.random.nextInt(3) == 0) {
                this.tameWithName(player);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
            }
        } else {
            boolean entersLove = this.isOwnedBy(player) && !this.isBaby() && this.getAge() == 0 && !this.isInLove();
            if (!entersLove && this.getStamina() >= MAX_STAMINA) {
                return InteractionResult.PASS;
            }
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(4.0f);
            }
            if (entersLove) {
                this.setInLove(player);
            }
            plainFeed = !entersLove;
        }
        stack.consume(1, player);
        this.addStamina(20);
        this.gameEvent(GameEvent.EAT);
        this.setPersistenceRequired();
        if (plainFeed && this.level() instanceof ServerLevel level) {
            this.playSound(OrpimentSounds.CRAW_EAT, 1.0f, 1.0f);
            level.sendParticles(OrpimentParticles.YELLOW_GLINT, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 10, 0.15, 0.15, 0.15, 0.02);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canMate(Animal other) {
        return other != this && other instanceof Craw craw && this.canParent() && craw.canParent();
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal mate) {
        ItemEntity egg = new ItemEntity(level, this.getX(), this.getY(), this.getZ(), new ItemStack(OrpimentBlocks.CRAW_EGG));
        egg.setDefaultPickUpDelay();
        this.finalizeSpawnChildFromBreeding(level, mate, null);
        this.playSound(SoundEvents.SNIFFER_EGG_PLOP, 1.0f, (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 0.5f);
        level.addFreshEntity(egg);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Craw child = OrpimentMobs.CRAW.create(level, EntitySpawnReason.BREEDING);
        if (child != null) {
            int mine = this.getVariant();
            int theirs = partner instanceof Craw craw ? craw.getVariant() : mine;
            child.setVariant(mine == theirs || this.random.nextBoolean() ? mine : theirs);
        }
        return child;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(OrpimentItems.BIRD_FEED);
    }

    @Override
    protected void randomizeAttributes(RandomSource random) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(36.0 + random.nextInt(5));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.27 + random.nextDouble() * 0.03);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0 + random.nextInt(2));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        this.setVariant(rollVariant(this.random));
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }
}
