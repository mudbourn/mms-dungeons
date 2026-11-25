package info.mudbourn.mmsdungeons.mobs.kobolds.ai;

import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Drinks a held potion, then puts the weapon it set aside back in hand.
public class KoboldPotionGoal extends Goal {

    private final AbstractKobold kobold;

    public KoboldPotionGoal(AbstractKobold kobold) {
        this.kobold = kobold;
    }

    @Override
    public boolean canUse() {
        return this.isHoldingPotion();
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.kobold.isUsingItem() && this.isHoldingPotion()) {
            InteractionHand hand = this.kobold.handHolding(stack -> stack.is(Items.POTION));
            this.kobold.removeEffect(MobEffects.SLOWNESS);
            ItemStack restored = hand == InteractionHand.MAIN_HAND ? this.kobold.getPrimary() : this.kobold.getSecondary();
            ItemStack other = hand == InteractionHand.MAIN_HAND ? this.kobold.getOffhandItem() : this.kobold.getSecondary();
            this.kobold.setItemInHand(hand, other.is(Items.TRIDENT) ? ItemStack.EMPTY : restored);
        }
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        InteractionHand hand = this.kobold.handHolding(stack -> stack.is(Items.POTION));
        int duration = this.kobold.getItemInHand(hand).getUseDuration(this.kobold);
        this.kobold.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 10, false, false));
        this.kobold.startUsingItem(hand);
    }

    private boolean isHoldingPotion() {
        return this.kobold.isHolding(stack -> stack.is(Items.POTION));
    }
}
