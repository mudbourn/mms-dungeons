package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

// A fire-proof horse that backs away, then charges down its target and sets whatever it hits alight.
public class NightmareHorse extends HorseMob {

    private static final EntityDataAccessor<Integer> VARIANT =
        SynchedEntityData.defineId(NightmareHorse.class, EntityDataSerializers.INT);

    private static final double CHARGE_SPEED = 0.55;
    private static final double BACKUP_SPEED = 0.12;
    private static final int CHARGE_COOLDOWN = 120;
    private static final int WINDUP_TICKS = 20;
    private static final int CHARGE_TICKS = 25;
    private static final double CHARGE_MIN_RANGE = 6.0;
    private static final double CHARGE_MAX_RANGE = 16.0;
    private static final double CHARGE_KNOCKBACK = 1.2;
    private static final float CHARGE_FIRE_SECONDS = 4.0f;
    private static final double HIT_MARGIN = 0.4;

    private final Set<UUID> struck = new HashSet<>();
    private int chargeCooldown;
    private int windup;
    private int charge;
    private Vec3 chargeDirection = Vec3.ZERO;

    public NightmareHorse(EntityType<? extends NightmareHorse> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.225)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 2.6);
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant == 0 ? 0 : 1);
    }

    @Override
    public boolean isBusy() {
        return super.isBusy() || this.windup > 0 || this.charge > 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.chargeCooldown > 0) {
            this.chargeCooldown--;
        }
        LivingEntity target = this.getTarget();
        boolean live = target != null && target.isAlive();
        if (this.windup > 0) {
            this.tickWindup(live ? target : null);
        } else if (this.charge > 0) {
            this.tickCharge(level);
        } else if (live && this.canStartCharge(target)) {
            this.windup = WINDUP_TICKS;
            this.getNavigation().stop();
            GeoTriggers.trigger(this, "rear");
        }
    }

    private boolean canStartCharge(LivingEntity target) {
        double distance = this.distanceTo(target);
        return this.chargeCooldown == 0
            && !this.isStriking()
            && distance >= CHARGE_MIN_RANGE
            && distance <= CHARGE_MAX_RANGE
            && this.hasLineOfSight(target);
    }

    private void tickWindup(@Nullable LivingEntity target) {
        if (target == null) {
            this.windup = 0;
            return;
        }
        Vec3 toward = new Vec3(target.getX() - this.getX(), 0.0, target.getZ() - this.getZ());
        if (toward.lengthSqr() > 1.0E-4) {
            this.chargeDirection = toward.normalize();
        }
        Steering.faceToward(this, target.position());
        Steering.walkToward(this, this.position().subtract(this.chargeDirection), BACKUP_SPEED, 0.3);
        if (--this.windup == 0) {
            this.charge = CHARGE_TICKS;
            this.struck.clear();
        }
    }

    private void tickCharge(ServerLevel level) {
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(this.chargeDirection.x * CHARGE_SPEED, motion.y, this.chargeDirection.z * CHARGE_SPEED);
        level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.3, this.getZ(), 4, 0.4, 0.1, 0.4, 0.02);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 behind = this.position().subtract(this.chargeDirection);
        for (LivingEntity victim : level.getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox().inflate(HIT_MARGIN),
            entity -> entity != this && !this.hasPassenger(entity) && !this.struck.contains(entity.getUUID())
        )) {
            this.struck.add(victim.getUUID());
            victim.hurtServer(level, this.damageSources().mobAttack(this), damage);
            this.shove(victim, CHARGE_KNOCKBACK, behind.x, behind.z);
            victim.igniteForSeconds(CHARGE_FIRE_SECONDS);
        }
        if (--this.charge == 0 || this.horizontalCollision) {
            this.charge = 0;
            this.chargeCooldown = CHARGE_COOLDOWN;
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        this.setVariant(this.random.nextInt(2));
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_HORSE_DEATH;
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
