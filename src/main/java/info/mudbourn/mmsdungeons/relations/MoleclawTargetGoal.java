package info.mudbourn.mmsdungeons.relations;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

// Moleclaws go after a mob that crowds them or carries a light, and a player who carries a light, and let go when it goes out of sight.
public class MoleclawTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    private static final double CROWDING_SQR = 4.0;
    private static final int INTERVAL = 10;

    public MoleclawTargetGoal(Mob mob) {
        super(mob, LivingEntity.class, INTERVAL, true, false, (target, level) -> provokes(mob, target));
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity current = this.mob.getTarget();
        return current != null && provokes(this.mob, current) && super.canContinueToUse();
    }

    private static boolean provokes(Mob moleclaw, LivingEntity target) {
        if (!target.isAlive() || target.getType() == moleclaw.getType()) {
            return false;
        }
        if (moleclaw instanceof OwnableEntity owned && owned.getOwner() != null) {
            return false;
        }
        if (target instanceof Player player) {
            return !player.isCreative() && !player.isSpectator() && LightSources.carries(player);
        }
        return target instanceof Mob && (moleclaw.distanceToSqr(target) <= CROWDING_SQR || LightSources.carries(target));
    }
}
