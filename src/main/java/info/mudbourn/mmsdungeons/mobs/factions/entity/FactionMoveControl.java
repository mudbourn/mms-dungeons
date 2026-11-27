package info.mudbourn.mmsdungeons.mobs.factions.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

// Walks a path the way a player fights: facing where it looks and stepping forward, back or sideways along the path. Flying and downed members leave movement to their own code.
final class FactionMoveControl extends MoveControl<FactionMember> {

    FactionMoveControl(FactionMember mob) {
        super(mob);
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
        this.operation = Operation.WAIT;
        float facing = this.mob.getYHeadRot();
        this.mob.setYRot(facing);
        float speed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
        double length = Math.sqrt(flat);
        double yaw = facing * Mth.DEG_TO_RAD;
        double forward = (dz * Mth.cos(yaw) - dx * Mth.sin(yaw)) / length;
        double right = (dx * Mth.cos(yaw) + dz * Mth.sin(yaw)) / length;
        this.mob.setSpeed(speed);
        this.mob.setZza((float) (forward * speed));
        this.mob.setXxa((float) (right * speed));
    }
}
