package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.mobs.factions.FactionClass;
import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import info.mudbourn.mmsdungeons.mobs.factions.FactionWeapons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Origins flight for faction members: avians surge, fairies hover and blazeborn burn fuel; ranged fliers fight from above, melee fliers fly to reach a target and land to fight.
final class FactionFlight {

    private static final int HEADROOM = 4;
    private static final int GROUND_TICKS = 40;
    private static final int STUCK_TICKS = 60;
    private static final int LANDING_TICKS = 100;
    private static final double FAR_TARGET = 10.0;
    private static final double CLIMB_GAP = 2.5;
    private static final double LAND_RANGE = 3.0;
    private static final float BLAZE_FUEL = 200.0f;
    private static final float BLAZE_TAKEOFF_FUEL = 60.0f;
    private static final float BLAZE_REFUEL = 0.5f;
    private static final int SURGE_COOLDOWN = 20;
    private static final double SURGE_SPEED = 1.2;
    private static final double SURGE_DISTANCE = 8.0;
    private static final double THREAT_RANGE = 4.0;
    private static final float FAIRY_WEAK_HEALTH = 6.0f;
    private static final float HURT_HEALTH = 0.5f;
    private static final double RETREAT_EXTRA = 6.0;
    private static final double CIRCLE_SPEED = 0.03;
    private static final double TAKEOFF_LIFT = 0.7;

    private final FactionMember mob;
    private final int circleSide;
    private boolean airborne;
    private boolean landing;
    private int groundTicks = GROUND_TICKS;
    private int landingTicks;
    private int stuckTicks;
    private int surgeCooldown;
    private float fuel = BLAZE_FUEL;

    FactionFlight(FactionMember mob) {
        this.mob = mob;
        this.circleSide = mob.getRandom().nextBoolean() ? 1 : -1;
    }

    boolean airborne() {
        return this.airborne;
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

    // Runs every server tick after the goals: decides to take off or land, and steers while in the air.
    void tick(ServerLevel level) {
        if (this.surgeCooldown > 0) {
            this.surgeCooldown--;
        }
        if (this.landing) {
            this.tickLanding();
            return;
        }
        LivingEntity target = this.mob.getTarget();
        boolean fighting = target != null && target.isAlive() && !this.mob.downed();
        if (!this.airborne) {
            this.groundTicks++;
            if (this.mob.origin() == FactionOrigin.BLAZEBORN) {
                this.fuel = Math.min(BLAZE_FUEL, this.fuel + BLAZE_REFUEL);
            }
            if (fighting && this.groundTicks >= GROUND_TICKS && this.mayFly() && this.fuelToStart() && this.wantsFlight(level, target)) {
                this.takeOff(level);
            }
            return;
        }
        if (!fighting || !this.mayFly()) {
            this.land();
            return;
        }
        if (this.mob.origin() == FactionOrigin.BLAZEBORN && --this.fuel <= 0.0f) {
            this.land();
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
        this.landing = true;
        this.landingTicks = 0;
        this.mob.setFlying(false);
    }

    // Puts a member saved in the air back on its feet when it loads.
    void forceGround() {
        this.airborne = false;
        this.landing = false;
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
        Vec3 motion = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(motion.x * 0.8, Math.max(motion.y - 0.04, -0.35), motion.z * 0.8);
    }

    private boolean mayFly() {
        return this.winged()
            && !this.mob.isInWater()
            && !this.mob.isPassenger()
            && !this.mob.downed()
            && this.mob.rollTicks() == 0;
    }

    private boolean fuelToStart() {
        return this.mob.origin() != FactionOrigin.BLAZEBORN || this.fuel >= BLAZE_TAKEOFF_FUEL;
    }

    // Ranged and support fliers always fight from the air; melee fliers fly only to reach a target that is far, above them, or cut off.
    private boolean wantsFlight(ServerLevel level, LivingEntity target) {
        if (!this.roomAbove(level, this.mob.blockPosition())) {
            return false;
        }
        if (!this.mob.factionClass().melee()) {
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

    private void takeOff(ServerLevel level) {
        this.airborne = true;
        this.stuckTicks = 0;
        this.mob.setFlying(true);
        this.mob.setNoGravity(true);
        this.mob.getNavigation().stop();
        Vec3 motion = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(motion.x, TAKEOFF_LIFT, motion.z);
        ParticleOptions trail = this.trail();
        if (trail != null) {
            level.sendParticles(trail, this.mob.getX(), this.mob.getY(), this.mob.getZ(), 12, 0.4, 0.1, 0.4, 0.05);
        }
        this.mob.playSound(this.flapSound(), 0.8f, this.flapPitch());
    }

    private void steer(ServerLevel level, LivingEntity target) {
        Vec3 goal = this.goal(level, target);
        Vec3 to = goal.subtract(this.mob.position());
        double distance = to.length();
        boolean threatened = this.mob.distanceTo(target) < THREAT_RANGE && !this.mob.factionClass().melee();
        if (this.mob.origin() == FactionOrigin.AVIAN && this.surgeCooldown == 0 && (distance > SURGE_DISTANCE || threatened) && this.clearTo(level, goal)) {
            this.surge(level, to.normalize());
            return;
        }
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
        this.effects(level);
    }

    // An avian's surge: a burst along the way it is heading, once a second.
    private void surge(ServerLevel level, Vec3 direction) {
        this.mob.setDeltaMovement(direction.scale(SURGE_SPEED));
        this.surgeCooldown = SURGE_COOLDOWN;
        level.sendParticles(ParticleTypes.CLOUD, this.mob.getX(), this.mob.getY(0.5), this.mob.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
        this.mob.playSound(SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.4f);
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
        double range = switch (type) {
            case ARCHER -> 9.0;
            case CLERIC, PRIEST -> 6.0;
            default -> this.mob.caster().preferredRange();
        };
        if (this.mob.consuming() || this.mob.getHealth() < this.mob.getMaxHealth() * HURT_HEALTH) {
            range += RETREAT_EXTRA;
        }
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

    private boolean clearTo(ServerLevel level, Vec3 goal) {
        Vec3 from = this.mob.position().add(0.0, this.mob.getBbHeight() * 0.5, 0.0);
        ClipContext context = new ClipContext(from, goal, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob);
        return level.clip(context).getType() == HitResult.Type.MISS;
    }

    // Flight speed by origin: avians fastest, fairies slowest and halved below 3 hearts, as the players' powers are.
    private double speed() {
        return switch (this.mob.origin()) {
            case AVIAN -> 0.5;
            case FAIRY -> this.mob.getHealth() < FAIRY_WEAK_HEALTH ? 0.16 : 0.32;
            default -> 0.38;
        };
    }

    private void effects(ServerLevel level) {
        ParticleOptions trail = this.trail();
        if (trail != null && this.mob.tickCount % 4 == 0) {
            level.sendParticles(trail, this.mob.getX(), this.mob.getY(0.3), this.mob.getZ(), 2, 0.25, 0.1, 0.25, 0.01);
        }
        if (this.mob.tickCount % 12 == 0) {
            this.mob.playSound(this.flapSound(), 0.4f, this.flapPitch());
        }
        if (this.mob.origin() == FactionOrigin.BLAZEBORN && this.mob.tickCount % 10 == 0) {
            this.mob.igniteForTicks(20);
        }
    }

    // The trail a flier leaves: none for a fairy, whose players give off no particles.
    private @Nullable ParticleOptions trail() {
        return switch (this.mob.origin()) {
            case FAIRY -> null;
            case BLAZEBORN -> ParticleTypes.FLAME;
            default -> ParticleTypes.CLOUD;
        };
    }

    private SoundEvent flapSound() {
        return this.mob.origin() == FactionOrigin.BLAZEBORN ? SoundEvents.BLAZE_BURN : SoundEvents.ENDER_DRAGON_FLAP;
    }

    private float flapPitch() {
        return this.mob.origin() == FactionOrigin.FAIRY ? 2.0f : 1.3f;
    }
}
