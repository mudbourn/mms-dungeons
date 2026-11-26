package info.mudbourn.mmsdungeons.mobs.knightquest.entity;

import info.mudbourn.mmsdungeons.mobs.knightquest.KnightQuestMobs;
import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// A small thief that turns angry at half health, then raises Ghastling shields or hits harder, and calms for a gold ingot.
public class Gremlin extends Monster {

    private static final EntityDataAccessor<Boolean> PASSIVE = SynchedEntityData.defineId(Gremlin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHIELDER = SynchedEntityData.defineId(Gremlin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(Gremlin.class, EntityDataSerializers.INT);

    private static final int PASSIVE_TICKS = 80;
    private static final int SHIELD_INTERVAL = 22;
    private static final int SHIELD_WINDOW = 25;
    private static final double ATTACK_DAMAGE_MULTIPLIER = 1.2;
    private static final double MOVEMENT_SPEED_MULTIPLIER = 1.1;
    private static final double ATTACK_SPEED_MULTIPLIER = 1.15;

    private int passiveTicks;
    private int shieldTicks;

    public Gremlin(EntityType<? extends Gremlin> type, Level level) {
        super(type, level);
        if (!level.isClientSide()) {
            this.entityData.set(SHIELDER, this.random.nextBoolean());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 35.0)
            .add(Attributes.ATTACK_DAMAGE, 5.5)
            .add(Attributes.ATTACK_SPEED, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.63)
            .add(Attributes.FOLLOW_RANGE, 35.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PASSIVE, false);
        builder.define(SHIELDER, false);
        builder.define(PHASE, 1);
    }

    public boolean isPassive() {
        return this.entityData.get(PASSIVE);
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.5, true));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 0.5, stack -> stack.is(Items.WHEAT), false));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.targetSelector.addGoal(1, new CalmableTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.getHealth() < this.getMaxHealth() * 0.5) {
            if (this.getPhase() == 1) {
                this.enrage();
            }
            if (this.entityData.get(SHIELDER)) {
                this.shieldTicks++;
                if (this.shieldTicks % SHIELD_INTERVAL == 0 && this.shieldTicks / SHIELD_WINDOW <= 1) {
                    this.spawnShield();
                }
            }
        }
        if (this.isPassive()) {
            this.passiveTicks++;
            if (this.passiveTicks >= PASSIVE_TICKS) {
                this.entityData.set(PASSIVE, false);
                this.passiveTicks = 0;
            }
        }
    }

    private void enrage() {
        this.entityData.set(PHASE, 2);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 0.8, this.getZ(), 6, 0.4, 0.4, 0.4, 0.0);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0f, 1.0f);
        if (this.entityData.get(SHIELDER)) {
            this.spawnShield();
        } else {
            this.scaleAttribute(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_MULTIPLIER);
            this.scaleAttribute(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_MULTIPLIER);
            this.scaleAttribute(Attributes.ATTACK_SPEED, ATTACK_SPEED_MULTIPLIER);
        }
    }

    private void scaleAttribute(Holder<Attribute> attribute, double multiplier) {
        Objects.requireNonNull(this.getAttribute(attribute)).setBaseValue(this.getAttributeValue(attribute) * multiplier);
    }

    private void spawnShield() {
        if (this.level() instanceof ServerLevel level) {
            Ghastling shield = KnightQuestMobs.GHASTLING.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (shield != null) {
                BlockPos at = this.blockPosition();
                shield.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 1.0f, 0.0f);
                level.addFreshEntity(shield);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.GOLD_INGOT) && !this.isPassive()) {
            if (this.level() instanceof ServerLevel) {
                stack.consume(1, player);
                this.setTarget(null);
                this.setLastHurtByMob(null);
                this.setAggressive(false);
                this.passiveTicks = 0;
                this.entityData.set(PASSIVE, true);
                GeoTriggers.trigger(this, this.random.nextBoolean() ? "gold" : "gold2");
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isPassive()) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Phase", this.getPhase());
        output.putInt("ShieldTicks", this.shieldTicks);
        output.putBoolean("Shielder", this.entityData.get(SHIELDER));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(PHASE, input.getIntOr("Phase", 1));
        this.shieldTicks = input.getIntOr("ShieldTicks", 0);
        this.entityData.set(SHIELDER, input.getBooleanOr("Shielder", false));
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.AXOLOTL_SWIM;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WOLF_STEP.value(), 0.15f, 1.0f);
    }

    // Looks for a player to attack unless calmed with gold.
    private static class CalmableTargetGoal extends NearestAttackableTargetGoal<Player> {

        private final Gremlin gremlin;

        CalmableTargetGoal(Gremlin gremlin) {
            super(gremlin, Player.class, true);
            this.gremlin = gremlin;
        }

        @Override
        public boolean canUse() {
            return !this.gremlin.isPassive() && super.canUse();
        }
    }
}
