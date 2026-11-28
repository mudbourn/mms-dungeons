package info.mudbourn.mmsdungeons.mobs.factions.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

// A human walled in on every side, such as at the bottom of a creeper hole, jumps and places dirt under its feet
// until it can step out, the way a player pillars up.
final class FactionPillar extends Goal {

    // How often an idle human checks whether it is boxed in.
    private static final int CHECK_TICKS = 20;
    // Blocks it will pillar before giving up, so a deep shaft doesn't become a dirt tower.
    private static final int MAX_BLOCKS = 8;
    // Ticks without placing a block before it gives up, in case something keeps knocking it off the pillar.
    private static final int GIVE_UP_TICKS = 60;

    private final FactionMember mob;
    private int check;
    private int placed;
    private int idle;
    private int jumpFrom;

    FactionPillar(FactionMember mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (--this.check > 0) {
            return false;
        }
        this.check = CHECK_TICKS;
        return this.able() && this.mob.level() instanceof ServerLevel level && this.trapped(level, this.mob.blockPosition());
    }

    @Override
    public boolean canContinueToUse() {
        return this.able() && this.placed < MAX_BLOCKS && this.idle < GIVE_UP_TICKS
            && this.mob.level() instanceof ServerLevel level && this.trapped(level, this.mob.blockPosition());
    }

    @Override
    public void start() {
        this.placed = 0;
        this.idle = 0;
        this.jumpFrom = Integer.MIN_VALUE;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(this.mob.level() instanceof ServerLevel level)) {
            return;
        }
        this.idle++;
        BlockPos feet = this.mob.blockPosition();
        if (this.mob.onGround()) {
            this.jumpFrom = feet.getY();
            this.mob.getJumpControl().jump();
            return;
        }
        // Once the jump has lifted it a full block, fill the space it left with dirt.
        BlockPos below = feet.below();
        if (below.getY() == this.jumpFrom && this.mob.getY() >= this.jumpFrom + 1.0 && level.getBlockState(below).canBeReplaced()) {
            BlockState dirt = Blocks.DIRT.defaultBlockState();
            level.setBlock(below, dirt, Block.UPDATE_ALL);
            SoundType sound = dirt.getSoundType();
            level.playSound(null, below, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);
            this.mob.swing(InteractionHand.MAIN_HAND, this.mob.getMainHandItem().getAttackAnimation(), true);
            this.jumpFrom = Integer.MIN_VALUE;
            this.placed++;
            this.idle = 0;
        }
    }

    private boolean able() {
        return this.mob.isAlive() && !this.mob.downed() && !this.mob.flying() && !this.mob.isInWater() && !this.mob.isInLava()
            && !this.mob.isPassenger();
    }

    // Boxed in: no side it can walk or step out of, but room overhead to climb.
    private boolean trapped(ServerLevel level, BlockPos feet) {
        if (!this.free(level, feet.above(2)) || !this.free(level, feet.above(3))) {
            return false;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos next = feet.relative(side);
            boolean walk = this.free(level, next) && this.free(level, next.above());
            boolean step = this.free(level, next.above()) && this.free(level, next.above(2));
            if (walk || step) {
                return false;
            }
        }
        return true;
    }

    private boolean free(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
    }
}
