package info.mudbourn.mmsdungeons.mobs.knightquest.entity.netherman;

import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Netherman;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

// Pulls every player nearby to a random spot beside the boss now and then.
public class NethermanPullGoal extends Goal {

    private static final int PERIOD = 400;
    private static final double RANGE = 50.0;
    private static final double SPREAD = 25.0;
    private static final double MIN_HEALTH = 0.1;

    private final Netherman netherman;
    private int chargeTime;

    public NethermanPullGoal(Netherman netherman) {
        this.netherman = netherman;
    }

    @Override
    public boolean canUse() {
        return this.netherman.getTarget() != null && this.netherman.getHealth() > this.netherman.getMaxHealth() * MIN_HEALTH && this.netherman.isSettled();
    }

    @Override
    public void start() {
        this.chargeTime = PERIOD;
    }

    @Override
    public void stop() {
        this.chargeTime = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.chargeTime > 0) {
            this.chargeTime--;
        }
        if (this.chargeTime == 0 && this.netherman.level() instanceof ServerLevel level) {
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, this.netherman.getBoundingBox().inflate(RANGE), player -> !player.isSpectator())) {
                Vec3 spot = NethermanSpots.standingSpot(
                    level,
                    player,
                    this.netherman.getX() + (this.netherman.getRandom().nextDouble() - 0.5) * SPREAD,
                    this.netherman.getY() + 3.0,
                    this.netherman.getZ() + (this.netherman.getRandom().nextDouble() - 0.5) * SPREAD,
                    10
                );
                if (spot != null) {
                    NethermanSpots.teleport(player, spot);
                }
            }
            this.chargeTime = PERIOD;
        }
    }
}
