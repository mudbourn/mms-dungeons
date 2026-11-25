package info.mudbourn.mmsdungeons.mobs.kobolds.ai;

import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.item.Items;

// Throws a held trident while the main hand is empty.
public class KoboldTridentGoal extends RangedAttackGoal {

    private final AbstractKobold kobold;

    public KoboldTridentGoal(AbstractKobold kobold, double speed, int interval, float radius) {
        super(kobold, speed, interval, radius);
        this.kobold = kobold;
    }

    @Override
    public boolean canUse() {
        return super.canUse() && this.kobold.isHolding(Items.TRIDENT) && this.kobold.getMainHandItem().isEmpty();
    }

    @Override
    public void start() {
        super.start();
        this.kobold.setAggressive(true);
        this.kobold.startUsingItem(this.kobold.handHolding(stack -> stack.is(Items.TRIDENT)));
    }

    @Override
    public void stop() {
        super.stop();
        this.kobold.setAggressive(false);
        this.kobold.stopUsingItem();
    }
}
