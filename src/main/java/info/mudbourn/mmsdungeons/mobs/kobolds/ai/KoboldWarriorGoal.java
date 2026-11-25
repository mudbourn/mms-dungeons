package info.mudbourn.mmsdungeons.mobs.kobolds.ai;

import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldMobs;
import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldWork;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Spends half a minute with a held axe, then drops its old gear and becomes a warrior.
public class KoboldWarriorGoal extends Goal {

    private static final int TRAINING_TICKS = 600;

    private final AbstractKobold kobold;

    public KoboldWarriorGoal(AbstractKobold kobold) {
        this.kobold = kobold;
    }

    @Override
    public boolean canUse() {
        return this.kobold.isHolding(stack -> stack.is(ItemTags.AXES));
    }

    @Override
    public void start() {
        this.kobold.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, TRAINING_TICKS, 10, false, false));
        KoboldWork.queue(TRAINING_TICKS, this::becomeWarrior);
    }

    private void becomeWarrior() {
        if (!this.kobold.isAlive() || !(this.kobold.level() instanceof ServerLevel level)) {
            return;
        }
        InteractionHand hand = this.kobold.handHolding(stack -> stack.is(ItemTags.AXES));
        ItemStack axe = this.kobold.getItemInHand(hand);
        ItemStack other = hand == InteractionHand.MAIN_HAND ? this.kobold.getOffhandItem() : this.kobold.getMainHandItem();
        this.drop(level, other);
        if (hand == InteractionHand.MAIN_HAND) {
            this.drop(level, this.kobold.getPrimary());
        }
        this.kobold.setItemSlot(EquipmentSlot.MAINHAND, axe);
        this.kobold.setDropChance(EquipmentSlot.MAINHAND, 1.0f);
        this.kobold.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        this.kobold.updateItemData();
        this.kobold.convertTo(KoboldMobs.KOBOLD_WARRIOR, ConversionParams.single(this.kobold, true, true), warrior -> { });
    }

    private void drop(ServerLevel level, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, this.kobold.getX(), this.kobold.getY(), this.kobold.getZ(), stack);
        item.setPickUpDelay(10);
        level.addFreshEntity(item);
    }
}
