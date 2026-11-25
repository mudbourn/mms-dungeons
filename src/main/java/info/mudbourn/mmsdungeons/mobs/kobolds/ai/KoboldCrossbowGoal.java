package info.mudbourn.mmsdungeons.mobs.kobolds.ai;

import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import java.util.EnumSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;

// Closes to range, loads a held crossbow, waits a beat and fires at the target.
public class KoboldCrossbowGoal extends Goal {

    private static final UniformInt PATH_DELAY = TimeUtil.rangeOfSeconds(1, 2);
    private static final int MIN_SIGHT_TICKS = 5;

    private final AbstractKobold mob;
    private final double speed;
    private final float radiusSqr;
    private State state = State.UNCHARGED;
    private int seeTime;
    private int attackDelay;
    private int pathDelay;

    public KoboldCrossbowGoal(AbstractKobold mob, double speed, float radius) {
        this.mob = mob;
        this.speed = speed;
        this.radiusSqr = radius * radius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.hasTarget() && this.mob.isHolding(Items.CROSSBOW);
    }

    @Override
    public boolean canContinueToUse() {
        return this.hasTarget() && (this.canUse() || !this.mob.getNavigation().isDone()) && this.mob.isHolding(Items.CROSSBOW);
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
        this.mob.setTarget(null);
        this.seeTime = 0;
        if (this.mob.isUsingItem()) {
            this.mob.stopUsingItem();
            this.mob.setChargingCrossbow(false);
            this.mob.getUseItem().set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        }
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
        boolean canSee = this.mob.getSensing().hasLineOfSight(target);
        if (canSee != (this.seeTime > 0)) {
            this.seeTime = 0;
        }
        this.seeTime += canSee ? 1 : -1;
        boolean approach = (this.mob.distanceToSqr(target) > this.radiusSqr || this.seeTime < MIN_SIGHT_TICKS) && this.attackDelay == 0;
        if (approach) {
            this.pathDelay--;
            if (this.pathDelay <= 0) {
                this.mob.getNavigation().moveTo(target, this.state == State.UNCHARGED ? this.speed : this.speed * 0.5);
                this.pathDelay = PATH_DELAY.sample(this.mob.getRandom());
            }
        } else {
            this.pathDelay = 0;
            this.mob.getNavigation().stop();
        }
        this.mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
        this.advance(target, canSee, approach);
    }

    private void advance(LivingEntity target, boolean canSee, boolean approach) {
        switch (this.state) {
            case UNCHARGED -> {
                if (!approach) {
                    this.mob.startUsingItem(this.mob.handHolding(stack -> stack.is(Items.CROSSBOW)));
                    this.state = State.CHARGING;
                    this.mob.setChargingCrossbow(true);
                }
            }
            case CHARGING -> {
                if (!this.mob.isUsingItem()) {
                    this.state = State.UNCHARGED;
                }
                if (this.mob.getTicksUsingItem() >= CrossbowItem.getChargeDuration(this.mob.getUseItem(), this.mob)) {
                    this.mob.releaseUsingItem();
                    this.state = State.CHARGED;
                    this.attackDelay = 20 + this.mob.getRandom().nextInt(20);
                    this.mob.setChargingCrossbow(false);
                }
            }
            case CHARGED -> {
                this.attackDelay--;
                if (this.attackDelay == 0) {
                    this.state = State.READY;
                }
            }
            case READY -> {
                if (canSee) {
                    this.mob.performRangedAttack(target, 1.0f);
                    this.state = State.UNCHARGED;
                }
            }
        }
    }

    private boolean hasTarget() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive();
    }

    private enum State {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY
    }
}
