package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// How a faction member gets about, by time and place: it walks short distances, runs (or sometimes strolls) longer ones, and only sprint-jumps and uses its origin's movement tricks when it hurries to help a crewmate, to reach something it is hostile to, or to get away from a player chasing it.
final class FactionTravel {

    private static final double SPRINT_FAR = 8.0;
    private static final double SPRINT_NEAR = 4.0;
    private static final double RUN_FAR = 14.0;
    private static final float STROLL_CHANCE = 0.35f;
    private static final double SAME_GOAL = 3.0;
    private static final int JUMP_COOLDOWN = 6;
    // How long it sprints on the ground before it starts sprint-jumping: a short run-up of under a second.
    private static final int RUN_UP_MIN_TICKS = 6;
    private static final int RUN_UP_SPREAD_TICKS = 10;
    private static final int JUMP_NODES = 3;
    private static final int CLEAR_FROM = 2;
    private static final int CLEAR_TO = 3;
    private static final double STRAIGHT_DOT = 0.9;
    private static final double FLY_FAR = 24.0;
    private static final int TOGGLE_GAP = 40;
    private static final double CLIMB_GAP = 3.0;
    private static final double LEAP_FAR = 14.0;
    private static final int LEAP_NODES = 6;
    private static final int LEAP_GAP = 95;
    private static final float LEAP_TILT = -10.0f;
    private static final double PEARL_FAR = 16.0;
    private static final int PEARL_GAP = 32;
    private static final double PEARL_SPEED = 1.5;
    private static final double PEARL_GRAVITY = 0.03;
    private static final double PEARL_DRAG = 0.99;
    private static final double PEARL_RANGE = 28.0;
    private static final double PEARL_MIN_RANGE = 8.0;
    private static final double PEARL_MIN_STUCK_RANGE = 3.5;
    private static final double PEARL_RISE = 12.0;
    private static final double PEARL_DROP = 12.0;
    private static final int PEARL_TRIES = 6;
    private static final int PEARL_NODE_SPACING = 3;
    private static final double PEARL_GAIN = 6.0;
    private static final double PEARL_GAIN_STUCK = 3.0;
    private static final double PEARL_TOLERANCE = 3.5;
    private static final double PEARL_HEIGHT_TOLERANCE = 2.5;
    private static final int ARC_STEPS = 80;
    private static final double ANGLE_LOW = -10.0;
    private static final double ANGLE_HIGH = 75.0;
    private static final double ANGLE_STEP = 2.5;
    private static final int SNAP_RISE = 6;
    private static final int SNAP_DROP = 14;
    private static final double DASH_FAR = 10.0;
    private static final int DASH_GAP = 70;
    private static final int DASH_ROLL_DELAY = 3;
    private static final int DASH_RUN_NODES = 3;
    private static final double SLING_FAR = 12.0;
    private static final int SLING_GAP = 60;
    private static final int SLING_COST = 2;
    private static final int SLING_HEADROOM = 8;
    private static final double SLING_ROOM = 4.0;
    private static final double SLING_AHEAD = 7.0;
    private static final double SLING_DROP = 14.0;
    private static final double SLING_RISE = 3.0;
    private static final double SLING_BOOST = 0.35;
    private static final double SLING_LIFT = 0.1;
    private static final double SLING_APEX = 0.05;
    private static final double LEAF_FAR = 16.0;
    private static final double LEAF_NEAR = 6.0;
    private static final double LEAF_FIGHT_RANGE = 12.0;
    private static final int LEAF_GAP = 100;
    private static final TagKey<Block> LEAF_PASSABLE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("mms_origins", "leaf_passable"));
    private static final Vec3[] AROUND = {
        new Vec3(1.0, 0.0, 0.0),
        new Vec3(-1.0, 0.0, 0.0),
        new Vec3(0.0, 0.0, 1.0),
        new Vec3(0.0, 0.0, -1.0),
        new Vec3(0.7, 0.0, 0.7),
        new Vec3(-0.7, 0.0, 0.7),
        new Vec3(0.7, 0.0, -0.7),
        new Vec3(-0.7, 0.0, -0.7)
    };

    private final FactionMember mob;
    private boolean sprinting;
    private boolean ownSprint;
    private int jumpGap;
    private int runUp = -1;
    private int leapGap;
    private int pearlGap;
    private int dashGap;
    private int slingGap;
    private int toggleGap;
    private int leafGap;
    private int rollAt = -1;
    private Vec3 rollDirection = Vec3.ZERO;
    private boolean slingReady;
    private @Nullable Vec3 chargeAim;
    private @Nullable Vec3 pacedGoal;
    private boolean strolling;

    // Where the member is headed: the path's end, how far it still is, and whether the way there is cut off.
    private record Route(@Nullable Path path, Vec3 goal, double span, boolean cutOff) {
    }

    FactionTravel(FactionMember mob) {
        this.mob = mob;
    }

    // Every server tick: sprints and sprint-jumps toward a far goal, and uses the origin's movement powers on the way.
    void tick(ServerLevel level) {
        this.jumpGap--;
        this.leapGap--;
        this.pearlGap--;
        this.dashGap--;
        this.slingGap--;
        this.toggleGap--;
        this.leafGap--;
        this.keepAim();
        if (this.mob.downed() || this.mob.isPassenger() || this.mob.consuming() || this.mob.tactics().rolling()) {
            this.stopSprint();
            return;
        }
        Route route = this.route();
        if (this.mob.flight().inAir()) {
            if (route != null) {
                this.mob.flight().journey(route.goal());
            }
            this.stopSprint();
            return;
        }
        if (this.tickLeaf(level, route)) {
            this.stopSprint();
            return;
        }
        if (route != null && route.span() >= FLY_FAR && this.mob.flight().mayJourney(level)) {
            this.mob.flight().journey(route.goal());
        }
        double span = route == null ? 0.0 : route.span();
        boolean urgent = this.urgent();
        boolean wantsRun = urgent
            ? span >= SPRINT_FAR || this.sprinting && span >= SPRINT_NEAR
            : !this.strolls(route) && (span >= RUN_FAR || this.sprinting && span >= SPRINT_FAR);
        this.sprinting = wantsRun;
        boolean running = wantsRun && this.mayRun();
        this.setSprint(running);
        boolean far = running && urgent;
        if (!far) {
            this.runUp = -1;
        } else if (this.runUp < 0) {
            this.runUp = RUN_UP_MIN_TICKS + this.mob.getRandom().nextInt(RUN_UP_SPREAD_TICKS);
        } else if (this.runUp > 0) {
            this.runUp--;
        }
        this.tickRollAfterDash();
        if (this.mob.powers().holding()) {
            return;
        }
        if (FactionOrigin.POWERS && this.mob.origin() != FactionOrigin.NONE) {
            this.originTravel(level, route, far);
        }
        if (far && this.runUp == 0 && route != null && route.path() != null && this.mob.onGround() && this.jumpGap <= 0
            && this.openAhead(level, route.path(), JUMP_NODES)) {
            this.mob.getJumpControl().jump();
            this.jumpGap = JUMP_COOLDOWN;
            this.slingReady = true;
        }
    }

    // In a hurry: closing on something it is hostile to, on its way to revive a crewmate, or running from a player chasing it.
    private boolean urgent() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() || this.mob.reviving() || this.mob.fleeing();
    }

    // Whether it has chosen to stroll rather than run to this goal: rolled once each time it sets off somewhere new.
    private boolean strolls(@Nullable Route route) {
        if (route == null) {
            this.pacedGoal = null;
            return false;
        }
        if (this.pacedGoal == null || this.pacedGoal.distanceTo(route.goal()) > SAME_GOAL) {
            this.pacedGoal = route.goal();
            this.strolling = this.mob.getRandom().nextFloat() < STROLL_CHANCE;
        }
        return this.strolling;
    }

    // Keeps facing the chosen way while a charged leap is held.
    private void keepAim() {
        Vec3 aim = this.chargeAim;
        if (aim == null) {
            return;
        }
        if (this.mob.powers().holding()) {
            this.mob.powers().aim(aim, LEAP_TILT);
        } else {
            this.chargeAim = null;
        }
    }

    private boolean mayRun() {
        return !this.mob.crawling() && !this.mob.isInWater() && !this.mob.armless() && !this.mob.isUsingItem();
    }

    private void setSprint(boolean on) {
        if (on) {
            this.mob.setSprinting(true);
            this.ownSprint = true;
        } else {
            this.stopSprint();
        }
    }

    private void stopSprint() {
        this.sprinting = false;
        if (this.ownSprint) {
            this.mob.setSprinting(false);
            this.ownSprint = false;
        }
    }

    // The way it is going: the end of its path, or its target when there is no path to it.
    private @Nullable Route route() {
        Path path = this.mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            Vec3 goal = Vec3.atBottomCenterOf(path.getTarget());
            double span = Math.max(this.mob.position().distanceTo(goal), this.remaining(path));
            return new Route(path, goal, span, !path.canReach() || this.mob.getNavigation().isStuck());
        }
        LivingEntity target = this.mob.getTarget();
        if (target != null && target.isAlive()) {
            return new Route(null, target.position(), this.mob.position().distanceTo(target.position()), true);
        }
        return null;
    }

    private double remaining(Path path) {
        double total = 0.0;
        Vec3 from = this.mob.position();
        for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) {
            Vec3 at = Vec3.atBottomCenterOf(path.getNodePos(i));
            total += from.distanceTo(at);
            from = at;
        }
        return total;
    }

    // Whether the next path nodes run level and straight with a roof high enough to jump under.
    private boolean openAhead(ServerLevel level, Path path, int count) {
        int next = path.getNextNodeIndex();
        if (next + count > path.getNodeCount()) {
            return false;
        }
        int feet = this.mob.blockPosition().getY();
        Vec3 before = this.mob.position();
        Vec3 heading = null;
        if (!this.roofClear(level, this.mob.blockPosition())) {
            return false;
        }
        for (int i = next; i < next + count; i++) {
            BlockPos node = path.getNodePos(i);
            Vec3 at = Vec3.atBottomCenterOf(node);
            Vec3 step = at.subtract(before).multiply(1.0, 0.0, 1.0);
            if (node.getY() != feet || step.lengthSqr() < 1.0E-4 || !this.roofClear(level, node)) {
                return false;
            }
            step = step.normalize();
            if (heading != null && heading.dot(step) < STRAIGHT_DOT) {
                return false;
            }
            heading = step;
            before = at;
        }
        return true;
    }

    private boolean roofClear(ServerLevel level, BlockPos feet) {
        for (int up = CLEAR_FROM; up <= CLEAR_TO; up++) {
            BlockPos pos = feet.above(up);
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // The origin's movement tricks, used only when in a hurry; a truffle keeps its spores in otherwise.
    private void originTravel(ServerLevel level, @Nullable Route route, boolean far) {
        double span = route == null ? 0.0 : route.span();
        switch (this.mob.origin()) {
            case BEASTFOLK -> this.beastfolk(level, route, far);
            case ENDERIAN -> {
                if (far) {
                    this.enderian(level, route);
                }
            }
            case FISHMAN -> {
                if (far) {
                    this.fishman(level, route);
                }
            }
            case ARACHNID -> {
                if (far) {
                    this.arachnid(level, route);
                }
            }
            case TRUFFLE -> this.truffle(far ? span : 0.0);
            default -> {
            }
        }
    }

    // Beastfolk: claws out so sprint jumps carry farther, and Acrobatics' charged leap along open ground.
    private void beastfolk(ServerLevel level, @Nullable Route route, boolean far) {
        if (far && !FactionOriginPowers.active(this.mob, FactionPowers.CLAWS) && this.toggleGap <= 0) {
            this.mob.powers().tap(FactionOriginPowers.SECONDARY, false);
            this.toggleGap = TOGGLE_GAP;
            return;
        }
        Path path = route == null ? null : route.path();
        if (path == null || route.span() < LEAP_FAR || this.leapGap > 0 || !this.mob.onGround() || !this.openAhead(level, path, LEAP_NODES)) {
            return;
        }
        Vec3 ahead = Vec3.atBottomCenterOf(path.getNodePos(path.getNextNodeIndex() + LEAP_NODES - 1));
        this.chargeAim = new Vec3(ahead.x, this.mob.getEyeY(), ahead.z);
        this.mob.powers().aim(this.chargeAim, LEAP_TILT);
        this.mob.powers().hold(FactionOriginPowers.PRIMARY, null, FactionPowers.LEAP_CHARGE, false);
        this.leapGap = LEAP_GAP;
    }

    // Enderian: ender pearls chained over long ground, up onto ledges, and across gaps and water, only to a landing it can see and trust.
    private void enderian(ServerLevel level, @Nullable Route route) {
        if (route == null || this.pearlGap > 0 || !this.mob.onGround() || this.mob.isInWater()) {
            return;
        }
        boolean climb = route.goal().y - this.mob.getY() > CLIMB_GAP;
        boolean stuck = route.cutOff() && this.mob.position().distanceTo(route.goal()) > PEARL_MIN_STUCK_RANGE;
        if (route.span() < PEARL_FAR && !climb && !stuck) {
            return;
        }
        boolean hard = climb || stuck;
        Vec3 eye = this.mob.getEyePosition();
        double needed = hard ? PEARL_GAIN_STUCK : PEARL_GAIN;
        for (Vec3 spot : this.pearlSpots(level, route, hard)) {
            double angle = this.pearlAngle(eye, spot);
            if (Double.isNaN(angle)) {
                continue;
            }
            Vec3 flat = spot.subtract(eye).multiply(1.0, 0.0, 1.0).normalize();
            Vec3 velocity = this.pearlVelocity(flat, angle);
            BlockHitResult hit = this.arc(level, eye, velocity);
            if (hit == null || !this.pearlLanding(level, hit, spot)) {
                continue;
            }
            double gain = this.mob.position().distanceTo(route.goal()) - hit.getLocation().distanceTo(route.goal());
            if (gain < needed) {
                continue;
            }
            this.mob.powers().aim(eye.add(flat), (float) -angle);
            this.mob.powers().tap(FactionOriginPowers.PRIMARY, false);
            this.pearlGap = PEARL_GAP;
            return;
        }
        this.pearlGap = PEARL_GAP / 2;
    }

    // Places to land, farthest first: along the path, and when it is cut off or the goal is high, straight toward the goal.
    private List<Vec3> pearlSpots(ServerLevel level, Route route, boolean hard) {
        List<Vec3> spots = new ArrayList<>();
        Vec3 from = this.mob.position();
        double min = hard ? PEARL_MIN_STUCK_RANGE : PEARL_MIN_RANGE;
        Path path = route.path();
        if (path != null) {
            for (int i = path.getNodeCount() - 1; i >= path.getNextNodeIndex() && spots.size() < PEARL_TRIES; i -= PEARL_NODE_SPACING) {
                Vec3 node = Vec3.atBottomCenterOf(path.getNodePos(i));
                double flat = node.subtract(from).horizontalDistance();
                double rise = node.y - from.y;
                if (flat >= min && flat <= PEARL_RANGE && rise <= PEARL_RISE && rise >= -PEARL_DROP) {
                    spots.add(node);
                }
            }
        }
        if (hard) {
            Vec3 to = route.goal().subtract(from).multiply(1.0, 0.0, 1.0);
            double flat = to.length();
            if (flat > 1.0E-4) {
                Vec3 direction = to.normalize();
                for (double distance : new double[] {Math.min(flat, PEARL_RANGE), 20.0, 12.0}) {
                    if (distance < min || distance > flat) {
                        continue;
                    }
                    Vec3 snapped = this.snap(level, from.add(direction.scale(distance)), route.goal().y);
                    if (snapped != null) {
                        spots.add(snapped);
                    }
                }
            }
        }
        return spots;
    }

    // The standing spot in a column near a height, found by looking down from above it.
    private @Nullable Vec3 snap(ServerLevel level, Vec3 column, double height) {
        BlockPos top = BlockPos.containing(column.x, height + SNAP_RISE, column.z);
        for (int down = 0; down <= SNAP_DROP + SNAP_RISE; down++) {
            BlockPos pos = top.below(down);
            if (!level.hasChunkAt(pos)) {
                return null;
            }
            boolean room = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
            if (room && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                return Vec3.atBottomCenterOf(pos);
            }
        }
        return null;
    }

    // The launch angle in degrees above flat that brings a pearl down nearest a spot, or NaN when none gets there.
    private double pearlAngle(Vec3 eye, Vec3 spot) {
        double reach = spot.subtract(eye).horizontalDistance();
        double best = Double.NaN;
        double bestError = PEARL_TOLERANCE;
        for (double angle = ANGLE_LOW; angle <= ANGLE_HIGH; angle += ANGLE_STEP) {
            double speed = PEARL_SPEED;
            double along = 0.0;
            double height = eye.y;
            double rise = Math.sin(angle * Mth.DEG_TO_RAD) * speed;
            double run = Math.cos(angle * Mth.DEG_TO_RAD) * speed;
            for (int step = 0; step < ARC_STEPS; step++) {
                along += run;
                height += rise;
                run *= PEARL_DRAG;
                rise = rise * PEARL_DRAG - PEARL_GRAVITY;
                if (rise < 0.0 && height <= spot.y) {
                    double error = Math.abs(along - reach);
                    if (error < bestError) {
                        bestError = error;
                        best = angle;
                    }
                    break;
                }
            }
        }
        return best;
    }

    private Vec3 pearlVelocity(Vec3 flat, double angle) {
        double up = Math.sin(angle * Mth.DEG_TO_RAD) * PEARL_SPEED;
        double run = Math.cos(angle * Mth.DEG_TO_RAD) * PEARL_SPEED;
        return new Vec3(flat.x * run, up, flat.z * run);
    }

    // Flies a pearl through the world and returns the block it hits, or null if it falls out of the world first.
    private @Nullable BlockHitResult arc(ServerLevel level, Vec3 from, Vec3 launch) {
        Vec3 at = from;
        Vec3 velocity = launch;
        for (int step = 0; step < ARC_STEPS; step++) {
            Vec3 next = at.add(velocity);
            if (!level.hasChunkAt(BlockPos.containing(next))) {
                return null;
            }
            BlockHitResult hit = level.clip(new ClipContext(at, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob));
            if (hit.getType() == HitResult.Type.BLOCK) {
                return hit;
            }
            if (next.y < level.getMinY()) {
                return null;
            }
            at = next;
            velocity = new Vec3(velocity.x * PEARL_DRAG, velocity.y * PEARL_DRAG - PEARL_GRAVITY, velocity.z * PEARL_DRAG);
        }
        return null;
    }

    // A landing on the top of solid ground with room to stand, near the spot aimed at, clear of lava and fire.
    private boolean pearlLanding(ServerLevel level, BlockHitResult hit, Vec3 spot) {
        if (hit.getDirection() != Direction.UP) {
            return false;
        }
        Vec3 land = hit.getLocation();
        if (land.subtract(spot).horizontalDistance() > PEARL_TOLERANCE || Math.abs(land.y - spot.y) > PEARL_HEIGHT_TOLERANCE) {
            return false;
        }
        BlockPos floor = hit.getBlockPos();
        BlockState feet = level.getBlockState(floor.above());
        BlockState head = level.getBlockState(floor.above(2));
        boolean room = feet.getCollisionShape(level, floor.above()).isEmpty() && head.getCollisionShape(level, floor.above(2)).isEmpty();
        boolean hazard = feet.getFluidState().is(FluidTags.LAVA)
            || level.getBlockState(floor).getFluidState().is(FluidTags.LAVA)
            || feet.is(BlockTags.FIRE)
            || level.getBlockState(floor).is(BlockTags.FIRE);
        return room && !hazard;
    }

    // Fishman: Aqua dash along the way when in water or wet, and a combat roll right after it on land.
    private void fishman(ServerLevel level, @Nullable Route route) {
        if (route == null || route.span() < DASH_FAR || this.dashGap > 0 || !this.mob.isInWaterOrRain()) {
            return;
        }
        boolean swimming = this.mob.isInWater();
        Path path = route.path();
        if (!swimming && (path == null || !this.mob.onGround() || !this.openAhead(level, path, DASH_RUN_NODES))) {
            return;
        }
        Vec3 to = route.goal().subtract(this.mob.position());
        Vec3 aimAt = swimming ? this.mob.getEyePosition().add(to) : this.mob.getEyePosition().add(to.x, 0.0, to.z);
        this.mob.powers().aim(aimAt, 0.0f);
        this.mob.powers().tap(FactionOriginPowers.SECONDARY, false);
        this.dashGap = DASH_GAP;
        if (!swimming) {
            this.rollAt = this.mob.tickCount + DASH_ROLL_DELAY;
            this.rollDirection = to;
        }
    }

    private void tickRollAfterDash() {
        if (this.rollAt < 0 || this.mob.tickCount < this.rollAt) {
            return;
        }
        this.rollAt = -1;
        if (this.mob.onGround() && !this.mob.isInWater()) {
            this.mob.tactics().travelRoll(this.rollDirection);
        }
    }

    // Arachnid: at the top of a sprint jump in the open, a web slung at the ground ahead is reeled for an instant and let go, keeping the speed.
    private void arachnid(ServerLevel level, @Nullable Route route) {
        if (!this.slingReady || this.mob.onGround()) {
            return;
        }
        Vec3 motion = this.mob.getDeltaMovement();
        if (motion.y > SLING_APEX) {
            return;
        }
        this.slingReady = false;
        if (route == null || route.span() < SLING_FAR || this.slingGap > 0 || !this.mob.isSprinting()
            || FactionOriginPowers.resource(this.mob, FactionPowers.SILK) < SLING_COST || !this.openSpace(level)) {
            return;
        }
        Vec3 heading = motion.multiply(1.0, 0.0, 1.0);
        if (heading.lengthSqr() < 1.0E-4) {
            return;
        }
        heading = heading.normalize();
        Vec3 column = this.mob.position().add(heading.scale(SLING_AHEAD));
        BlockHitResult floor = level.clip(new ClipContext(column.add(0.0, SLING_RISE, 0.0), column.add(0.0, -SLING_DROP, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob));
        if (floor.getType() != HitResult.Type.BLOCK || floor.getDirection() != Direction.UP) {
            return;
        }
        Vec3 anchor = floor.getLocation();
        Vec3 eye = this.mob.getEyePosition();
        if (level.clip(new ClipContext(eye, anchor.add(0.0, 0.05, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob)).getType() != HitResult.Type.MISS) {
            return;
        }
        FactionOriginPowers.spend(this.mob, FactionPowers.SILK, SLING_COST);
        this.mob.setDeltaMovement(motion.add(heading.x * SLING_BOOST, SLING_LIFT, heading.z * SLING_BOOST));
        this.mob.needsSync = true;
        this.slingGap = SLING_GAP;
    }

    // Open sky or a tall ceiling over its head and clear air around it, not a cave or a tunnel.
    private boolean openSpace(ServerLevel level) {
        BlockPos head = this.mob.blockPosition();
        for (int up = 1; up <= SLING_HEADROOM; up++) {
            BlockPos pos = head.above(up);
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return false;
            }
        }
        Vec3 eye = this.mob.getEyePosition();
        for (Vec3 side : AROUND) {
            ClipContext context = new ClipContext(eye, eye.add(side.scale(SLING_ROOM)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob);
            if (level.clip(context).getType() != HitResult.Type.MISS) {
                return false;
            }
        }
        return true;
    }

    // Truffle: spores drawn while crossing far ground for their Speed, and put away once idle to save charge.
    private void truffle(double span) {
        int charge = FactionOriginPowers.resource(this.mob, FactionPowers.SPORES_DRAWN);
        if (charge < 0 || this.toggleGap > 0) {
            return;
        }
        boolean idle = this.mob.getTarget() == null && span < SPRINT_NEAR;
        if (charge == 0 && span >= SPRINT_FAR || charge != 0 && idle) {
            this.mob.powers().tap(FactionOriginPowers.SECONDARY, false);
            this.toggleGap = TOGGLE_GAP;
        }
    }

    // Floran: folds into leaf form to cross far ground that leaves can seep through, and unfolds near the goal or off such ground. True while in leaf form.
    private boolean tickLeaf(ServerLevel level, @Nullable Route route) {
        if (this.mob.origin() != FactionOrigin.FLORAN) {
            return false;
        }
        FactionFloran floran = this.mob.powers().floran();
        LivingEntity target = this.mob.getTarget();
        boolean fighting = target != null && this.mob.distanceTo(target) < LEAF_FIGHT_RANGE;
        boolean onLeaves = level.getBlockState(this.mob.blockPosition().below()).is(LEAF_PASSABLE);
        if (floran.journeying()) {
            if (route == null || route.span() < LEAF_NEAR || !onLeaves || fighting) {
                floran.journey(false);
                this.leafGap = LEAF_GAP;
                return false;
            }
            return true;
        }
        if (route != null && route.span() >= LEAF_FAR && onLeaves && !fighting && this.leafGap <= 0 && this.mob.onGround() && !floran.leafForm() && !floran.changing()) {
            floran.journey(true);
            return true;
        }
        return false;
    }
}
