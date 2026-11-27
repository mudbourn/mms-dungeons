package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import java.util.EnumSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// A bat-winged horse that takes to the air against its target and rears at it in the air.
public class HorseBat extends HorseMob {

    private static final EntityDataAccessor<Boolean> FLYING =
        SynchedEntityData.defineId(HorseBat.class, EntityDataSerializers.BOOLEAN);

    private static final double FLY_SPEED = 0.3;
    private static final double FLY_BLEND = 0.15;

    public HorseBat(EntityType<? extends HorseBat> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.225)
            .add(Attributes.FLYING_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 2.6);
    }

    public boolean isFlying() {
        return this.entityData.get(FLYING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
    }

    @Override
    protected void registerAttackGoals() {
        this.goalSelector.addGoal(2, new AirStrikeGoal());
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return !this.isFlying() && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_HORSE_DEATH;
    }

    private class AirStrikeGoal extends Goal {

        AirStrikeGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = HorseBat.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            HorseBat.this.entityData.set(FLYING, true);
            HorseBat.this.setNoGravity(true);
            HorseBat.this.getNavigation().stop();
        }

        @Override
        public void stop() {
            HorseBat.this.entityData.set(FLYING, false);
            HorseBat.this.setNoGravity(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            HorseBat bat = HorseBat.this;
            LivingEntity target = bat.getTarget();
            if (target == null) {
                return;
            }
            bat.resetFallDistance();
            bat.getLookControl().setLookAt(target, 30.0f, 30.0f);
            Steering.faceToward(bat, target.position());
            if (!bat.isStriking()) {
                Vec3 chest = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
                Steering.flyToward(bat, chest, FLY_SPEED, FLY_BLEND);
            }
            if (bat.isReadyToStrike() && bat.distanceTo(target) <= bat.reach() && bat.hasLineOfSight(target)) {
                bat.startAttack(target);
            }
        }
    }
}
