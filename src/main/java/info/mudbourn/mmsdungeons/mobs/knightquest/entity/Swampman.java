package info.mudbourn.mmsdungeons.mobs.knightquest.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// A swamp axeman that, below half health, turns darker and throws axes from range.
public class Swampman extends Monster implements RangedAttackMob {

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(Swampman.class, EntityDataSerializers.INT);

    private static final int AIR_SUPPLY = 1000;
    private static final float AXE_SPEED = 1.3f;

    public Swampman(EntityType<? extends Swampman> type, Level level) {
        super(type, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 50.0)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.ATTACK_SPEED, 0.5)
            .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AxeThrowGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.6, true));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        SwampmanAxe axe = new SwampmanAxe(level, this);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.34) - axe.getY();
        double dz = target.getZ() - this.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        float uncertainty = 14 - level.getDifficulty().getId() * 4;
        Projectile.spawnProjectileUsingShoot(axe, level, ItemStack.EMPTY, dx, dy + flat * 0.2, dz, AXE_SPEED, uncertainty);
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.0f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.getPhase() == 1 && this.getHealth() < this.getMaxHealth() * 0.5) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0f, 1.0f);
            this.entityData.set(PHASE, 2);
        }
    }

    @Override
    public int getMaxAirSupply() {
        return AIR_SUPPLY;
    }

    @Override
    protected int increaseAirSupply(int current) {
        return this.getMaxAirSupply();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Phase", this.getPhase());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(PHASE, input.getIntOr("Phase", 1));
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.AXOLOTL_SWIM;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAMEL_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CAMEL_HURT;
    }

    // Throws axes from range once the Swampman has turned dark.
    private static class AxeThrowGoal extends RangedBowAttackGoal<Swampman> {

        private final Swampman swampman;

        AxeThrowGoal(Swampman swampman) {
            super(swampman, 0.7, 10, 15.0f);
            this.swampman = swampman;
        }

        @Override
        public boolean canUse() {
            return this.swampman.getPhase() != 1 && super.canUse();
        }
    }
}
