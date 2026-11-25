package info.mudbourn.mmsdungeons.mobs.kobolds.ai;

import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.ItemStack;

// Raises a held shield against a target that is shooting or within reach.
public class KoboldShieldGoal extends Goal {

    private static final double MIN_DISTANCE = 0.2;
    private static final double MAX_MELEE_DISTANCE = 5.2;

    private final AbstractKobold kobold;

    public KoboldShieldGoal(AbstractKobold kobold) {
        this.kobold = kobold;
    }

    @Override
    public boolean canUse() {
        return this.isHoldingShield() && this.shouldRaise();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.kobold.startUsingItem(this.kobold.handHolding(KoboldShieldGoal::isShield));
    }

    @Override
    public void stop() {
        super.stop();
        this.kobold.stopUsingItem();
    }

    private boolean shouldRaise() {
        LivingEntity target = this.kobold.getTarget();
        if (target == null || !target.isAlive() || this.kobold.getCooldown() > 0) {
            return false;
        }
        double distance = this.kobold.distanceTo(target);
        return distance >= MIN_DISTANCE && (target instanceof RangedAttackMob || distance <= MAX_MELEE_DISTANCE);
    }

    private boolean isHoldingShield() {
        return this.kobold.isHolding(KoboldShieldGoal::isShield);
    }

    private static boolean isShield(ItemStack stack) {
        return stack.has(DataComponents.BLOCKS_ATTACKS);
    }
}
