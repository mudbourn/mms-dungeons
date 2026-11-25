package info.mudbourn.mmsdungeons.mobs.kobolds.ai;

import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldTags;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.KoboldWarrior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

// Picks fights with the mobs that raid kobold dens, and warriors with a longer list.
public class KoboldTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    private static final int INTERVAL = 12;

    public KoboldTargetGoal(AbstractKobold kobold) {
        super(
            kobold,
            LivingEntity.class,
            INTERVAL,
            true,
            false,
            (target, level) -> target.typeHolder().is(kobold instanceof KoboldWarrior ? KoboldTags.WARRIOR_TARGETS : KoboldTags.TARGETS)
        );
    }

    @Override
    protected double getFollowDistance() {
        return this.mob.getVehicle() != null ? super.getFollowDistance() * 3.0 : super.getFollowDistance();
    }
}
