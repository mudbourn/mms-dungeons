package info.mudbourn.mmsdungeons.mobs.orpiment.entity;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentEffects;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentFluids;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentItems;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentMobs;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A cube of living tar that splits on death and, when tiny, grows by sitting in tar.
public class TarCube extends AbstractCubeMob implements Enemy {

    private static final int TICKS_TO_GROW = 100;
    private static final int SEARCH_RANGE = 16;
    private static final int SEARCH_HEIGHT = 4;

    private int ticksInTar;

    public TarCube(EntityType<? extends TarCube> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.MAX_HEALTH, 3.0)
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 16.0)
            .add(Attributes.ARMOR, 0.0);
    }

    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new StayInTarGoal(this));
        this.goalSelector.addGoal(1, new SeekTarGoal(this));
        this.goalSelector.addGoal(3, new CubeMobAttackGoal(this));
    }

    @Override
    protected void addTargetingGoals() {
        this.targetSelector.addGoal(
            1,
            new NearestAttackableTargetGoal<>(
                this,
                Player.class,
                10,
                true,
                false,
                (target, level) -> this.getSize() != 1 && Math.abs(target.getY() - this.getY()) <= 4.0
            )
        );
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true) {
            @Override
            public boolean canUse() {
                return TarCube.this.getSize() != 1 && super.canUse();
            }
        });
    }

    @Override
    public void setSize(int size, boolean updateHealth) {
        super.setSize(size, updateHealth);
        int actual = this.getSize();
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(actual);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2);
        this.xpReward = actual;
    }

    @Override
    protected void setCubeMobHealth(int size) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(size * size + 2.0);
    }

    @Override
    protected boolean canBeABaby() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.isTiny()) {
            this.ticksInTar = this.isInTar() ? this.ticksInTar + 1 : 0;
            if (this.ticksInTar >= TICKS_TO_GROW) {
                this.growToMedium();
            }
        } else if (this.isInTar()) {
            Vec3 motion = this.getDeltaMovement();
            if (motion.y < 0.1) {
                this.setDeltaMovement(motion.x, motion.y + 0.05, motion.z);
            }
            if (this.onGround() && this.random.nextInt(20) == 0) {
                this.getJumpControl().jump();
            }
        }
    }

    private void growToMedium() {
        if (this.isDeadOrDying()) {
            return;
        }
        this.convertTo(OrpimentMobs.TAR_CUBE, ConversionParams.single(this, true, false), EntitySpawnReason.CONVERSION, cube -> {
            cube.setSize(2, true);
            cube.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        });
    }

    private boolean isInTar() {
        Fluid fluid = this.level().getFluidState(this.blockPosition()).getType();
        return fluid == OrpimentFluids.TAR || fluid == OrpimentFluids.FLOWING_TAR;
    }

    @Override
    protected void dealDamage(LivingEntity target) {
        super.dealDamage(target);
        if (target instanceof Player) {
            target.addEffect(new MobEffectInstance(OrpimentEffects.TARRED, 60, 0, false, true));
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.getEffect().equals(OrpimentEffects.TARRED) && super.canBeAffected(effect);
    }

    @Override
    protected float getSoundVolume() {
        return 0.2f * this.getSize();
    }

    @Override
    protected @Nullable ParticleOptions getParticleType() {
        return new ItemParticleOption(ParticleTypes.ITEM, OrpimentItems.TAR_CLUMP);
    }

    @Override
    protected SoundEvent getJumpSound() {
        return OrpimentSounds.TAR_CUBE_JUMP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return OrpimentSounds.TAR_CUBE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return OrpimentSounds.TAR_CUBE_DEATH;
    }

    @Override
    protected SoundEvent getSquishSound() {
        return OrpimentSounds.TAR_CUBE_JUMP;
    }

    // Keeps a tiny cube still while it sits in tar.
    private static class StayInTarGoal extends Goal {

        private final TarCube cube;

        StayInTarGoal(TarCube cube) {
            this.cube = cube;
            this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.cube.getSize() == 1 && !this.cube.isNoAi() && this.cube.isInTar();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            this.cube.setDeltaMovement(0.0, this.cube.getDeltaMovement().y, 0.0);
        }
    }

    // Sends a tiny cube toward the nearest tar.
    private static class SeekTarGoal extends Goal {

        private final TarCube cube;
        private @Nullable BlockPos tar;

        SeekTarGoal(TarCube cube) {
            this.cube = cube;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.cube.getSize() != 1 || this.cube.isNoAi() || this.cube.isInTar()) {
                return false;
            }
            this.tar = this.findNearestTar();
            return this.tar != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.cube.getSize() != 1 || this.cube.isInTar()) {
                return false;
            }
            if (this.tar == null || !isTar(this.cube.level().getFluidState(this.tar).getType())) {
                this.tar = this.findNearestTar();
            }
            return this.tar != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.tar != null && this.cube.getMoveControl() instanceof CubeMobMoveControl<?> control) {
                double dx = this.tar.getX() + 0.5 - this.cube.getX();
                double dz = this.tar.getZ() + 0.5 - this.cube.getZ();
                control.setDirection((float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f, true);
                control.setWantedMovement(1.0);
            }
        }

        @Override
        public void stop() {
            this.tar = null;
        }

        private static boolean isTar(Fluid fluid) {
            return fluid == OrpimentFluids.TAR || fluid == OrpimentFluids.FLOWING_TAR;
        }

        private @Nullable BlockPos findNearestTar() {
            BlockPos origin = this.cube.blockPosition();
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-SEARCH_RANGE, -SEARCH_HEIGHT, -SEARCH_RANGE),
                origin.offset(SEARCH_RANGE, SEARCH_HEIGHT, SEARCH_RANGE)
            )) {
                if (isTar(this.cube.level().getFluidState(pos).getType()) && pos.distSqr(origin) < bestDistance) {
                    bestDistance = pos.distSqr(origin);
                    best = pos.immutable();
                }
            }
            return best;
        }
    }
}
