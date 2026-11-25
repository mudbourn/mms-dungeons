package info.mudbourn.mmsdungeons.mobs.kobolds.ai;

import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

// Melee only while holding something in the main hand.
public class KoboldMeleeGoal extends MeleeAttackGoal {

    public KoboldMeleeGoal(AbstractKobold kobold, double speed, boolean followEvenIfUnseen) {
        super(kobold, speed, followEvenIfUnseen);
    }

    @Override
    public boolean canUse() {
        return super.canUse() && this.hasWeapon();
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && this.hasWeapon();
    }

    private boolean hasWeapon() {
        return !this.mob.getMainHandItem().isEmpty();
    }
}
