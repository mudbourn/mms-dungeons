package info.mudbourn.mmsdungeons.mobs.kobolds.ai;

import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.phys.AABB;

// Hits back at what hurt it and calls every idle kobold nearby to the same target.
public class KoboldRevengeGoal extends HurtByTargetGoal {

    private static final double CALL_RANGE = 32.0;

    public KoboldRevengeGoal(AbstractKobold kobold) {
        super(kobold);
        this.setAlertOthers(AbstractKobold.class);
    }

    @Override
    public void start() {
        super.start();
        if (this.mob.level().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        AABB area = this.mob.getBoundingBox().inflate(CALL_RANGE);
        for (AbstractKobold kobold : this.mob.level().getEntitiesOfClass(AbstractKobold.class, area, KoboldRevengeGoal::isIdle)) {
            kobold.setTarget(this.mob.getTarget());
        }
    }

    private static boolean isIdle(AbstractKobold kobold) {
        LivingEntity target = kobold.getTarget();
        return target == null || !target.isAlive();
    }
}
