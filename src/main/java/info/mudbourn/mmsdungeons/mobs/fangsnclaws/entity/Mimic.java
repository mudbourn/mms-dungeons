package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.Identifier;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

// A chest that eats whoever opens it, then hops after them and drops what it swallowed when it dies.
public class Mimic extends Monster {

    private static final EntityDataAccessor<Boolean> REVEALED =
        SynchedEntityData.defineId(Mimic.class, EntityDataSerializers.BOOLEAN);

    private static final int CATCH_TICKS = 20;
    private static final int CHEW_TICKS = 35;
    private static final int CHEW_BEAT = 10;
    private static final float CHEW_DAMAGE = 2.0f;
    private static final float AXE_FACTOR = 1.5f;
    private static final double HOP_SPEED = 0.7;
    private static final double HOP_LIFT = 0.55;
    private static final double HOP_RANGE_SQUARED = 256.0;
    private static final int TOUCH_GAP = 20;
    private static final float KEEP_CHANCE = 0.3f;
    private static final String RANDOM_SIZE_SKIP = "random_mob_sizes.checked";
    private static final List<Identifier> RANDOM_SIZE_MODIFIERS = List.of(
        Identifier.parse("random_mob_sizes:health_scale"),
        Identifier.parse("random_mob_sizes:speed_scale"),
        Identifier.parse("random_mob_sizes:damage_scale")
    );

    private final List<ItemStack> storedLoot = new ArrayList<>();
    private Player trapped;
    private int catchTicks;
    private int chewTicks;
    private Vec3 catchStart = Vec3.ZERO;
    private Vec3 mouth = Vec3.ZERO;
    private int hopCooldown;
    private int lastTouch;

    public Mimic(EntityType<? extends Mimic> type, Level level) {
        super(type, level);
        this.addTag(RANDOM_SIZE_SKIP);
    }

    // Resets a size rolled by the Random Mob Sizes datapack.
    private void clearRandomSize() {
        this.getAttribute(Attributes.SCALE).setBaseValue(1.0);
        for (Identifier modifier : RANDOM_SIZE_MODIFIERS) {
            this.getAttribute(Attributes.MAX_HEALTH).removeModifier(modifier);
            this.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(modifier);
            this.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(modifier);
        }
        this.setHealth(Math.min(this.getHealth(), this.getMaxHealth()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REVEALED, false);
    }

    public boolean isRevealed() {
        return this.entityData.get(REVEALED);
    }

    private void reveal() {
        this.entityData.set(REVEALED, true);
        this.playSound(FangsNClawsSounds.MIMIC_OPEN, 1.0f, 1.0f);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>(this, Player.class, false) {
            @Override
            public boolean canUse() {
                return Mimic.this.isRevealed() && super.canUse();
            }
        });
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isRevealed() || this.trapped != null || !this.isAlive()) {
            return super.mobInteract(player, hand);
        }
        if (!this.level().isClientSide()) {
            if (player.getAbilities().instabuild || player.isSpectator()) {
                this.reveal();
            } else {
                this.beginCatch(player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private void beginCatch(Player player) {
        this.reveal();
        this.trapped = player;
        this.catchTicks = CATCH_TICKS;
        this.catchStart = player.position();
        Vec3 toPlayer = player.position().subtract(this.position());
        Vec3 flat = new Vec3(toPlayer.x, 0.0, toPlayer.z);
        this.mouth = this.position().add(flat.lengthSqr() > 1.0E-4 ? flat.normalize().scale(0.4) : Vec3.ZERO);
        this.faceYaw(toPlayer);
        GeoTriggers.trigger(this, "catch");
    }

    private void faceYaw(Vec3 toward) {
        float yaw = (float) (Mth.atan2(toward.z, toward.x) * 180.0 / Math.PI) - 90.0f;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean axe = source.getDirectEntity() instanceof LivingEntity attacker
            && attacker.getMainHandItem().is(ItemTags.AXES);
        float scaled = axe ? amount * AXE_FACTOR : amount;
        boolean hurt = super.hurtServer(level, source, scaled);
        if (hurt && !this.isRevealed()) {
            this.reveal();
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.trapped != null) {
            this.tickTrap(level);
        } else if (this.isRevealed() && this.isAlive()) {
            this.tickHop();
        } else {
            float snapped = Math.round(this.getYRot() / 90.0f) * 90.0f;
            this.setYRot(snapped);
            this.setYBodyRot(snapped);
            this.setYHeadRot(snapped);
        }
    }

    private void tickTrap(ServerLevel level) {
        Player victim = this.trapped;
        boolean holdable = victim.isAlive()
            && !victim.isRemoved()
            && victim.level() == this.level()
            && !victim.isSpectator()
            && !victim.getAbilities().instabuild
            && !this.isDeadOrDying();
        if (!holdable) {
            this.release();
        } else if (this.catchTicks > 0) {
            this.catchTicks--;
            this.hold(victim, this.catchStart.lerp(this.mouth, 1.0 - this.catchTicks / (double) CATCH_TICKS));
            if (this.catchTicks == 0) {
                this.chewTicks = CHEW_TICKS;
                GeoTriggers.trigger(this, "attack_chew");
            }
        } else if (this.chewTicks > 0) {
            this.chewTicks--;
            this.hold(victim, this.mouth);
            boolean bite = this.chewTicks % CHEW_BEAT == CHEW_BEAT / 2;
            if (bite && victim.hurtServer(level, this.damageSources().mobAttack(this), CHEW_DAMAGE)) {
                victim.setInvulnerableTime(0);
            }
            if (this.chewTicks == 0) {
                this.release();
            }
        } else {
            this.release();
        }
    }

    private void hold(Player victim, Vec3 at) {
        victim.teleportTo(at.x, at.y, at.z);
        victim.setDeltaMovement(Vec3.ZERO);
        victim.resetFallDistance();
    }

    private void release() {
        Player victim = this.trapped;
        if (victim != null && victim.isAlive() && !victim.isSpectator() && !victim.getAbilities().instabuild) {
            this.setTarget(victim);
        }
        this.trapped = null;
        this.catchTicks = 0;
        this.chewTicks = 0;
    }

    private void tickHop() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        this.getLookControl().setLookAt(target);
        if (this.hopCooldown > 0) {
            this.hopCooldown--;
            return;
        }
        if (!this.onGround() || this.distanceToSqr(target) > HOP_RANGE_SQUARED) {
            return;
        }
        Vec3 toTarget = target.position().subtract(this.position());
        Vec3 flat = new Vec3(toTarget.x, 0.0, toTarget.z);
        Vec3 direction = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : Vec3.ZERO;
        this.faceYaw(toTarget);
        this.setDeltaMovement(direction.x * HOP_SPEED, HOP_LIFT, direction.z * HOP_SPEED);
        this.needsSync = true;
        this.playSound(SoundEvents.CHEST_OPEN, 1.0f, 0.8f + (this.random.nextFloat() - 0.5f) * 0.2f);
        GeoTriggers.trigger(this, "jump");
        this.hopCooldown = 25 + this.random.nextInt(15);
    }

    @Override
    public void playerTouch(Player player) {
        if (this.level() instanceof ServerLevel level
            && this.isRevealed()
            && this.trapped == null
            && this.isAlive()
            && this.tickCount - this.lastTouch >= TOUCH_GAP
            && this.getTarget() == player
            && this.doHurtTarget(level, player)) {
            this.lastTouch = this.tickCount;
        }
    }

    @Override
    public boolean isPushable() {
        return this.isRevealed() && super.isPushable();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // Swallows the chest and digests some of its stacks; the rest drop on death.
    public void consumeChest(BlockPos pos) {
        Level level = this.level();
        BlockState state = level.getBlockState(pos);
        if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity chest) {
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                ItemStack stack = chest.getItem(slot);
                if (!stack.isEmpty()) {
                    this.storedLoot.add(stack.copy());
                }
                chest.setItem(slot, ItemStack.EMPTY);
            }
            this.storedLoot.removeIf(stack -> this.getRandom().nextFloat() >= KEEP_CHANCE);
        }
        Direction facing = state.hasProperty(ChestBlock.FACING) ? state.getValue(ChestBlock.FACING) : Direction.NORTH;
        float yaw = facing.getAxis() == Direction.Axis.Y ? 0.0f : facing.toYRot();
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        level.removeBlock(pos, false);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (ItemStack stack : this.storedLoot) {
            this.spawnAtLocation(level, stack);
        }
        this.storedLoot.clear();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Revealed", this.isRevealed());
        ValueOutput.TypedOutputList<ItemStack> list = output.list("StoredLoot", ItemStack.CODEC);
        this.storedLoot.forEach(list::add);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(REVEALED, input.getBooleanOr("Revealed", false));
        this.storedLoot.clear();
        input.listOrEmpty("StoredLoot", ItemStack.CODEC).forEach(this.storedLoot::add);
        this.addTag(RANDOM_SIZE_SKIP);
        this.clearRandomSize();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHIELD_BLOCK.value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FangsNClawsSounds.MIMIC_DEATH;
    }
}
