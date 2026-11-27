package info.mudbourn.mmsdungeons.mobs.factions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Ground pathing that cuts the corners a path does not need: it heads straight for the farthest of the next few path points it can walk to in a line over solid, safe floor, so open ground is crossed at any angle instead of in block-grid steps. Near each point it starts aiming partly at the one after, so the bends it keeps are rounded off rather than turned on the spot.
final class FactionNavigation extends GroundPathNavigation {

    private static final int LOOKAHEAD_NODES = 8;
    private static final int RECHECK_TICKS = 5;
    private static final double SAMPLE_STEP = 0.5;
    private static final double BLEND_RANGE = 2.0;
    private static final double MAX_BLEND = 0.5;

    private final FactionMember member;
    private int checkedIndex = -1;
    private int checkedTick;

    FactionNavigation(FactionMember member, Level level) {
        super(member, level);
        this.member = member;
    }

    @Override
    public void tick() {
        super.tick();
        this.aimAhead();
    }

    // Within two blocks of the next point, it steers toward a spot between that point and the one after, leaning further the closer it gets. Only on level ground, so jumps and drops keep their exact targets.
    private void aimAhead() {
        Path path = this.path;
        if (path == null || path.isDone() || !this.mob.getMoveControl().hasWanted()) {
            return;
        }
        int next = path.getNextNodeIndex();
        if (next + 1 >= path.getNodeCount()) {
            return;
        }
        Node node = path.getNode(next);
        Node after = path.getNode(next + 1);
        if (node.y != after.y || node.y != Mth.floor(this.member.getY())) {
            return;
        }
        Vec3 here = Vec3.atBottomCenterOf(node.asBlockPos());
        double distance = Math.sqrt(this.member.distanceToSqr(here.x, this.member.getY(), here.z));
        if (distance >= BLEND_RANGE) {
            return;
        }
        Vec3 there = Vec3.atBottomCenterOf(after.asBlockPos());
        double blend = (1.0 - distance / BLEND_RANGE) * MAX_BLEND;
        Vec3 aim = here.lerp(there, blend);
        this.mob.getMoveControl().setWantedPosition(aim.x, here.y, aim.z, this.speedModifier);
    }

    @Override
    protected void followThePath() {
        super.followThePath();
        Path path = this.path;
        if (path == null || path.isDone()) {
            this.checkedIndex = -1;
            return;
        }
        int next = path.getNextNodeIndex();
        if (next == this.checkedIndex && this.member.tickCount - this.checkedTick < RECHECK_TICKS) {
            return;
        }
        this.checkedIndex = next;
        this.checkedTick = this.member.tickCount;
        int furthest = next;
        int last = Math.min(path.getNodeCount() - 1, next + LOOKAHEAD_NODES);
        for (int i = next + 1; i <= last; i++) {
            if (!this.straightTo(path.getNode(i))) {
                break;
            }
            furthest = i;
        }
        if (furthest > next) {
            path.setNextNodeIndex(furthest);
            this.checkedIndex = furthest;
        }
    }

    // Whether it can walk in a straight line from where it stands to the node: same floor level, safe walkable ground under its feet the whole way, and nothing in the way of its body.
    private boolean straightTo(Node node) {
        if (node.y != Mth.floor(this.member.getY())) {
            return false;
        }
        Vec3 from = this.member.position();
        Vec3 to = Vec3.atBottomCenterOf(node.asBlockPos());
        Vec3 line = to.subtract(from);
        double length = line.horizontalDistance();
        if (length < SAMPLE_STEP) {
            return true;
        }
        double half = this.member.getBbWidth() / 2.0;
        AABB body = this.member.getBoundingBox();
        int steps = Mth.ceil(length / SAMPLE_STEP);
        for (int step = 1; step <= steps; step++) {
            Vec3 at = from.add(line.scale(step / (double) steps));
            if (!this.level.noCollision(this.member, body.move(at.subtract(from)))) {
                return false;
            }
            for (int corner = 0; corner < 4; corner++) {
                double x = at.x + ((corner & 1) == 0 ? -half : half);
                double z = at.z + ((corner & 2) == 0 ? -half : half);
                if (!this.walkable(BlockPos.containing(x, at.y, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean walkable(BlockPos feet) {
        PathType type = WalkNodeEvaluator.getPathTypeStatic(this.member, feet);
        return type == PathType.WALKABLE && this.member.getPathfindingMalus(type) >= 0.0f;
    }
}
