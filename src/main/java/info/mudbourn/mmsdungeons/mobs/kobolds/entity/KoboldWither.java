package info.mudbourn.mmsdungeons.mobs.kobolds.entity;

import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldArms;
import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldSounds;
import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldTags;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import org.jspecify.annotations.Nullable;

public class KoboldWither extends AbstractKoboldSkeleton {

    private static final int WITHER_TICKS = 200;
    private static final int SMITE_LEVEL = 5;

    public KoboldWither(EntityType<KoboldWither> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.LAVA, 8.0f);
        this.getNavigation().setCanOpenDoors(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new OpenDoorGoal(this, false));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(1, new RangedCrossbowAttackGoal<>(this, 1.0, 12.0f));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, LivingEntity.class, 8.0f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isAlive() && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) {
            this.playSound(this.getAmbientSound(), 1.0f, this.getVoicePitch());
            if (!this.level().isClientSide()) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_TICKS), this);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        this.setCanPickUpLoot(true);
        if (reason != EntitySpawnReason.NATURAL) {
            this.setPersistenceRequired();
        }
        return data;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, KoboldArms.melee(KoboldArms.Family.SWORD, KoboldArms.NETHERITE, random));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    @Override
    protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor level, RandomSource random, DifficultyInstance difficulty) {
        Holder<Enchantment> smite = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SMITE);
        this.getMainHandItem().enchant(smite, SMITE_LEVEL);
    }

    @Override
    protected boolean canReplaceCurrentItem(ItemStack drop, ItemStack held, EquipmentSlot slot) {
        return drop.is(KoboldTags.ARMOR) && (held.isEmpty() || this.canReplaceEqualItem(drop, held));
    }

    @Override
    public boolean canReplaceEqualItem(ItemStack drop, ItemStack held) {
        if (!drop.isEnchanted()) {
            return super.canReplaceEqualItem(drop, held);
        }
        if (held.isEnchanted() && AbstractKobold.enchantScore(drop) > AbstractKobold.enchantScore(held)) {
            return true;
        }
        return !held.isEnchanted();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_TICKS), this);
        }
        return super.doHurtTarget(level, target);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return KoboldSounds.WITHERBOLD_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return KoboldSounds.WITHERBOLD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KoboldSounds.WITHERBOLD_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.WITHER_SKELETON_STEP;
    }

    @Override
    protected AbstractArrow getArrow(ItemStack arrow, float power, @Nullable ItemStack weapon) {
        AbstractArrow projectile = super.getArrow(arrow, power, weapon);
        projectile.igniteForSeconds(100.0f);
        return projectile;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.WITHER) && super.canBeAffected(effect);
    }
}
