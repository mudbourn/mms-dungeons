package info.mudbourn.mmsdungeons.mobs.kobolds.ai;

import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;

// Keeps range and shoots a held bow at the target, strafing while it waits to draw.
public class KoboldBowGoal extends Goal {

    private static final int DRAW_TICKS = 20;
    private static final int STRAFE_TICKS = 20;
    private static final int LOST_SIGHT_TICKS = -60;

    private final AbstractKobold mob;
    private final double speed;
    private final int attackInterval;
    private final float radiusSqr;
    private int attackTime = -1;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public KoboldBowGoal(AbstractKobold mob, double speed, int attackInterval, float radius) {
        this.mob = mob;
        this.speed = speed;
        this.attackInterval = attackInterval;
        this.radiusSqr = radius * radius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && this.mob.isHolding(Items.BOW);
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.mob.setAggressive(true);
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.seeTime = 0;
        this.attackTime = -1;
        this.mob.stopUsingItem();
        this.mob.getMoveControl().strafe(0.0f, 0.0f);
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        double distanceSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSee = this.mob.getSensing().hasLineOfSight(target);
        if (canSee != (this.seeTime > 0)) {
            this.seeTime = 0;
        }
        this.seeTime += canSee ? 1 : -1;
        if (distanceSqr <= this.radiusSqr && this.seeTime >= DRAW_TICKS) {
            this.mob.getNavigation().stop();
            this.strafingTime++;
        } else {
            this.mob.getNavigation().moveTo(target, this.speed);
            this.strafingTime = -1;
        }
        this.strafe(target, distanceSqr);
        this.shoot(target, canSee);
    }

    private void strafe(LivingEntity target, double distanceSqr) {
        if (this.strafingTime >= STRAFE_TICKS) {
            if (this.mob.getRandom().nextFloat() < 0.3) {
                this.strafingClockwise = !this.strafingClockwise;
            }
            if (this.mob.getRandom().nextFloat() < 0.3) {
                this.strafingBackwards = !this.strafingBackwards;
            }
            this.strafingTime = 0;
        }
        if (this.strafingTime <= -1) {
            this.mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
            return;
        }
        if (distanceSqr > this.radiusSqr * 0.75f) {
            this.strafingBackwards = false;
        } else if (distanceSqr < this.radiusSqr * 0.25f) {
            this.strafingBackwards = true;
        }
        this.mob.getMoveControl().strafe(this.strafingBackwards ? -0.5f : 0.5f, this.strafingClockwise ? 0.5f : -0.5f);
        if (this.mob.getControlledVehicle() instanceof Mob vehicle) {
            vehicle.lookAt(target, 30.0f, 30.0f);
        }
        this.mob.lookAt(target, 30.0f, 30.0f);
    }

    private void shoot(LivingEntity target, boolean canSee) {
        if (this.mob.isUsingItem()) {
            if (!canSee && this.seeTime < LOST_SIGHT_TICKS) {
                this.mob.stopUsingItem();
            } else if (canSee) {
                int drawn = this.mob.getTicksUsingItem();
                if (drawn >= DRAW_TICKS) {
                    this.mob.stopUsingItem();
                    this.mob.performRangedAttack(target, BowItem.getPowerForTime(drawn));
                    this.attackTime = this.attackInterval;
                }
            }
        } else if (--this.attackTime <= 0 && this.seeTime >= LOST_SIGHT_TICKS) {
            this.mob.startUsingItem(this.mob.handHolding(stack -> stack.is(Items.BOW)));
        }
    }
}
