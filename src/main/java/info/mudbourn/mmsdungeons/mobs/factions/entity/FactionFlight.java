package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.mobs.factions.FactionClass;
import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import info.mudbourn.mmsdungeons.mobs.factions.FactionWeapons;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Origins flight for faction members: the origin's flight power decides when it may fly and how fast, and this steers; ranged fliers fight from above, melee fliers fly to reach a target and land to fight, and an avian glides like an elytra, lancers jousting and the rest circling above.
final class FactionFlight {

    private static final int HEADROOM = 4;
    private static final int GROUND_TICKS = 40;
    private static final int STUCK_TICKS = 60;
    private static final int LANDING_TICKS = 100;
    private static final double FAR_TARGET = 10.0;
    private static final double CLIMB_GAP = 2.5;
    private static final double LAND_RANGE = 3.0;
    private static final float BLAZE_TAKEOFF_FUEL = 0.3f;
    private static final float WING_SPEED_SCALE = 7.0f;
    private static final double JOURNEY_SURGE_RANGE = 24.0;
    private static final int FORM_GAP = 30;
    private static final int FORM_GESTURE = 11;
    private static final float HURT_HEALTH = 0.5f;
    private static final double RETREAT_EXTRA = 6.0;
    private static final double CIRCLE_SPEED = 0.03;
    private static final double TAKEOFF_LIFT = 0.7;
    private static final double ARCHER_RANGE = 9.0;
    private static final double SUPPORT_RANGE = 6.0;
    private static final double ORBIT_RADIUS = 10.0;
    private static final double ORBIT_LEAD = 10.0;
    private static final double ORBIT_HEIGHT = 8.0;
    private static final double GLIDE_APPROACH = 28.0;
    private static final double CRUISE_HEIGHT = 12.0;
    private static final int JOURNEY_GRACE = 2;
    private static final double JOURNEY_LAND_RANGE = 6.0;
    private static final double JOURNEY_HEIGHT = 3.0;

    private final FactionMember mob;
    private final int circleSide;
    private boolean airborne;
    private boolean landing;
    private int groundTicks = GROUND_TICKS;
    private int landingTicks;
    private int stuckTicks;
    private int surgeTicks;
    private int formGap;
    private @Nullable Vec3 journey;
    private int journeyUntil;
    private boolean sustained;
    private final FactionGlide glide;
    private final FactionJoust joust;

    FactionFlight(FactionMember mob) {
        this.mob = mob;
        this.glide = new FactionGlide(mob);
        this.joust = new FactionJoust(mob);
        this.circleSide = mob.getRandom().nextBoolean() ? 1 : -1;
    }

    boolean airborne() {
        return this.airborne;
    }

    // Whether the wings are open and the game's fall-flying is carrying it.
    boolean gliding() {
        return this.glide.active();
    }

    // Whether this member is an avian knight, who fights with a spear from the air.
    boolean lancer() {
        return this.mob.origin() == FactionOrigin.AVIAN && this.mob.factionClass() == FactionClass.KNIGHT;
    }

    private boolean avian() {
        return this.mob.origin() == FactionOrigin.AVIAN;
    }

    // Flying or still coming down, while gravity is off.
    boolean inAir() {
        return this.airborne || this.landing;
    }

    // Whether this member's origin flies at all.
    boolean winged() {
        FactionOrigin origin = this.mob.origin();
        return origin == FactionOrigin.AVIAN || origin == FactionOrigin.FAIRY || origin == FactionOrigin.BLAZEBORN;
    }

    // Asks the flier to cross a long way by air toward a point; the ask lapses unless renewed every tick.
    void journey(Vec3 goal) {
        this.journey = goal;
        this.journeyUntil = this.mob.tickCount + JOURNEY_GRACE;
    }

    // Whether this member's origin may fly out of a fight: winged origins, and a phantom out of the sun.
    boolean mayJourney(ServerLevel level) {
        return (this.winged() || this.mob.origin() == FactionOrigin.PHANTOM && !this.sunlit(level)) && this.unhindered();
    }

    private boolean travelling() {
        return this.journey != null && (this.sustained || this.mob.tickCount <= this.journeyUntil);
    }

    private boolean sunlit(ServerLevel level) {
        BlockPos head = BlockPos.containing(this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
        return level.isBrightOutside() && level.canSeeSky(head);
    }

    // Runs every server tick after the goals: decides to take off or land, and steers while in the air.
    void tick(ServerLevel level) {
        this.formGap--;
        if (this.landing) {
            this.tickLanding();
            return;
        }
        LivingEntity target = this.mob.getTarget();
        boolean fighting = target != null && target.isAlive() && !this.mob.downed();
        if (!this.airborne) {
            this.groundTicks++;
            this.glide.rest();
            this.tickForm(level, fighting);
            if (fighting && this.groundTicks >= GROUND_TICKS && this.mayFly() && this.fuelToStart() && this.wantsFlight(level, target)) {
                this.takeOff(level, false, target.position());
            } else if (!fighting && this.travelling() && this.groundTicks >= GROUND_TICKS && this.mayJourney(level) && this.powerAllows() && this.fuelToStart()
                && this.roomAbove(level, this.mob.blockPosition())) {
                this.takeOff(level, true, this.journey);
            }
            return;
        }
        if (fighting) {
            this.sustained = false;
        }
        boolean journeying = !fighting && this.travelling();
        if (!this.mob.flying() || !(fighting ? this.mayFly() : journeying && this.mayJourney(level) && this.powerAllows())) {
            this.land();
            return;
        }
        if (this.avian()) {
            this.tickGlide(level, target, journeying);
            return;
        }
        if (journeying) {
            this.tickJourney(level);
            return;
        }
        if (this.mob.factionClass().melee() && this.closeOnFoot(target)) {
            this.land();
            return;
        }
        this.mob.getNavigation().stop();
        this.steer(level, target);
    }

    // Comes down at once, and gently, so a blazeborn out of fuel is not hurt by the fall.
    void land() {
        if (!this.airborne) {
            return;
        }
        this.airborne = false;
        this.sustained = false;
        this.landing = true;
        this.landingTicks = 0;
        this.surgeTicks = 0;
        this.glide.end();
        this.mob.setFlying(false);
    }

    // Puts a member saved in the air back on its feet when it loads.
    void forceGround() {
        this.airborne = false;
        this.sustained = false;
        this.landing = false;
        this.glide.end();
        this.mob.setFlying(false);
        this.mob.setNoGravity(false);
    }

    private void tickLanding() {
        this.mob.resetFallDistance();
        if (this.mob.onGround() || this.mob.isInWater() || ++this.landingTicks > LANDING_TICKS) {
            this.landing = false;
            this.groundTicks = 0;
            this.mob.setNoGravity(false);
            return;
        }
        if (this.avian()) {
            return;
        }
        Vec3 motion = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(motion.x * 0.8, Math.max(motion.y - 0.04, -0.35), motion.z * 0.8);
    }

    private boolean mayFly() {
        return this.winged() && this.unhindered() && this.powerAllows();
    }

    // Whether the origin's flight power lets it fly now; an avian's wings have no such power, so they always do.
    private boolean powerAllows() {
        FactionOrigin origin = this.mob.origin();
        if (origin == FactionOrigin.AVIAN) {
            return true;
        }
        return FactionOrigin.POWERS && FactionWings.allowed(this.mob);
    }

    // A phantom takes its form to fly and leaves it once it has no flight to make; the form is the power's own gesture, sneak and the second key.
    private void tickForm(ServerLevel level, boolean fighting) {
        if (this.mob.origin() != FactionOrigin.PHANTOM || !FactionOrigin.POWERS || this.formGap > 0 || this.mob.powers().holding() || this.mob.downed()) {
            return;
        }
        boolean formed = FactionWings.formed(this.mob);
        boolean wantsForm = !fighting && this.travelling() && this.mayJourney(level);
        if (formed != wantsForm) {
            this.mob.powers().hold(FactionOriginPowers.SECONDARY, null, FORM_GESTURE, true);
            this.formGap = FORM_GAP;
        }
    }

    private boolean unhindered() {
        return !this.mob.isInWater()
            && !this.mob.isPassenger()
            && !this.mob.downed()
            && this.mob.rollTicks() == 0;
    }

    private boolean fuelToStart() {
        return switch (this.mob.origin()) {
            case AVIAN -> this.glide.rested();
            case BLAZEBORN -> FactionOrigin.POWERS && FactionWings.fuel(this.mob) >= BLAZE_TAKEOFF_FUEL;
            default -> true;
        };
    }

    // Ranged and support fliers and lancers always fight from the air; other melee fliers fly only to reach a target that is far, above them, or cut off.
    private boolean wantsFlight(ServerLevel level, LivingEntity target) {
        if (!this.roomAbove(level, this.mob.blockPosition())) {
            return false;
        }
        if (!this.mob.factionClass().melee() || this.lancer()) {
            return true;
        }
        Path path = this.mob.getNavigation().getPath();
        boolean cutOff = path != null && !path.canReach() || this.mob.getNavigation().isStuck();
        return this.mob.distanceTo(target) > FAR_TARGET || target.getY() - this.mob.getY() > CLIMB_GAP || cutOff;
    }

    private boolean closeOnFoot(LivingEntity target) {
        double dx = target.getX() - this.mob.getX();
        double dz = target.getZ() - this.mob.getZ();
        return target.onGround() && dx * dx + dz * dz < LAND_RANGE * LAND_RANGE && Math.abs(target.getY() - this.mob.getY()) < 1.5;
    }

    private boolean roomAbove(ServerLevel level, BlockPos feet) {
        for (int up = 2; up <= HEADROOM; up++) {
            BlockPos pos = feet.above(up);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void takeOff(ServerLevel level, boolean onJourney, Vec3 heading) {
        this.airborne = true;
        this.sustained = onJourney;
        this.stuckTicks = 0;
        this.surgeTicks = 0;
        this.mob.setFlying(true);
        this.mob.getNavigation().stop();
        if (this.avian()) {
            this.glide.launch(heading);
        } else {
            this.mob.setNoGravity(true);
            Vec3 motion = this.mob.getDeltaMovement();
            this.mob.setDeltaMovement(motion.x, TAKEOFF_LIFT, motion.z);
        }
    }

    // Flies toward the journey's end and lands once it is near.
    private void tickJourney(ServerLevel level) {
        Vec3 goal = this.journey;
        if (goal == null || this.mob.position().subtract(goal).horizontalDistance() <= JOURNEY_LAND_RANGE) {
            this.land();
            return;
        }
        this.mob.getNavigation().stop();
        this.surgeTicks = this.mob.position().subtract(goal).horizontalDistance() > JOURNEY_SURGE_RANGE ? this.surgeTicks + 1 : 0;
        this.fly(level, goal.add(0.0, JOURNEY_HEIGHT, 0.0));
    }

    private void steer(ServerLevel level, LivingEntity target) {
        this.surgeTicks = 0;
        this.fly(level, this.goal(level, target));
    }

    private void fly(ServerLevel level, Vec3 goal) {
        Vec3 to = goal.subtract(this.mob.position());
        double distance = to.length();
        double speed = this.speed();
        Vec3 wanted = distance < 0.05 ? Vec3.ZERO : to.scale(Math.min(speed, distance * 0.3) / distance);
        if (this.mob.horizontalCollision) {
            wanted = wanted.add(0.0, speed, 0.0);
        }
        Vec3 motion = this.mob.getDeltaMovement();
        motion = motion.add(wanted.subtract(motion).scale(0.25));
        motion = motion.add(0.0, Mth.sin(this.mob.tickCount * 0.2f) * 0.01, 0.0);
        this.mob.setDeltaMovement(motion);
        this.mob.resetFallDistance();
        this.stuckTicks = distance > 3.0 && motion.horizontalDistance() < 0.03 ? this.stuckTicks + 1 : 0;
        if (this.stuckTicks > STUCK_TICKS) {
            this.land();
            return;
        }
    }

    // An avian glides: it lands when it touches down or crashes, flies a journey as a long glide, and fights as a lancer on passes or as an archer or mage circling above.
    private void tickGlide(ServerLevel level, @Nullable LivingEntity target, boolean journeying) {
        if (this.glide.grounded() || this.glide.crashed()) {
            this.land();
            return;
        }
        this.mob.getNavigation().stop();
        if (journeying) {
            this.glideJourney(level);
        } else if (target != null) {
            this.glideFight(level, target);
        }
        if (!this.glide.active()) {
            this.land();
        }
    }

    // Cruises toward the journey's end, then glides down onto it once near.
    private void glideJourney(ServerLevel level) {
        Vec3 goal = this.journey;
        if (goal == null) {
            this.land();
            return;
        }
        if (this.mob.position().subtract(goal).horizontalDistance() <= GLIDE_APPROACH) {
            this.glide.steer(level, goal, FactionGlide.Intent.LAND);
            return;
        }
        this.glide.steer(level, goal.add(0.0, CRUISE_HEIGHT, 0.0), FactionGlide.Intent.CRUISE);
    }

    // With no stamina left it glides down beside the target to fight on foot.
    private void glideFight(ServerLevel level, LivingEntity target) {
        if (this.glide.spent()) {
            this.joust.stop();
            this.glide.steer(level, this.landingSpot(target), FactionGlide.Intent.LAND);
        } else if (this.lancer()) {
            Vec3 goal = this.joust.goal(level, target, this.glide.speed());
            this.glide.steer(level, goal, this.joust.attacking() ? FactionGlide.Intent.PASS : FactionGlide.Intent.CRUISE);
        } else {
            this.glide.steer(level, this.orbit(level, target), FactionGlide.Intent.CRUISE);
        }
    }

    // Where a spent glider comes down: on the target for a lancer, at its range for a shooter.
    private Vec3 landingSpot(LivingEntity target) {
        if (this.lancer()) {
            return target.position();
        }
        Vec3 away = this.mob.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(1.0, 0.0, 0.0);
        }
        return target.position().add(away.normalize().scale(this.standoff(this.mob.factionClass())));
    }

    // A point along a wide ring high above the target, always a little ahead of the glider so it keeps circling.
    private Vec3 orbit(ServerLevel level, LivingEntity target) {
        double radius = Math.max(ORBIT_RADIUS, this.standoff(this.mob.factionClass()));
        Vec3 away = this.mob.position().subtract(target.position());
        double angle = Mth.atan2(away.z, away.x) + ORBIT_LEAD / radius * this.circleSide;
        Vec3 ring = target.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
        return ring.add(0.0, Math.max(1.0, Math.min(ORBIT_HEIGHT, this.ceiling(level, ring, ORBIT_HEIGHT) - 2.0)), 0.0);
    }

    // How far off a ranged or support member keeps from its target, farther off when hurt.
    private double standoff(FactionClass type) {
        double range = switch (type) {
            case ARCHER -> ARCHER_RANGE;
            case CLERIC, PRIEST -> SUPPORT_RANGE;
            default -> this.mob.caster().preferredRange();
        };
        if (this.mob.consuming() || this.mob.getHealth() < this.mob.getMaxHealth() * HURT_HEALTH) {
            range += RETREAT_EXTRA;
        }
        return range;
    }

    // Where to be: beside a melee target at weapon reach, or circling a ranged one high up at the class's range, farther off when hurt.
    private Vec3 goal(ServerLevel level, LivingEntity target) {
        FactionClass type = this.mob.factionClass();
        Vec3 fromTarget = this.mob.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
        if (fromTarget.lengthSqr() < 1.0E-4) {
            fromTarget = new Vec3(1.0, 0.0, 0.0);
        }
        if (type.melee()) {
            double reach = FactionWeapons.reach(this.mob.getMainHandItem()) * 0.6;
            return target.position().add(fromTarget.normalize().scale(reach));
        }
        double range = this.standoff(type);
        double angle = Mth.atan2(fromTarget.z, fromTarget.x) + CIRCLE_SPEED * this.circleSide;
        Vec3 ring = target.position().add(Math.cos(angle) * range, 0.0, Math.sin(angle) * range);
        double height = switch (type) {
            case ARCHER -> 5.0;
            case CLERIC, PRIEST -> 3.5;
            default -> 4.5;
        };
        return ring.add(0.0, Math.max(1.0, Math.min(height, this.ceiling(level, ring, height) - 2.0)), 0.0);
    }

    // How far up from a spot the air stays open, looking no higher than a little over the wanted height.
    private double ceiling(ServerLevel level, Vec3 spot, double height) {
        BlockPos base = BlockPos.containing(spot);
        int limit = Mth.ceil(height) + 2;
        for (int up = 1; up <= limit; up++) {
            BlockPos pos = base.above(up);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return up;
            }
        }
        return limit + 1;
    }

    // The flight power's speed scaled from a player's flying speed to blocks a tick.
    private double speed() {
        return FactionWings.speed(this.mob, this.surgeTicks) * WING_SPEED_SCALE;
    }
}
