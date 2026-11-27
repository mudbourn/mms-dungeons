package info.mudbourn.mmsdungeons.mobs.factions.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A human that fears a player, and has not made peace with them, keeps its distance and stares at them: it backs off when they come close, runs when they get right up to it, and watches them while they stay in sight.
final class FactionWary extends Goal {

    private static final double WATCH_RANGE = 16.0;
    private static final double KEEP_AWAY = 7.0;
    private static final double TOO_CLOSE = 3.5;
    private static final int RETREAT_DISTANCE = 10;
    private static final int RETREAT_HEIGHT = 4;
    private static final int REPLAN_TICKS = 20;

    private final FactionMember mob;
    private @Nullable Player feared;
    private int replan;
    private boolean running;

    FactionWary(FactionMember mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.mob.downed() || this.mob.getTarget() != null) {
            return false;
        }
        this.feared = this.nearestFeared();
        return this.feared != null;
    }

    @Override
    public boolean canContinueToUse() {
        Player feared = this.feared;
        return feared != null
            && !this.mob.downed()
            && this.mob.getTarget() == null
            && this.fears(feared)
            && this.mob.distanceToSqr(feared) <= WATCH_RANGE * WATCH_RANGE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.replan = 0;
    }

    @Override
    public void stop() {
        this.feared = null;
        this.run(false);
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        Player feared = this.feared;
        if (feared == null) {
            return;
        }
        double distance = this.mob.distanceToSqr(feared);
        if (distance < TOO_CLOSE * TOO_CLOSE) {
            this.run(true);
        } else if (distance >= KEEP_AWAY * KEEP_AWAY) {
            this.run(false);
        }
        // A runner looks where it is going, as a sprinting player must; otherwise it keeps its eyes on the player.
        if (!this.running) {
            this.mob.getLookControl().setLookAt(feared, 30.0f, 30.0f);
        }
        if (distance >= KEEP_AWAY * KEEP_AWAY) {
            if (!this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().stop();
            }
            return;
        }
        if (--this.replan > 0 && !this.mob.getNavigation().isDone()) {
            return;
        }
        this.replan = REPLAN_TICKS;
        Vec3 away = DefaultRandomPos.getPosAway(this.mob, RETREAT_DISTANCE, RETREAT_HEIGHT, feared.position());
        if (away != null) {
            this.mob.getNavigation().moveTo(away.x, away.y, away.z, 1.0);
        }
    }

    // Breaks into a run, or slows back to a walk once it has put some ground between them.
    private void run(boolean on) {
        if (this.running != on) {
            this.running = on;
            this.mob.setSprinting(on);
        }
    }

    private @Nullable Player nearestFeared() {
        Player nearest = null;
        double best = WATCH_RANGE * WATCH_RANGE;
        for (Player player : this.mob.level().players()) {
            double distance = this.mob.distanceToSqr(player);
            if (distance <= best && this.fears(player) && this.mob.hasLineOfSight(player)) {
                nearest = player;
                best = distance;
            }
        }
        return nearest;
    }

    // Cowed by the player's kit, but not at peace with them: a friend has nothing to fear.
    private boolean fears(Player player) {
        return player.isAlive()
            && !player.isSpectator()
            && !player.isCreative()
            && this.mob.cowedBy(player)
            && !this.mob.peace().friendlyTo(player, this.mob.level().getGameTime());
    }
}
