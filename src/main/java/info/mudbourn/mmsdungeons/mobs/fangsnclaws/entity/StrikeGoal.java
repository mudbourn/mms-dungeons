package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

// Chases the target on foot and starts the mob's blow once it is in reach.
public class StrikeGoal extends Goal {

    private static final int REPATH_TICKS = 5;
    private static final float LOOK_TURN = 30.0f;

    private final StrikeMob mob;
    private final double speed;
    private int repath;

    public StrikeGoal(StrikeMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.mob.setAggressive(true);
        this.repath = 0;
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(target, LOOK_TURN, LOOK_TURN);
        if (this.mob.isBusy()) {
            this.mob.getNavigation().stop();
            return;
        }
        if (--this.repath <= 0) {
            this.repath = REPATH_TICKS;
            this.mob.getNavigation().moveTo(target, this.speed);
        }
        boolean inReach = this.mob.distanceTo(target) <= this.mob.reach() && this.mob.hasLineOfSight(target);
        if (inReach && this.mob.isReadyToStrike()) {
            this.mob.startAttack(target);
        }
    }
}
