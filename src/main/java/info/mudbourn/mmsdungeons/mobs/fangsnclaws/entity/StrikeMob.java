package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

// A monster that plays a named animation, then lands its blow on a set tick.
public abstract class StrikeMob extends Monster {

    private static final double HIT_SLACK = 0.8;
    private static final double MIN_SHOVE_DISTANCE = 1.0E-4;
    private static final double SHOVE_LIFT = 0.25;

    private LivingEntity strikeTarget;
    private int strikeTick;
    private int strikeHit;
    private int strikeTotal;
    private int strikeCooldown;

    protected StrikeMob(EntityType<? extends StrikeMob> type, Level level) {
        super(type, level);
    }

    // Starts this mob's blow against a target in reach.
    public abstract void startAttack(LivingEntity target);

    public boolean isStriking() {
        return this.strikeTick > 0;
    }

    public boolean isReadyToStrike() {
        return this.strikeTick == 0 && this.strikeCooldown == 0 && !this.isBusy();
    }

    // Whether the mob is mid-action and must not chase.
    public boolean isBusy() {
        return this.strikeTick > 0;
    }

    public double reach() {
        return this.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
    }

    protected void beginStrike(LivingEntity target, String animation, int hitTick, int totalTicks, int cooldown) {
        GeoTriggers.trigger(this, animation);
        this.strikeTarget = target;
        this.strikeTick = 1;
        this.strikeHit = hitTick;
        this.strikeTotal = totalTicks;
        this.strikeCooldown = totalTicks + cooldown;
    }

    // Lands the blow on a target still in reach.
    protected void landStrike(ServerLevel level, LivingEntity target) {
        super.doHurtTarget(level, target);
    }

    // Throws a target away from a point, along the ground and a little up.
    protected void shove(LivingEntity target, double strength, double fromX, double fromZ) {
        double dx = target.getX() - fromX;
        double dz = target.getZ() - fromZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < MIN_SHOVE_DISTANCE) {
            return;
        }
        target.push(dx / length * strength, SHOVE_LIFT, dz / length * strength);
        target.needsSync = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level) {
            if (this.strikeCooldown > 0) {
                this.strikeCooldown--;
            }
            this.tickStrike(level);
        }
    }

    private void tickStrike(ServerLevel level) {
        if (this.strikeTick <= 0) {
            return;
        }
        this.strikeTick++;
        if (this.strikeTick == this.strikeHit) {
            LivingEntity target = this.strikeTarget;
            if (target != null && target.isAlive() && this.distanceTo(target) <= this.reach() + HIT_SLACK) {
                this.landStrike(level, target);
            }
            this.strikeTarget = null;
        }
        if (this.strikeTick >= this.strikeTotal) {
            this.strikeTick = 0;
            this.strikeTarget = null;
        }
    }
}
