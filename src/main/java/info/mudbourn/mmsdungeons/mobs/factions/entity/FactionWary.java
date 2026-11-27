package info.mudbourn.mmsdungeons.mobs.factions.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A human that fears a player, and has not made peace with them, keeps its distance and stares at them: it backs off when they come close, runs when they get right up to it or chase it, and watches them while they stay in sight.
final class FactionWary extends Goal {

    private static final double WATCH_RANGE = 16.0;
    private static final double KEEP_AWAY = 7.0;
    private static final double TOO_CLOSE = 3.5;
    private static final int RETREAT_DISTANCE = 10;
    private static final int FLEE_DISTANCE = 16;
    private static final int RETREAT_HEIGHT = 4;
    private static final int REPLAN_TICKS = 20;
    private static final double CHASE_RANGE = 12.0;
    private static final int CHASE_SAMPLE_TICKS = 10;
    // Blocks the player closes in one sample: a little under a player's walk.
    private static final double CHASE_CLOSING = 1.5;
    private static final int CHASE_HOLD_TICKS = 60;

    private final FactionMember mob;
    private @Nullable Player feared;
    private @Nullable Vec3 lastSeenAt;
    private int sample;
    private int chased;
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
        this.sample = 0;
        this.chased = 0;
        this.lastSeenAt = null;
    }

    @Override
    public void stop() {
        this.feared = null;
        this.lastSeenAt = null;
        this.chased = 0;
        this.mob.setFleeing(false);
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
        boolean chased = this.tickChase(feared, distance);
        boolean fleeing = this.mob.fleeing();
        this.mob.setFleeing(chased);
        if (chased || distance < TOO_CLOSE * TOO_CLOSE) {
            this.run(true);
        } else if (distance >= KEEP_AWAY * KEEP_AWAY) {
            this.run(false);
        }
        if (!this.running) {
            this.mob.getLookControl().setLookAt(feared, 30.0f, 30.0f);
        }
        if (!chased && distance >= KEEP_AWAY * KEEP_AWAY) {
            if (!this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().stop();
            }
            return;
        }
        if (chased == fleeing && --this.replan > 0 && !this.mob.getNavigation().isDone()) {
            return;
        }
        this.replan = REPLAN_TICKS;
        Vec3 away = DefaultRandomPos.getPosAway(this.mob, chased ? FLEE_DISTANCE : RETREAT_DISTANCE, RETREAT_HEIGHT, feared.position());
        if (away != null) {
            this.mob.getNavigation().moveTo(away.x, away.y, away.z, 1.0);
        }
    }

    // Whether the player is chasing it: closing on it at walking pace or faster, remembered for a few seconds after.
    private boolean tickChase(Player feared, double distance) {
        if (this.chased > 0) {
            this.chased--;
        }
        if (--this.sample <= 0) {
            this.sample = CHASE_SAMPLE_TICKS;
            Vec3 now = feared.position();
            Vec3 then = this.lastSeenAt;
            this.lastSeenAt = now;
            if (then != null && distance <= CHASE_RANGE * CHASE_RANGE) {
                Vec3 toward = this.mob.position().subtract(then).multiply(1.0, 0.0, 1.0).normalize();
                if (now.subtract(then).dot(toward) >= CHASE_CLOSING) {
                    this.chased = CHASE_HOLD_TICKS;
                }
            }
        }
        return this.chased > 0;
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
