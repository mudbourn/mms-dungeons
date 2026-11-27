package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEffects;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// A pool of acid that burns and corrodes everything standing in it until it dries up.
public class AcidCloud extends Entity {

    private static final EntityDataAccessor<Float> WIDTH =
        SynchedEntityData.defineId(AcidCloud.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT =
        SynchedEntityData.defineId(AcidCloud.class, EntityDataSerializers.FLOAT);

    private static final int DEFAULT_LIFETIME = 200;
    private static final int APPLY_INTERVAL = 20;
    private static final int ACID_TICKS = 120;
    private static final int PARTICLES = 4;
    private static final int PUDDLE_SOUND_TICKS = 74;
    private static final int ACID_COLOR = 0xFF76F154;
    private static final float DEFAULT_DAMAGE = 1.0f;

    private int lifetime = DEFAULT_LIFETIME;
    private float tickDamage = DEFAULT_DAMAGE;

    public AcidCloud(EntityType<? extends AcidCloud> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }

    public void setTickDamage(float tickDamage) {
        this.tickDamage = tickDamage;
    }

    public void setSize(float width, float height) {
        this.entityData.set(WIDTH, width);
        this.entityData.set(HEIGHT, height);
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WIDTH, 2.0f);
        builder.define(HEIGHT, 1.0f);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(this.entityData.get(WIDTH), this.entityData.get(HEIGHT));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (WIDTH.equals(accessor) || HEIGHT.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.tickCount >= this.lifetime) {
            this.discard();
            return;
        }
        this.spawnParticles(level);
        if (this.tickCount % PUDDLE_SOUND_TICKS == 1) {
            this.playSound(FangsNClawsSounds.ACID_PUDDLE, 0.8f, 1.0f);
        }
        if (this.tickCount % APPLY_INTERVAL == 0) {
            this.burn(level);
        }
    }

    private void burn(ServerLevel level) {
        DamageSource source = this.damageSources().magic();
        for (LivingEntity victim : level.getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox(),
            entity -> !PurpleWorm.isKin(entity)
        )) {
            victim.hurtServer(level, source, this.tickDamage);
            victim.addEffect(new MobEffectInstance(FangsNClawsEffects.CORRODED, ACID_TICKS, 0));
        }
    }

    private void spawnParticles(ServerLevel level) {
        double spreadX = this.getBbWidth() * 0.5;
        level.sendParticles(
            ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ACID_COLOR),
            this.getX(),
            this.getY() + this.getBbHeight() * 0.5,
            this.getZ(),
            PARTICLES,
            spreadX,
            this.getBbHeight() * 0.4,
            spreadX,
            0.0
        );
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.lifetime = input.getIntOr("Lifetime", DEFAULT_LIFETIME);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Lifetime", this.lifetime);
    }
}
