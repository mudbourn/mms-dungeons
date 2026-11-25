package info.mudbourn.mmsdungeons.mobs.kobolds.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldCrossbowGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldBowGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldArms;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

public abstract class AbstractKoboldSkeleton extends AbstractSkeleton implements CrossbowAttackMob {

    private static final EntityDataAccessor<Boolean> DATA_CHARGING = SynchedEntityData.defineId(
        AbstractKoboldSkeleton.class,
        EntityDataSerializers.BOOLEAN
    );

    private KoboldCrossbowGoal<AbstractKoboldSkeleton> crossbowGoal;
    private KoboldBowGoal<AbstractKoboldSkeleton> bowGoal;

    protected AbstractKoboldSkeleton(EntityType<? extends AbstractKoboldSkeleton> type, Level level) {
        super(type, level);
    }

    // Vanilla skeleton AI only shoots the vanilla bow and treats anything else as a melee weapon. Skelebolds shoot
    // any crossbow or bow they hold, longbows and chain crossbows included.
    @Override
    public void reassessWeaponGoal() {
        super.reassessWeaponGoal();
        if (this.level() == null || this.level().isClientSide()) {
            return;
        }
        if (this.crossbowGoal == null) {
            this.crossbowGoal = new KoboldCrossbowGoal<>(this, 1.0, 12.0f);
            this.bowGoal = new KoboldBowGoal<>(this, 1.0, 20, 15.0f);
        }
        this.goalSelector.removeGoal(this.crossbowGoal);
        this.goalSelector.removeGoal(this.bowGoal);
        Item held = this.getMainHandItem().getItem();
        if (held instanceof CrossbowItem || held instanceof BowItem) {
            this.goalSelector.removeAllGoals(goal -> goal instanceof MeleeAttackGoal || goal instanceof RangedBowAttackGoal);
            this.goalSelector.addGoal(4, held instanceof CrossbowItem ? this.crossbowGoal : this.bowGoal);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (this.getMainHandItem().getItem() instanceof CrossbowItem) {
            this.performCrossbowAttack(this, 6.0f);
        } else {
            super.performRangedAttack(target, power);
        }
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(DATA_CHARGING, charging);
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_CHARGING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGING, false);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        this.populateDefaultEquipmentEnchantments(level, level.getRandom(), difficulty);
        return data;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, KoboldArms.ranged(false, random));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.15f);
    }

    // Undead kobolds have no baby form, so a spawn egg still makes an adult.
    @Override
    public void setBaby(boolean baby) {
        super.setBaby(false);
    }

    @Override
    public boolean isBaby() {
        return false;
    }
}
