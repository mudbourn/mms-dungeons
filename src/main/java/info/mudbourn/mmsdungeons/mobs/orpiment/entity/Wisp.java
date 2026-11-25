package info.mudbourn.mmsdungeons.mobs.orpiment.entity;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// A small drifting flame spirit that rams its target.
public class Wisp extends Monster {

    private static final double WANDER_RANGE = 16.0;

    public Wisp(EntityType<? extends Wisp> type, Level level) {
        super(type, level);
        this.xpReward = 2;
        this.moveControl = new FlyingMoveControl(this, 10, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.ARMOR, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.FOLLOW_RANGE, 16.0)
            .add(Attributes.FLYING_SPEED, 0.2);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new RamGoal(this));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.3, 20) {
            @Override
            protected Vec3 getPosition() {
                RandomSource random = Wisp.this.getRandom();
                return new Vec3(
                    Wisp.this.getX() + (random.nextFloat() * 2.0f - 1.0f) * WANDER_RANGE,
                    Wisp.this.getY() + (random.nextFloat() * 2.0f - 1.0f) * WANDER_RANGE,
                    Wisp.this.getZ() + (random.nextFloat() * 2.0f - 1.0f) * WANDER_RANGE
                );
            }
        });
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.7, false));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.targetSelector.addGoal(6, new HurtByTargetGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return OrpimentSounds.WISP_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return OrpimentSounds.WISP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return OrpimentSounds.WISP_DEATH;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level() instanceof ServerLevel level && this.random.nextFloat() < 0.3f) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0, this.getZ(), 1, 1.0, 1.0, 1.0, 0.001);
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isInWater() || this.isInLava()) {
            super.travel(input);
        } else {
            this.travelFlying(input, (float) this.getAttributeValue(Attributes.FLYING_SPEED));
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setNoGravity(true);
    }

    // Steers at the target and strikes it on contact.
    private static class RamGoal extends Goal {

        private final Wisp wisp;

        RamGoal(Wisp wisp) {
            this.wisp = wisp;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.wisp.getTarget() != null && !this.wisp.getMoveControl().hasWanted();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.wisp.getTarget();
            return this.wisp.getMoveControl().hasWanted() && target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.steer();
        }

        @Override
        public void tick() {
            LivingEntity target = this.wisp.getTarget();
            if (target == null) {
                return;
            }
            if (this.wisp.getBoundingBox().intersects(target.getBoundingBox())) {
                if (this.wisp.level() instanceof ServerLevel level) {
                    this.wisp.doHurtTarget(level, target);
                }
            } else if (this.wisp.distanceToSqr(target) < 16.0) {
                this.steer();
            }
        }

        private void steer() {
            LivingEntity target = this.wisp.getTarget();
            if (target != null) {
                Vec3 eye = target.getEyePosition(1.0f);
                this.wisp.getMoveControl().setWantedPosition(eye.x, eye.y, eye.z, 0.4);
            }
        }
    }
}
