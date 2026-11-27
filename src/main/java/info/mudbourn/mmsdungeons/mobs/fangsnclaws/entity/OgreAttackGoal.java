package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

// Charges its target, swings in melee range and slams to close a gap.
public class OgreAttackGoal extends Goal {

    private static final double MIN_RANGE = 3.0;
    private static final double MAX_RANGE = 5.0;
    private static final double SLAM_RANGE = 6.0;
    private static final double SLAM_REACH = 4.0;
    private static final double CHASE_SPEED = 1.4;
    private static final double CLOSE_SPEED = 0.7;
    private static final int ATTACK_INTERVAL = 25;
    private static final int SLAM_COOLDOWN = 120;
    private static final int STUCK_SLAM_TICKS = 40;
    private static final int REPATH_TICKS = 20;

    private final Ogre ogre;
    private int attackCooldown;
    private int slamCooldown;
    private int stuckTicks;

    public OgreAttackGoal(Ogre ogre) {
        this.ogre = ogre;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.ogre.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void stop() {
        this.ogre.setRunning(false);
        this.ogre.getNavigation().stop();
        this.stuckTicks = 0;
        this.slamCooldown = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.ogre.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.slamCooldown > 0) {
            this.slamCooldown--;
        }
        this.ogre.getLookControl().setLookAt(target, 30.0f, 30.0f);
        if (this.ogre.isBusy()) {
            this.ogre.setRunning(false);
            if (this.ogre.isRooted()) {
                this.ogre.getNavigation().stop();
            }
            return;
        }
        this.trackStuck(target);
        if (this.trySlam(target) || this.ogre.useSpecialAttack(target)) {
            return;
        }
        this.approach(target);
    }

    private void trackStuck(LivingEntity target) {
        if (!this.ogre.getNavigation().isStuck()) {
            this.stuckTicks = 0;
            return;
        }
        this.stuckTicks++;
        if (this.ogre.onGround()) {
            this.ogre.getJumpControl().jump();
        }
        if (this.stuckTicks % REPATH_TICKS == 0) {
            this.ogre.getNavigation().moveTo(target, CHASE_SPEED);
        }
    }

    private boolean trySlam(LivingEntity target) {
        if (this.slamCooldown > 0) {
            return false;
        }
        double distance = this.ogre.distanceTo(target);
        boolean gap = distance > MAX_RANGE && distance <= SLAM_RANGE;
        boolean stuck = this.stuckTicks >= STUCK_SLAM_TICKS && !this.ogre.hasLineOfSight(target);
        if (!gap && !stuck) {
            return false;
        }
        Vec3 toTarget = target.position().subtract(this.ogre.position());
        Vec3 direction = toTarget.lengthSqr() > 0.0 ? toTarget.normalize() : this.ogre.getLookAngle();
        this.ogre.startSlam(this.ogre.position().add(direction.x * SLAM_REACH, 0.0, direction.z * SLAM_REACH));
        this.slamCooldown = SLAM_COOLDOWN;
        this.stuckTicks = 0;
        return true;
    }

    private void approach(LivingEntity target) {
        double distance = this.ogre.distanceTo(target);
        if (distance > MAX_RANGE || !this.ogre.hasLineOfSight(target)) {
            this.ogre.setRunning(true);
            this.ogre.getNavigation().moveTo(target, CHASE_SPEED);
            return;
        }
        this.ogre.setRunning(false);
        if (distance >= MIN_RANGE) {
            this.ogre.getNavigation().moveTo(target, CLOSE_SPEED);
        } else {
            this.ogre.getNavigation().stop();
        }
        if (this.attackCooldown <= 0) {
            this.attackCooldown = ATTACK_INTERVAL;
            this.ogre.startAttack(target);
        }
    }
}
