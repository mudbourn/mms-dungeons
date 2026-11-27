package info.mudbourn.mmsdungeons.mobs.factions.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

// A lancer's passes: it climbs away from its target, turns, dives with the spear levelled so the spear's own charge hits on the way by, then pulls up and away for the next pass.
final class FactionJoust {

    private static final double ATTACK_HEIGHT = 8.0;
    private static final double ATTACK_RANGE = 40.0;
    private static final double TURN_DISTANCE = 16.0;
    private static final double OUTBOUND = 24.0;
    private static final double CIRCLE_HEIGHT = 14.0;
    private static final double PASS_END = 2.5;
    private static final double PASSED_RANGE = 8.0;
    private static final double STAB_RANGE = 3.0;
    private static final double MIN_PASS_SPEED = 0.7;
    private static final int PASS_TICKS = 120;
    private static final int PASS_GAP = 20;

    private final FactionMember mob;
    private boolean attacking;
    private boolean reached;
    private int passTicks;
    private int gap;
    private float healthBefore;

    FactionJoust(FactionMember mob) {
        this.mob = mob;
    }

    // Whether it is on a pass, diving at the target with the spear out.
    boolean attacking() {
        return this.attacking;
    }

    // Where to fly: at the target on a pass, otherwise out and up from it.
    Vec3 goal(ServerLevel level, LivingEntity target, double speed) {
        this.gap--;
        Vec3 aim = target.getBoundingBox().getCenter();
        Vec3 to = aim.subtract(this.mob.position());
        double distance = to.length();
        if (this.attacking) {
            this.passTicks++;
            this.reached |= distance <= STAB_RANGE;
            boolean passed = distance < PASS_END || to.dot(this.mob.getDeltaMovement()) < 0.0 && distance < PASSED_RANGE;
            if (passed || this.passTicks > PASS_TICKS || this.mob.consuming()) {
                this.finish(level, target);
                return this.outbound(target);
            }
            return aim;
        }
        boolean high = this.mob.getY() >= target.getY() + ATTACK_HEIGHT;
        boolean turned = to.horizontalDistance() >= TURN_DISTANCE;
        if (this.gap <= 0 && high && turned && distance <= ATTACK_RANGE && speed >= MIN_PASS_SPEED && !this.mob.consuming()) {
            this.begin(target);
            return aim;
        }
        return this.outbound(target);
    }

    // Puts the spear away and forgets the pass.
    void stop() {
        if (this.attacking) {
            this.mob.stopUsingItem();
        }
        this.attacking = false;
    }

    private void begin(LivingEntity target) {
        this.attacking = true;
        this.reached = false;
        this.passTicks = 0;
        this.healthBefore = target.getHealth();
        this.mob.startUsingItem(InteractionHand.MAIN_HAND);
    }

    // Ends the pass; a target the spear's charge did not hurt, though the pass came within reach, takes a plain hit.
    private void finish(ServerLevel level, LivingEntity target) {
        this.stop();
        this.gap = PASS_GAP;
        if (this.reached && target.isAlive() && target.getHealth() >= this.healthBefore) {
            this.mob.swing(InteractionHand.MAIN_HAND, this.mob.getMainHandItem().getAttackAnimation(), true);
            this.mob.doHurtTarget(level, target);
        }
    }

    // A point straight ahead of the way it is heading, high above the target.
    private Vec3 outbound(LivingEntity target) {
        Vec3 velocity = this.mob.getDeltaMovement();
        Vec3 heading = new Vec3(velocity.x, 0.0, velocity.z);
        if (heading.lengthSqr() < 1.0E-4) {
            heading = this.mob.getLookAngle().multiply(1.0, 0.0, 1.0);
        }
        heading = heading.normalize();
        return new Vec3(this.mob.getX() + heading.x * OUTBOUND, target.getY() + CIRCLE_HEIGHT, this.mob.getZ() + heading.z * OUTBOUND);
    }
}
