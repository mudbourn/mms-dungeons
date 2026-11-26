package info.mudbourn.mmsdungeons.mobs.knightquest.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

// Walks around the nearest pumpkin, hopping now and then, then up to it.
public class MoveToPumpkinGoal extends Goal {

    private static final int SEARCH_RADIUS = 10;
    private static final int SEARCH_HEIGHT = 2;
    private static final double CIRCLE_RADIUS = 3.0;
    private static final double ANGLE_STEP = Math.PI / 8;
    private static final int CIRCLE_TICKS = 80;

    private final PathfinderMob mob;
    private final double speed;
    private @Nullable BlockPos pumpkin;
    private double angle;
    private int circled;

    public MoveToPumpkinGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        this.pumpkin = this.findPumpkin();
        return this.pumpkin != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.pumpkin != null && this.mob.level().getBlockState(this.pumpkin).is(Blocks.PUMPKIN);
    }

    @Override
    public void start() {
        this.circled = 0;
        this.circleStep();
    }

    @Override
    public void stop() {
        this.pumpkin = null;
    }

    @Override
    public void tick() {
        if (this.pumpkin == null) {
            return;
        }
        if (this.circled < CIRCLE_TICKS) {
            if (this.mob.getNavigation().isDone()) {
                this.angle += ANGLE_STEP;
                this.circleStep();
            }
            this.circled++;
            if (this.mob.getRandom().nextInt(20) == 0) {
                this.mob.getJumpControl().jump();
            }
        } else {
            this.mob.getNavigation().moveTo(this.pumpkin.getX() + 0.5, this.pumpkin.getY(), this.pumpkin.getZ() + 0.5, this.speed);
        }
    }

    private void circleStep() {
        if (this.pumpkin == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(this.pumpkin.getX(), this.pumpkin.getY(), this.pumpkin.getZ());
        this.mob.getNavigation().moveTo(
            this.pumpkin.getX() + CIRCLE_RADIUS * Math.cos(this.angle),
            this.pumpkin.getY(),
            this.pumpkin.getZ() + CIRCLE_RADIUS * Math.sin(this.angle),
            this.speed
        );
    }

    private @Nullable BlockPos findPumpkin() {
        Level level = this.mob.level();
        BlockPos origin = this.mob.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
            origin.offset(-SEARCH_RADIUS, -SEARCH_HEIGHT, -SEARCH_RADIUS),
            origin.offset(SEARCH_RADIUS, SEARCH_HEIGHT, SEARCH_RADIUS)
        )) {
            if (level.getBlockState(pos).is(Blocks.PUMPKIN)) {
                return pos.immutable();
            }
        }
        return null;
    }
}
