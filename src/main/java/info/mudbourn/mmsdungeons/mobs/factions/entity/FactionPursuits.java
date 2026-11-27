package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

// What a human does with time on its hands: hunts monsters for their loot, and on the Overworld's surface by day
// an origin with a green thumb, Avian or Floran, keeps a small wheat plot instead.
final class FactionPursuits {

    private static final int CHECK_TICKS = 40;
    private static final double HUNT_RANGE_OVERWORLD = 20.0;
    private static final double HUNT_RANGE_ELSEWHERE = 48.0;
    private static final double FARM_DEFENSE_RANGE = 10.0;
    private static final int PLOT_RADIUS = 3;
    private static final int PLOT_SIZE = 9;
    private static final double WORK_REACH = 2.5;
    private static final int WORK_TICKS = 20;
    private static final int GIVE_UP_TICKS = 200;

    private FactionPursuits() {
    }

    // About half the members with a farming origin take to farming; the rest of their crew keeps watch.
    private static boolean farmer(FactionMember mob) {
        FactionOrigin origin = mob.origin();
        return (origin == FactionOrigin.AVIAN || origin == FactionOrigin.FLORAN) && (mob.getUUID().getLeastSignificantBits() & 1L) == 0L;
    }

    // On the Overworld, by day, under open sky: where farming comes first.
    private static boolean farmingTime(FactionMember mob) {
        Level level = mob.level();
        return level.dimension() == Level.OVERWORLD && level.isBrightOutside() && level.canSeeSky(mob.blockPosition());
    }

    // Picks the nearest monster as quarry: across the Overworld only close by, and only a threat to the plot while
    // farming; anywhere else hunting is what it is there for, so it looks far.
    static final class HuntMonstersGoal extends TargetGoal {

        private final FactionMember hunter;
        private int check;

        HuntMonstersGoal(FactionMember mob) {
            super(mob, true);
            this.hunter = mob;
        }

        @Override
        public boolean canUse() {
            if (--this.check > 0 || this.hunter.getTarget() != null || this.hunter.downed()) {
                return false;
            }
            this.check = CHECK_TICKS;
            double range = this.range();
            LivingEntity nearest = null;
            double best = range * range;
            for (LivingEntity other : this.hunter.level().getEntitiesOfClass(LivingEntity.class, this.hunter.getBoundingBox().inflate(range), this::quarry)) {
                double distance = this.hunter.distanceToSqr(other);
                if (distance < best && this.hunter.getSensing().hasLineOfSight(other)) {
                    nearest = other;
                    best = distance;
                }
            }
            this.targetMob = nearest;
            return nearest != null;
        }

        @Override
        public void start() {
            this.hunter.setTarget(this.targetMob);
            super.start();
        }

        @Override
        protected double getFollowDistance() {
            return this.range();
        }

        private double range() {
            if (this.hunter.level().dimension() != Level.OVERWORLD) {
                return HUNT_RANGE_ELSEWHERE;
            }
            return farmer(this.hunter) && farmingTime(this.hunter) ? FARM_DEFENSE_RANGE : HUNT_RANGE_OVERWORLD;
        }

        private boolean quarry(LivingEntity other) {
            return other instanceof Enemy
                && !(other instanceof FactionMember)
                && !(other instanceof OwnableEntity owned && owned.getOwner() != null)
                && other.isAlive()
                && !this.hunter.considersEntityAsAlly(other);
        }
    }

    // Keeps a 3 by 3 wheat plot where it first settled: tills the ground, sows it, and reaps and resows ripe wheat.
    static final class FarmGoal extends Goal {

        private final FactionMember mob;
        private @Nullable BlockPos plot;
        private @Nullable BlockPos job;
        private int check;
        private int work;
        private int walking;

        FarmGoal(FactionMember mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (--this.check > 0 || !this.free() || !(this.mob.level() instanceof ServerLevel level)
                || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
                return false;
            }
            this.check = CHECK_TICKS;
            if (this.plot == null || this.plot.distSqr(this.mob.blockPosition()) > 32 * 32) {
                this.plot = this.mob.blockPosition();
            }
            this.job = this.nextJob(level);
            return this.job != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.job != null && this.free() && this.walking < GIVE_UP_TICKS;
        }

        @Override
        public void start() {
            this.work = 0;
            this.walking = 0;
        }

        @Override
        public void stop() {
            this.job = null;
            this.mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            BlockPos job = this.job;
            if (job == null || !(this.mob.level() instanceof ServerLevel level)) {
                return;
            }
            this.mob.getLookControl().setLookAt(job.getX() + 0.5, job.getY() + 0.5, job.getZ() + 0.5);
            if (this.mob.distanceToSqr(job.getX() + 0.5, job.getY() + 0.5, job.getZ() + 0.5) > WORK_REACH * WORK_REACH) {
                this.walking++;
                if (this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(job.getX() + 0.5, job.getY() + 1.0, job.getZ() + 0.5, 1.0);
                }
                return;
            }
            this.mob.getNavigation().stop();
            if (++this.work < WORK_TICKS) {
                return;
            }
            this.mob.swing(InteractionHand.MAIN_HAND, this.mob.getMainHandItem().getAttackAnimation(), true);
            this.act(level, job);
            this.job = this.nextJob(level);
            this.work = 0;
            this.walking = 0;
        }

        private boolean free() {
            return farmer(this.mob) && farmingTime(this.mob) && this.mob.getTarget() == null && !this.mob.downed();
        }

        // Ripe wheat first, then bare farmland to sow, then ground to till while the plot is short of nine.
        private @Nullable BlockPos nextJob(ServerLevel level) {
            BlockPos plot = this.plot;
            if (plot == null) {
                return null;
            }
            BlockPos sow = null;
            BlockPos till = null;
            int fields = 0;
            for (BlockPos pos : BlockPos.betweenClosed(plot.offset(-PLOT_RADIUS, -2, -PLOT_RADIUS), plot.offset(PLOT_RADIUS, 1, PLOT_RADIUS))) {
                BlockState state = level.getBlockState(pos);
                BlockState above = level.getBlockState(pos.above());
                if (state.is(Blocks.FARMLAND)) {
                    fields++;
                    if (above.getBlock() instanceof CropBlock crop && crop.isMaxAge(above)) {
                        return pos.immutable();
                    }
                    if (above.isAir() && sow == null) {
                        sow = pos.immutable();
                    }
                } else if (till == null && above.isAir() && (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT))
                    && level.canSeeSky(pos.above())) {
                    till = pos.immutable();
                }
            }
            return sow != null ? sow : fields < PLOT_SIZE ? till : null;
        }

        private void act(ServerLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            BlockState above = level.getBlockState(pos.above());
            if (state.is(Blocks.FARMLAND) && above.getBlock() instanceof CropBlock crop && crop.isMaxAge(above)) {
                level.destroyBlock(pos.above(), true, this.mob);
                level.setBlock(pos.above(), Blocks.WHEAT.defaultBlockState(), Block.UPDATE_ALL);
            } else if (state.is(Blocks.FARMLAND) && above.isAir()) {
                level.setBlock(pos.above(), Blocks.WHEAT.defaultBlockState(), Block.UPDATE_ALL);
            } else if ((state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) && above.isAir()) {
                level.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
