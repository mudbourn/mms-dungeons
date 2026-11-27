package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A fire-breathing ogre of the Nether that scorches everything in a cone ahead of it.
public class HellOgre extends Ogre {

    private static final EntityDataAccessor<Boolean> BREATHING =
        SynchedEntityData.defineId(HellOgre.class, EntityDataSerializers.BOOLEAN);

    private static final int BREATH_TICKS = 80;
    private static final int BREATH_START = 10;
    private static final int BREATH_END = 70;
    private static final int BREATH_SOUND_INTERVAL = 20;
    private static final int BREATH_COOLDOWN = 180;
    private static final float BREATH_DAMAGE = 3.0f;
    private static final double BREATH_RANGE = 10.0;
    private static final double BREATH_MIN_RANGE = 5.0;
    private static final int BURN_TICKS = 80;
    private static final float CONE_COS = Mth.cos((float) (Math.PI * 2.0 / 9.0));

    private int breathTick;
    private int breathCooldown;

    public HellOgre(EntityType<? extends HellOgre> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Ogre.createAttributes().add(Attributes.MAX_HEALTH, 70.0).add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BREATHING, false);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        this.setSiamese(false);
        return data;
    }

    @Override
    protected double slamDamage() {
        return 10.0;
    }

    @Override
    public boolean isBusy() {
        return super.isBusy() || this.breathTick > 0;
    }

    @Override
    public boolean isRooted() {
        return this.breathTick > 0;
    }

    public boolean isFireBreathing() {
        return this.entityData.get(BREATHING);
    }

    @Override
    public boolean useSpecialAttack(LivingEntity target) {
        if (this.breathCooldown > 0) {
            return false;
        }
        double distance = this.distanceTo(target);
        if (distance <= BREATH_MIN_RANGE || distance > BREATH_RANGE || !this.hasLineOfSight(target)) {
            return false;
        }
        GeoTriggers.trigger(this, "fire_breath_attack");
        this.breathTick = 1;
        this.breathCooldown = BREATH_COOLDOWN;
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level) {
            if (this.breathCooldown > 0) {
                this.breathCooldown--;
            }
            this.tickBreath(level);
        } else if (this.isFireBreathing()) {
            this.spawnFlames();
        }
    }

    private void tickBreath(ServerLevel level) {
        if (this.breathTick <= 0) {
            return;
        }
        this.breathTick++;
        boolean active = this.breathTick >= BREATH_START && this.breathTick <= BREATH_END;
        this.entityData.set(BREATHING, active);
        if (active) {
            if (this.breathTick == BREATH_START) {
                this.playSound(FangsNClawsSounds.OGRE_ROAR, 1.5f, 1.0f);
            }
            this.scorch(level);
            if ((this.breathTick - BREATH_START) % BREATH_SOUND_INTERVAL == 0) {
                this.playSound(FangsNClawsSounds.HELL_OGRE_FIRE_BREATH, 3.0f, 1.0f);
            }
        }
        if (this.breathTick >= BREATH_TICKS) {
            this.breathTick = 0;
            this.entityData.set(BREATHING, false);
        }
    }

    private void scorch(ServerLevel level) {
        Vec3 look = this.calculateViewVector(this.getXRot(), this.yHeadRot);
        AABB reach = this.getBoundingBox().inflate(BREATH_RANGE);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, reach)) {
            if (victim instanceof Ogre) {
                continue;
            }
            Vec3 toVictim = new Vec3(
                victim.getX() - this.getX(),
                victim.getY() + victim.getBbHeight() * 0.5 - this.getEyeY(),
                victim.getZ() - this.getZ()
            );
            if (toVictim.lengthSqr() <= BREATH_RANGE * BREATH_RANGE && look.dot(toVictim.normalize()) >= CONE_COS) {
                victim.hurtServer(level, this.damageSources().mobAttack(this), BREATH_DAMAGE);
                victim.setRemainingFireTicks(BURN_TICKS);
            }
        }
    }

    private void spawnFlames() {
        Vec3 look = this.calculateViewVector(this.getXRot(), this.yHeadRot).normalize();
        Vec3 mouth = new Vec3(this.getX(), this.getY() + this.getEyeHeight() * 0.7, this.getZ()).add(look.scale(1.5));
        double speed = this.random.nextDouble() * 0.35 + 0.35;
        for (int i = 0; i < 8; i++) {
            Vec3 jitter = new Vec3(
                this.random.nextDouble() * 0.9 - 0.45,
                this.random.nextDouble() * 0.9 - 0.45,
                this.random.nextDouble() * 0.9 - 0.45
            ).normalize();
            Vec3 velocity = look.scale(3.0).add(jitter).normalize().scale(speed);
            this.level().addParticle(
                ParticleTypes.FLAME,
                mouth.x + this.random.nextDouble() * 0.3 - 0.15,
                mouth.y + this.random.nextDouble() * 0.3 - 0.15,
                mouth.z + this.random.nextDouble() * 0.3 - 0.15,
                velocity.x,
                velocity.y,
                velocity.z
            );
        }
    }
}
