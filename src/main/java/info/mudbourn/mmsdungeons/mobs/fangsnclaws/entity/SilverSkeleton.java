package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsGear;
import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

// An armored skeleton that cuts with a sword in each hand.
public class SilverSkeleton extends Monster {

    private static final String[] SWINGS = {"attack_right", "attack_left", "attack_double"};
    private static final int HIT_TICK = 8;
    private static final int ATTACK_TICKS = 18;
    private static final double HIT_REACH = 3.5;

    private Entity pendingTarget;
    private int attackTick;

    public SilverSkeleton(EntityType<? extends SilverSkeleton> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 2.5)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(FangsNClawsGear.SILVER_SWORD));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(FangsNClawsGear.SILVER_SWORD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
        return data;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        GeoTriggers.trigger(this, SWINGS[this.random.nextInt(SWINGS.length)]);
        this.pendingTarget = target;
        this.attackTick = 1;
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level) || this.attackTick <= 0) {
            return;
        }
        this.attackTick++;
        if (this.attackTick == HIT_TICK) {
            if (this.pendingTarget != null
                && this.pendingTarget.isAlive()
                && this.distanceTo(this.pendingTarget) <= HIT_REACH
                && this.hasLineOfSight(this.pendingTarget)) {
                super.doHurtTarget(level, this.pendingTarget);
            }
            this.pendingTarget = null;
        }
        if (this.attackTick >= ATTACK_TICKS) {
            this.attackTick = 0;
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }
}
