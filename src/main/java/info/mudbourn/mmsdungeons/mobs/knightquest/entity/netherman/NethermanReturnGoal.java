package info.mudbourn.mmsdungeons.mobs.knightquest.entity.netherman;

import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Netherman;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

// With no target, teleports next to the nearest player.
public class NethermanReturnGoal extends Goal {

    private static final int PERIOD = 100;
    private static final int OPENING_TICKS = 100;
    private static final double RANGE = 64.0;
    private static final double MIN_RANGE = 3.0;
    private static final double MAX_RANGE = 10.0;

    private final Netherman netherman;
    private int idleTime;

    public NethermanReturnGoal(Netherman netherman) {
        this.netherman = netherman;
    }

    @Override
    public boolean canUse() {
        return this.netherman.getTarget() == null
            && this.netherman.isSettled()
            && (this.netherman.getPhase() != 1 || this.netherman.tickCount > OPENING_TICKS);
    }

    @Override
    public void start() {
        this.idleTime = PERIOD;
    }

    @Override
    public void stop() {
        this.idleTime = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.idleTime > 0) {
            this.idleTime--;
        }
        if (this.idleTime == 0) {
            this.idleTime = PERIOD;
            this.teleportNearPlayer();
        }
    }

    private void teleportNearPlayer() {
        if (!(this.netherman.level() instanceof ServerLevel level)) {
            return;
        }
        Player nearest = null;
        for (Player player : level.players()) {
            if (!player.isCreative() && !player.isSpectator() && player.distanceToSqr(this.netherman) <= RANGE * RANGE && (nearest == null || player.distanceToSqr(this.netherman) < nearest.distanceToSqr(this.netherman))) {
                nearest = player;
            }
        }
        if (nearest == null) {
            return;
        }
        Vec3 spot = NethermanSpots.ringSpot(level, this.netherman, nearest, MIN_RANGE, MAX_RANGE);
        if (spot != null) {
            NethermanSpots.teleport(this.netherman, spot);
            this.netherman.getNavigation().stop();
        }
    }
}
