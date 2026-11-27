package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEffects;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

// A lob of acid that hurts what it hits and leaves a pool of acid where it lands.
public class AcidSplit extends ThrowableProjectile {

    private static final EntityDataAccessor<Float> SCALE =
        SynchedEntityData.defineId(AcidSplit.class, EntityDataSerializers.FLOAT);

    private static final double GRAVITY = 0.03;
    private static final int MAX_LIFETIME = 200;
    private static final float DAMAGE = 8.0f;
    private static final int ACID_TICKS = 100;
    private static final float POOL_WIDTH = 2.0f;
    private static final float POOL_HEIGHT = 0.6f;
    private static final int POOL_TICKS = 160;

    public AcidSplit(EntityType<? extends AcidSplit> type, Level level) {
        super(type, level);
    }

    public AcidSplit(Level level, LivingEntity owner, float scale) {
        super(FangsNClawsMobs.ACID_SPLIT, level);
        this.setOwner(owner);
        this.entityData.set(SCALE, scale);
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0f);
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !PurpleWorm.isKin(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount > MAX_LIFETIME) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level() instanceof ServerLevel level && result.getEntity() instanceof LivingEntity living) {
            DamageSource source = this.damageSources().indirectMagic(this, this.getOwner());
            living.hurtServer(level, source, DAMAGE * this.getScale());
            living.addEffect(new MobEffectInstance(FangsNClawsEffects.CORRODED, ACID_TICKS, 0));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel level) {
            this.splash(level);
        }
    }

    private void splash(ServerLevel level) {
        this.playSound(FangsNClawsSounds.ACID_SPLASH, 1.0f, 1.0f);
        AcidCloud pool = new AcidCloud(FangsNClawsMobs.ACID_CLOUD, level);
        pool.setPos(this.getX(), this.getY(), this.getZ());
        pool.setSize(POOL_WIDTH * this.getScale(), POOL_HEIGHT);
        pool.setLifetime(POOL_TICKS);
        level.addFreshEntity(pool);
        this.discard();
    }
}
