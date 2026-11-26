package info.mudbourn.mmsdungeons.mobs.knightquest.entity;

import info.mudbourn.mmsdungeons.mobs.knightquest.KnightQuestItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// A small pumpkin spirit that a Great Essence tames, then sits, fights and wears gear for its owner.
public class Samhain extends TamableAnimal {

    private static final EntityDataAccessor<Integer> SIT_VARIATION = SynchedEntityData.defineId(Samhain.class, EntityDataSerializers.INT);

    private static final double TAMED_HEALTH = 20.0;
    private static final float GREAT_ESSENCE_HEAL = 16.0f;
    private static final float SMALL_ESSENCE_HEAL = 4.0f;
    private static final int SIT_VARIATIONS = 3;

    public Samhain(EntityType<? extends Samhain> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 8.0)
            .add(Attributes.ATTACK_DAMAGE, 0.5)
            .add(Attributes.ATTACK_SPEED, 2.0)
            .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SIT_VARIATION, 0);
    }

    // Which sitting pose to hold, from 0 to 2.
    public int getSitVariation() {
        return this.entityData.get(SIT_VARIATION);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.6, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 0.6, 6.0f, 2.0f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new MoveToPumpkinGoal(this, 0.68));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.isTame() && stack.is(KnightQuestItems.GREAT_ESSENCE)) {
            if (!this.level().isClientSide()) {
                stack.consume(1, player);
                this.tame(player);
                this.navigation.stop();
                this.setTarget(null);
                this.level().broadcastEntityEvent(this, (byte) 7);
                this.sit(true);
            }
            return InteractionResult.SUCCESS;
        }
        if (this.isTame() && hand == InteractionHand.MAIN_HAND && this.isOwnedBy(player)) {
            if (this.level() instanceof ServerLevel level) {
                this.ownerInteract(level, player, stack);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void ownerInteract(ServerLevel level, Player player, ItemStack stack) {
        float heal = stack.is(KnightQuestItems.GREAT_ESSENCE) ? GREAT_ESSENCE_HEAL : stack.is(KnightQuestItems.SMALL_ESSENCE) ? SMALL_ESSENCE_HEAL : 0.0f;
        if (heal > 0.0f && this.getHealth() < this.getMaxHealth()) {
            this.heal(heal);
            stack.consume(1, player);
        } else if (player.isShiftKeyDown()) {
            this.returnGear(level, player);
        } else {
            this.sit(!this.isOrderedToSit());
        }
    }

    private void sit(boolean sitting) {
        this.setOrderedToSit(sitting);
        this.setInSittingPose(sitting);
        this.entityData.set(SIT_VARIATION, this.random.nextInt(SIT_VARIATIONS));
    }

    private void returnGear(ServerLevel level, Player player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor() || slot == EquipmentSlot.MAINHAND) {
                ItemStack worn = this.getItemBySlot(slot);
                if (!worn.isEmpty()) {
                    if (!player.getInventory().add(worn)) {
                        player.spawnAtLocation(level, worn);
                    }
                    this.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity entity) {
        if (!this.isTame()) {
            return;
        }
        ItemStack stack = entity.getItem();
        boolean weapon = stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES);
        EquipmentSlot slot = weapon ? EquipmentSlot.MAINHAND : this.getEquipmentSlotForItem(stack);
        if (!weapon && !slot.isArmor()) {
            return;
        }
        if (this.getItemBySlot(slot).isEmpty()) {
            this.onItemPickup(entity);
            this.setItemSlot(slot, stack.split(1));
            this.take(entity, 1);
            if (stack.isEmpty()) {
                entity.discard();
            }
        }
    }

    @Override
    protected void applyTamingSideEffects() {
        if (this.isTame()) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(TAMED_HEALTH);
            this.setHealth((float) TAMED_HEALTH);
        } else {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8.0);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("SitVariation", this.getSitVariation());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(SIT_VARIATION, input.getIntOr("SitVariation", 0));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.AXOLOTL_SWIM;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WOLF_STEP.value(), 0.15f, 1.0f);
    }
}
