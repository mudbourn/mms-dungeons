package info.mudbourn.mmsdungeons.relations;

import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

// Picks the nearest creature the relations file makes this mob hostile to.
public class FeudTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    private static final int INTERVAL = 40;

    public FeudTargetGoal(Mob mob) {
        this(mob, INTERVAL, target -> false);
    }

    public FeudTargetGoal(Mob mob, int interval, Predicate<LivingEntity> extra) {
        super(
            mob,
            LivingEntity.class,
            interval,
            true,
            false,
            (target, level) -> MobRelations.shouldTarget(mob, target) || extra.test(target)
        );
    }

    @Override
    public boolean canUse() {
        return this.mob.getTarget() == null && MobRelations.mayFeud(this.mob) && super.canUse();
    }
}
