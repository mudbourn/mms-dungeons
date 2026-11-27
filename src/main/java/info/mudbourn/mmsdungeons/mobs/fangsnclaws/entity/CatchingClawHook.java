package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEnchantments;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsGear;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

// The hook a Catching Claw throws: it sticks to a creature, or a dropped item, and reels it in on retrieve.
public class CatchingClawHook extends ThrowableProjectile {

    private static final EntityDataAccessor<Integer> HOOKED_ID =
        SynchedEntityData.defineId(CatchingClawHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IN_GROUND =
        SynchedEntityData.defineId(CatchingClawHook.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NETHERITE =
        SynchedEntityData.defineId(CatchingClawHook.class, EntityDataSerializers.BOOLEAN);

    private static final double GRAVITY = 0.03;
    private static final double DRAG = 0.929;
    private static final int MAX_LIFETIME = 800;
    private static final double PULL_BASE = 1.0;
    private static final double PULL_PER_ROOT_BLOCK = 0.15;
    private static final double AIM_WEIGHT = 2.0;
    private static final double CLAW_DAMAGE = 2.0;
    private static final double NETHERITE_CLAW_DAMAGE = 3.0;
    private static final double SCRATCH_DAMAGE = 0.25;

    public CatchingClawHook(EntityType<? extends CatchingClawHook> type, Level level) {
        super(type, level);
    }

    public CatchingClawHook(Player owner, Level level, boolean netherite) {
        super(FangsNClawsMobs.CATCHING_CLAW_HOOK, level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.entityData.set(NETHERITE, netherite);
    }

    public boolean isNetherite() {
        return this.entityData.get(NETHERITE);
    }

    public boolean isInGround() {
        return this.entityData.get(IN_GROUND);
    }

    public boolean isHooked() {
        return this.entityData.get(HOOKED_ID) > 0;
    }

    public Entity getHookedEntity() {
        int id = this.entityData.get(HOOKED_ID);
        return id > 0 ? this.level().getEntity(id - 1) : null;
    }

    private void setHookedEntity(Entity entity) {
        this.entityData.set(HOOKED_ID, entity == null ? 0 : entity.getId() + 1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HOOKED_ID, 0);
        builder.define(IN_GROUND, false);
        builder.define(NETHERITE, false);
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    private ItemStack clawStack() {
        if (this.getOwner() instanceof Player player) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = player.getItemInHand(hand);
                if (held.is(FangsNClawsGear.CATCHING_CLAW) || held.is(FangsNClawsGear.CATCHING_CLAW_NETHERITE)) {
                    return held;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private int enchantLevel(ResourceKey<Enchantment> enchantment) {
        ItemStack claw = this.clawStack();
        return claw.isEmpty() ? 0 : FangsNClawsEnchantments.level(claw, this.level(), enchantment);
    }

    private float hookDamage() {
        double bonus = (this.isNetherite() ? NETHERITE_CLAW_DAMAGE : CLAW_DAMAGE)
            + SCRATCH_DAMAGE * this.enchantLevel(FangsNClawsEnchantments.SCRATCH);
        double base = this.getOwner() instanceof LivingEntity owner
            ? owner.getAttributeValue(Attributes.ATTACK_DAMAGE)
            : 1.0;
        return (float) (base + bonus);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity instanceof ItemEntity) {
            return entity.isAlive() && this.enchantLevel(FangsNClawsEnchantments.ITEM_CATCHER) > 0;
        }
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = result.getEntity();
        if (entity == this.getOwner() || this.isHooked()) {
            return;
        }
        if (entity instanceof LivingEntity living) {
            living.hurtServer(level, this.damageSources().thrown(this, this.getOwner()), this.hookDamage());
            this.setHookedEntity(living);
            this.setDeltaMovement(Vec3.ZERO);
        } else if (entity instanceof ItemEntity) {
            this.setHookedEntity(entity);
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (this.level().isClientSide()) {
            return;
        }
        this.entityData.set(IN_GROUND, true);
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide()) {
            if (this.tickCount > MAX_LIFETIME || this.getOwner() == null || !this.getOwner().isAlive()) {
                this.discard();
                return;
            }
        }
        Entity hooked = this.getHookedEntity();
        if (hooked != null) {
            if (!this.level().isClientSide() && !hooked.isAlive()) {
                this.setHookedEntity(null);
            }
            this.setPos(hooked.getX(), hooked.getY() + hooked.getBbHeight() * 0.5, hooked.getZ());
            this.setDeltaMovement(Vec3.ZERO);
            this.baseTick();
            return;
        }
        if (this.isInGround()) {
            this.baseTick();
            return;
        }
        super.tick();
        this.setDeltaMovement(this.getDeltaMovement().scale(DRAG));
    }

    // Reels in whatever the hook holds toward the player, then removes the hook.
    public void retrieve(Player player) {
        if (this.level() instanceof ServerLevel level) {
            Entity hooked = this.getHookedEntity();
            if (hooked instanceof LivingEntity living) {
                Vec3 toPlayer = player.position().subtract(living.position());
                double distance = toPlayer.length();
                Vec3 direction = distance > 1.0E-6 ? toPlayer.normalize() : Vec3.ZERO;
                Vec3 pull = direction.add(player.getLookAngle().scale(AIM_WEIGHT)).normalize();
                double strength = PULL_BASE + Math.sqrt(distance) * PULL_PER_ROOT_BLOCK;
                living.setDeltaMovement(pull.scale(strength));
                living.setInvulnerableTime(0);
                living.hurtServer(level, this.damageSources().thrown(this, player), this.hookDamage());
            } else if (hooked instanceof ItemEntity item) {
                ItemStack stack = item.getItem().copy();
                if (!player.addItem(stack)) {
                    player.spawnAtLocation(level, stack);
                }
                item.discard();
            }
        }
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("InGround", this.isInGround());
        output.putBoolean("Netherite", this.isNetherite());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(IN_GROUND, input.getBooleanOr("InGround", false));
        this.entityData.set(NETHERITE, input.getBooleanOr("Netherite", false));
    }
}
