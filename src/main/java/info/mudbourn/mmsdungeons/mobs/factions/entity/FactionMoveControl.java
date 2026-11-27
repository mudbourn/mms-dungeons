package info.mudbourn.mmsdungeons.mobs.factions.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

// Walks a path the way a player fights: facing where it looks and stepping forward, sideways or a short way back along the path. Flying and downed members leave movement to their own code.
final class FactionMoveControl extends MoveControl<FactionMember> {

    private static final float MAX_TURN = 25.0f;
    private static final float TURN_EASE = 0.45f;
    private static final float MIN_TURN = 3.0f;
    // A player backs off a step or two from a foe in front of them, but turns round to go anywhere farther.
    private static final double BACKPEDAL_RANGE = 6.0;
    private static final double BACKWARD = -0.5;
    // The sprint boost as it shows in a mob's speed attribute: the square root of a player's 30 percent, see FactionMember.
    private static final float SPRINT_BOOST = (float) Math.sqrt(1.3);

    FactionMoveControl(FactionMember mob) {
        super(mob);
    }

    // Turns the body toward its heading the way a walker leans into a turn: most of the way quickly, easing out at the end, and never snapping more than 25 degrees a tick.
    @Override
    protected float rotlerp(float from, float to, float max) {
        float diff = Mth.wrapDegrees(to - from);
        float limit = Math.min(max, MAX_TURN);
        float step = Math.abs(diff) <= MIN_TURN ? diff : Mth.clamp(diff * TURN_EASE, -limit, limit);
        if (Math.abs(step) < MIN_TURN && Math.abs(diff) > MIN_TURN) {
            step = Math.copySign(MIN_TURN, diff);
        }
        return from + step;
    }

    @Override
    public void tick() {
        if (this.mob.flight().airborne()) {
            this.operation = Operation.WAIT;
            this.mob.setZza(0.0f);
            this.mob.setXxa(0.0f);
            this.mob.setYya(0.0f);
            return;
        }
        if (this.operation != Operation.MOVE_TO || !this.mob.isAggressive()) {
            super.tick();
            return;
        }
        double dx = this.wantedX - this.mob.getX();
        double dz = this.wantedZ - this.mob.getZ();
        double dy = this.wantedY - this.mob.getY();
        double flat = dx * dx + dz * dz;
        boolean step = dy > this.mob.maxUpStep() && flat < Math.max(1.0f, this.mob.getBbWidth());
        if (flat + dy * dy < MIN_SPEED_SQR || step) {
            super.tick();
            return;
        }
        float facing = this.mob.getYHeadRot();
        double length = Math.sqrt(flat);
        double yaw = facing * Mth.DEG_TO_RAD;
        double forward = (dz * Mth.cos(yaw) - dx * Mth.sin(yaw)) / length;
        if (forward < BACKWARD && !this.backingOff()) {
            super.tick();
            return;
        }
        this.operation = Operation.WAIT;
        this.mob.setYRot(facing);
        float speed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
        double right = (dx * Mth.cos(yaw) + dz * Mth.sin(yaw)) / length;
        // A player only keeps the sprint boost while moving at least partly forward; stepping back or sideways it moves at a walk.
        if (this.mob.isSprinting() && forward <= 0.0) {
            speed /= SPRINT_BOOST;
        }
        this.mob.setSpeed(speed);
        this.mob.setZza((float) (forward * speed));
        this.mob.setXxa((float) (right * speed));
    }

    // Backing away is only for a foe close by and in sight; any longer way back it walks facing where it goes.
    private boolean backingOff() {
        LivingEntity target = this.mob.getTarget();
        return target != null && this.mob.distanceTo(target) < BACKPEDAL_RANGE && this.mob.getSensing().hasLineOfSight(target);
    }
}
