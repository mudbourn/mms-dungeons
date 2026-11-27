package info.mudbourn.mmsdungeons.mobs.factions.entity;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// An avian's wings as an elytra: the game's own fall-flying carries the member, and this only turns it, pulls it up while it has speed to spend, and surges to win speed back.
final class FactionGlide {

    enum Intent {
        CRUISE,
        LAND,
        PASS
    }

    private static final float MAX_STAMINA = 1360.0f;
    private static final float SURGE_COST = 100.0f;
    private static final float TAKEOFF_STAMINA = 400.0f;
    private static final float REFILL_TICKS = 300.0f;
    private static final int SURGE_COOLDOWN = 20;
    private static final double SURGE_BOOST = 0.8;
    private static final double SURGE_CAP = 1.9;
    private static final double SURGE_BELOW = 0.9;
    private static final double MAX_SPEED = 2.4;
    private static final double STALL_SPEED = 0.5;
    private static final double CLIMB_SPEED = 1.1;
    private static final float MAX_CLIMB_ANGLE = 50.0f;
    private static final float MAX_DIVE_ANGLE = 60.0f;
    private static final float RECOVER_DIVE = 35.0f;
    private static final float SURGE_CLIMB_PITCH = 10.0f;
    private static final float YAW_RATE = 9.0f;
    private static final float PITCH_RATE = 6.0f;
    private static final float LAUNCH_PITCH = 35.0f;
    private static final double LAUNCH_SPEED = 1.0;
    private static final double JUMP_LIFT = 0.42;
    private static final int LAUNCH_GRACE = 12;
    private static final double PROBE_TICKS = 8.0;
    private static final double PROBE_MIN = 5.0;
    private static final double PROBE_MAX = 24.0;
    private static final float AVOID_PITCH = 45.0f;
    private static final float AVOID_TURN = 60.0f;
    private static final double SIDE_PROBE = 8.0;
    private static final double FLARE_HEIGHT = 5.0;
    private static final float FLARE_PITCH = 40.0f;
    private static final double FLARE_SPEED = 0.35;
    private static final double TOUCH_HEIGHT = 1.2;
    private static final double HEIGHT_SCAN = 32.0;
    private static final double MIN_FLAT = 1.0;
    private static final double MOVING = 0.1;

    private final FactionMember mob;
    private float stamina = MAX_STAMINA;
    private int surgeCooldown;
    private int ticks;
    private boolean active;

    FactionGlide(FactionMember mob) {
        this.mob = mob;
    }

    boolean active() {
        return this.active;
    }

    // Whether the wings have the stamina to take off again.
    boolean rested() {
        return this.stamina >= TAKEOFF_STAMINA;
    }

    // Whether there is no stamina left for a surge.
    boolean spent() {
        return this.stamina < SURGE_COST;
    }

    double speed() {
        return this.mob.getDeltaMovement().length();
    }

    // Stamina comes back while off the wing.
    void rest() {
        this.stamina = Math.min(MAX_STAMINA, this.stamina + MAX_STAMINA / REFILL_TICKS);
    }

    // Jumps and opens the wings toward a point, a surge on the way off the ground.
    void launch(Vec3 toward) {
        float yaw = yawTo(toward.subtract(this.mob.position()));
        this.face(yaw, -LAUNCH_PITCH);
        this.active = true;
        this.ticks = 0;
        this.stamina -= SURGE_COST;
        this.surgeCooldown = SURGE_COOLDOWN;
        this.mob.setGliding(true);
        this.mob.setDeltaMovement(this.mob.getLookAngle().scale(LAUNCH_SPEED).add(0.0, JUMP_LIFT, 0.0));
    }

    // Folds the wings.
    void end() {
        this.active = false;
        this.mob.setGliding(false);
    }

    // Whether it has come back down onto the ground after the take-off.
    boolean grounded() {
        return this.ticks > LAUNCH_GRACE && this.mob.onGround();
    }

    // Whether it has flown into a wall.
    boolean crashed() {
        return this.ticks > LAUNCH_GRACE && this.mob.horizontalCollision;
    }

    // Turns and pitches toward a point for one tick, the way a glider steers: it climbs only on the speed it has, and surges when it is slow.
    void steer(ServerLevel level, Vec3 goal, Intent intent) {
        this.ticks++;
        if (this.surgeCooldown > 0) {
            this.surgeCooldown--;
        }
        this.mob.resetFallDistance();
        Vec3 velocity = this.mob.getDeltaMovement();
        double speed = velocity.length();
        Vec3 to = goal.subtract(this.mob.position());
        float wantYaw = yawTo(to);
        float wantPitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.max(to.horizontalDistance(), MIN_FLAT)));
        float pitch = Mth.clamp(wantPitch, -climbLimit(speed), MAX_DIVE_ANGLE);
        if (speed < STALL_SPEED) {
            pitch = RECOVER_DIVE;
        }
        float yaw = this.mob.getYRot();
        BlockHitResult ahead = this.probe(level, speed > MOVING ? velocity : this.mob.getLookAngle(), Mth.clamp(speed * PROBE_TICKS, PROBE_MIN, PROBE_MAX));
        if (ahead.getType() == HitResult.Type.BLOCK) {
            Direction face = ahead.getDirection();
            if (face == Direction.UP) {
                pitch = intent == Intent.CRUISE ? Math.min(pitch, -Math.min(AVOID_PITCH, climbLimit(speed))) : pitch;
            } else if (face == Direction.DOWN) {
                pitch = Math.max(pitch, 0.0f);
            } else {
                float side = this.openSide(level, yaw);
                if (side == 0.0f) {
                    pitch = -Math.min(AVOID_PITCH, climbLimit(speed));
                } else {
                    wantYaw = yaw + side * AVOID_TURN;
                }
            }
        }
        if (intent == Intent.LAND) {
            double height = this.heightAbove(level);
            if (height <= TOUCH_HEIGHT) {
                this.end();
                return;
            }
            if (height < FLARE_HEIGHT && this.mob.getDeltaMovement().horizontalDistance() > FLARE_SPEED) {
                pitch = -FLARE_PITCH;
            }
        }
        this.face(yaw + turn(yaw, wantYaw, YAW_RATE), this.mob.getXRot() + Mth.clamp(pitch - this.mob.getXRot(), -PITCH_RATE, PITCH_RATE));
        this.surgeIfSlow(level, speed, pitch, intent);
        if (speed > MAX_SPEED) {
            this.mob.setDeltaMovement(velocity.scale(MAX_SPEED / speed));
        }
    }

    // How steeply it may pull up at this speed: not at all at a stall, fully with speed to spare.
    private static float climbLimit(double speed) {
        return Mth.clamp((float) ((speed - STALL_SPEED) / (CLIMB_SPEED - STALL_SPEED)), 0.0f, 1.0f) * MAX_CLIMB_ANGLE;
    }

    private static float yawTo(Vec3 to) {
        return (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0f;
    }

    private static float turn(float from, float to, float rate) {
        return Mth.clamp(Mth.wrapDegrees(to - from), -rate, rate);
    }

    private void face(float yaw, float pitch) {
        this.mob.setYRot(yaw);
        this.mob.setXRot(pitch);
        this.mob.setYHeadRot(yaw);
        this.mob.setYBodyRot(yaw);
    }

    // A burst along the look when it is slow, or slow with a climb to make.
    private void surgeIfSlow(ServerLevel level, double speed, float pitch, Intent intent) {
        boolean climbing = pitch < -SURGE_CLIMB_PITCH;
        boolean slow = speed < SURGE_BELOW || climbing && speed < CLIMB_SPEED;
        if (intent == Intent.LAND || !slow || this.stamina < SURGE_COST || this.surgeCooldown > 0) {
            return;
        }
        this.stamina -= SURGE_COST;
        this.surgeCooldown = SURGE_COOLDOWN;
        this.mob.setDeltaMovement(this.mob.getLookAngle().scale(Math.min(speed + SURGE_BOOST, SURGE_CAP)));
    }

    private BlockHitResult probe(ServerLevel level, Vec3 direction, double length) {
        Vec3 from = this.mob.getEyePosition();
        ClipContext context = new ClipContext(from, from.add(direction.normalize().scale(length)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob);
        return level.clip(context);
    }

    // Which way a wall ahead can be turned around: 1 or -1 for an open side, 0 for none.
    private float openSide(ServerLevel level, float yaw) {
        for (float side : new float[] {1.0f, -1.0f}) {
            float turned = (yaw + side * AVOID_TURN) * Mth.DEG_TO_RAD;
            Vec3 direction = new Vec3(-Mth.sin(turned), 0.0, Mth.cos(turned));
            if (this.probe(level, direction, SIDE_PROBE).getType() == HitResult.Type.MISS) {
                return side;
            }
        }
        return 0.0f;
    }

    // How far the ground is below it, up to a limit.
    private double heightAbove(ServerLevel level) {
        Vec3 from = this.mob.position();
        ClipContext context = new ClipContext(from, from.add(0.0, -HEIGHT_SCAN, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob);
        BlockHitResult hit = level.clip(context);
        return hit.getType() == HitResult.Type.MISS ? HEIGHT_SCAN : from.y - hit.getLocation().y;
    }
}
