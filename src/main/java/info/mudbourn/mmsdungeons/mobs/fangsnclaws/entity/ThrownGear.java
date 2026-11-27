package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import java.util.Collection;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

// A thrown weapon that hurts the first thing it hits, then flies back to its owner when it can.
public abstract class ThrownGear extends AbstractArrow implements ItemSupplier {

    private static final EntityDataAccessor<Byte> RETURN_PULL =
        SynchedEntityData.defineId(ThrownGear.class, EntityDataSerializers.BYTE);

    private static final int GROUND_TICKS_BEFORE_SPENT = 4;
    private static final double RETURN_LIFT = 0.015;
    private static final double RETURN_ACCELERATION = 0.05;
    private static final double RETURN_DRAG = 0.95;
    private static final Vec3 AFTER_HIT_MOTION = new Vec3(0.02, 0.2, 0.02);

    private boolean dealtDamage;
    private int returnTicks;

    protected ThrownGear(EntityType<? extends ThrownGear> type, Level level) {
        super(type, level);
    }

    protected ThrownGear(EntityType<? extends ThrownGear> type, LivingEntity shooter, Level level, ItemStack stack) {
        super(type, shooter, level, stack, null);
    }

    protected ThrownGear(
        EntityType<? extends ThrownGear> type,
        double x,
        double y,
        double z,
        Level level,
        ItemStack stack
    ) {
        super(type, x, y, z, level, stack, null);
    }

    protected abstract float thrownDamage();

    protected abstract int returnPull(ServerLevel level, ItemStack stack);

    protected abstract SoundEvent hitSound();

    protected DamageSource damageSource(Entity owner) {
        return this.damageSources().thrown(this, owner == null ? this : owner);
    }

    protected void onDamaged(LivingEntity victim) {
    }

    protected void refreshReturnPull() {
        if (this.level() instanceof ServerLevel level) {
            int pull = this.returnPull(level, this.getPickupItemStackOrigin());
            this.entityData.set(RETURN_PULL, (byte) Mth.clamp(pull, 0, 127));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RETURN_PULL, (byte) 0);
    }

    public boolean isStuck() {
        return this.isInGround();
    }

    @Override
    public ItemStack getItem() {
        return this.getPickupItemStackOrigin();
    }

    @Override
    public ItemStack getWeaponItem() {
        return this.getPickupItemStackOrigin();
    }

    @Override
    public void tick() {
        if (this.inGroundTime > GROUND_TICKS_BEFORE_SPENT) {
            this.dealtDamage = true;
        }
        Entity owner = this.getOwner();
        int pull = this.entityData.get(RETURN_PULL);
        if (pull > 0 && (this.dealtDamage || this.isNoPhysics()) && owner != null) {
            if (!this.canReturnTo(owner)) {
                if (this.level() instanceof ServerLevel level && this.pickup == Pickup.ALLOWED) {
                    this.spawnAtLocation(level, this.getPickupItem(), 0.1f);
                }
                this.discard();
            } else {
                this.setNoPhysics(true);
                Vec3 toOwner = owner.getEyePosition().subtract(this.position());
                this.setPosRaw(this.getX(), this.getY() + toOwner.y * RETURN_LIFT * pull, this.getZ());
                double acceleration = RETURN_ACCELERATION * pull;
                this.setDeltaMovement(
                    this.getDeltaMovement().scale(RETURN_DRAG).add(toOwner.normalize().scale(acceleration))
                );
                if (this.returnTicks == 0) {
                    this.playSound(SoundEvents.TRIDENT_RETURN, 10.0f, 1.0f);
                }
                this.returnTicks++;
            }
        }
        super.tick();
    }

    private boolean canReturnTo(Entity owner) {
        return owner.isAlive() && !(owner instanceof ServerPlayer player && player.isSpectator());
    }

    @Override
    protected EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
        return this.dealtDamage ? null : super.findHitEntity(from, to);
    }

    @Override
    protected Collection<EntityHitResult> findHitEntities(Vec3 from, Vec3 to) {
        return this.dealtDamage ? List.of() : super.findHitEntities(from, to);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity victim = result.getEntity();
        DamageSource source = this.damageSource(this.getOwner());
        float damage = this.thrownDamage();
        if (this.level() instanceof ServerLevel level) {
            damage = EnchantmentHelper.modifyDamage(level, this.getWeaponItem(), victim, source, damage);
        }
        this.dealtDamage = true;
        boolean hurt = victim.hurtOrSimulate(source, damage);
        if (hurt) {
            if (this.level() instanceof ServerLevel level) {
                EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak(
                    level,
                    victim,
                    source,
                    this.getWeaponItem(),
                    broken -> this.discard()
                );
            }
            if (victim instanceof LivingEntity living) {
                this.doKnockback(living, source);
                this.doPostHurtEffects(living);
                this.onDamaged(living);
            }
        }
        if (victim.projectileReceivesSideEffectsOnHit(hurt)) {
            this.playSound(this.hitSound(), 1.0f, 1.0f);
            this.deflect(ProjectileDeflection.REVERSE, victim, this.owner, false, AFTER_HIT_MOTION);
        }
    }

    @Override
    protected boolean tryPickup(Player player) {
        return super.tryPickup(player)
            || this.isNoPhysics() && this.ownedBy(player) && player.getInventory().add(this.getPickupItem());
    }

    @Override
    public void playerTouch(Player player) {
        if (this.ownedBy(player) || this.getOwner() == null) {
            super.playerTouch(player);
        }
    }

    @Override
    public void tickDespawn() {
        if (this.pickup != Pickup.ALLOWED || this.entityData.get(RETURN_PULL) <= 0) {
            super.tickDespawn();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("DealtDamage", this.dealtDamage);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.dealtDamage = input.getBooleanOr("DealtDamage", false);
        this.refreshReturnPull();
    }
}
