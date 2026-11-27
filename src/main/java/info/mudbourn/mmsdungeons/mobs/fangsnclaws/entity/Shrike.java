package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// An owlbear of the cold that throws feather volleys and sweeps its prey with a blizzard.
public class Shrike extends Owlbear {

    private static final double FEATHER_FAN = Math.toRadians(50.0);
    private static final float FEATHER_SPEED = 2.6f;
    private static final float FEATHER_SPREAD = 0.5f;
    private static final double FEATHER_MIN_RANGE = 3.0;
    private static final double FEATHER_MAX_RANGE = 22.0;
    private static final int FEATHER_WINDUP = 8;
    private static final int FEATHER_COOLDOWN = 50;
    private static final double BLIZZARD_START_RANGE = 24.0;
    private static final double APPROACH_RANGE = 4.0;
    private static final double HOVER_ABOVE = 2.5;
    private static final double FLY_SPEED = 0.45;
    private static final int APPROACH_MAX_TICKS = 120;
    private static final int BLIZZARD_TICKS = 60;
    private static final int BLIZZARD_COOLDOWN = 200;
    private static final double BLIZZARD_LENGTH = 20.0;
    private static final double BLIZZARD_HALF_WIDTH = 4.0;
    private static final double BLIZZARD_UP = 3.0;
    private static final double BLIZZARD_DOWN = 5.0;
    private static final double BLIZZARD_PUSH = 0.12;
    private static final float BLIZZARD_DAMAGE = 3.0f;
    private static final int BLIZZARD_HIT_PERIOD = 10;
    private static final int SLOW_TICKS = 40;
    private static final int FREEZE_STEP = 16;

    private int blizzardCooldown;
    private int approachTicks;
    private int blizzardTicks;

    public Shrike(EntityType<? extends Shrike> type, Level level) {
        super(type, level);
    }

    @Override
    protected EntityType<? extends BabyMob> babyType() {
        return FangsNClawsMobs.BABY_SHRIKE;
    }

    @Override
    protected boolean divesWhenBlocked() {
        return false;
    }

    @Override
    protected boolean useFirstSwing() {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FeatherGoal());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.blizzardCooldown > 0) {
            this.blizzardCooldown--;
        }
        LivingEntity target = this.getTarget();
        boolean ready = target != null
            && target.isAlive()
            && this.blizzardCooldown == 0
            && !this.isBusy()
            && this.distanceTo(target) <= BLIZZARD_START_RANGE;
        if (ready) {
            this.startFlight();
        }
    }

    @Override
    protected void endFlight() {
        super.endFlight();
        this.approachTicks = 0;
        this.blizzardTicks = 0;
        this.blizzardCooldown = BLIZZARD_COOLDOWN;
    }

    @Override
    protected void flyAgainst(ServerLevel level, LivingEntity target) {
        Steering.faceToward(this, target.position());
        if (!this.isBlizzard()) {
            Vec3 above = new Vec3(target.getX(), target.getY() + HOVER_ABOVE, target.getZ());
            Steering.flyToward(this, above, FLY_SPEED, 0.15);
            this.setFlapping(true);
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            boolean close = Math.sqrt(dx * dx + dz * dz) <= APPROACH_RANGE;
            if (close || ++this.approachTicks >= APPROACH_MAX_TICKS) {
                this.setBlizzard(true);
                this.setFlapping(false);
                this.playSound(SoundEvents.PLAYER_HURT_FREEZE, 1.4f, 0.7f);
            }
            return;
        }
        Vec3 hover = new Vec3(this.getX(), target.getY() + HOVER_ABOVE, this.getZ());
        Steering.flyToward(this, hover, 0.2, 0.2);
        this.applyBlizzard(level);
        if (++this.blizzardTicks >= BLIZZARD_TICKS) {
            this.endFlight();
        }
    }

    private void applyBlizzard(ServerLevel level) {
        Vec3 look = this.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        Vec3 direction = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        boolean hits = this.blizzardTicks % BLIZZARD_HIT_PERIOD == 0;
        for (LivingEntity victim : level.getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox().inflate(BLIZZARD_LENGTH),
            entity -> entity != this && !(entity instanceof Owlbear)
        )) {
            Vec3 offset = victim.position().subtract(this.position());
            double along = offset.dot(direction);
            double across = Math.abs(offset.x * direction.z - offset.z * direction.x);
            boolean inside = along >= 0.0
                && along <= BLIZZARD_LENGTH
                && across <= BLIZZARD_HALF_WIDTH
                && offset.y >= -BLIZZARD_DOWN
                && offset.y <= BLIZZARD_UP;
            if (!inside) {
                continue;
            }
            victim.push(direction.x * BLIZZARD_PUSH, 0.05, direction.z * BLIZZARD_PUSH);
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, 0));
            victim.setTicksFrozen(Math.min(victim.getTicksFrozen() + FREEZE_STEP, victim.getTicksRequiredToFreeze()));
            if (hits) {
                victim.hurtServer(level, this.damageSources().mobAttack(this), BLIZZARD_DAMAGE);
            }
        }
        Vec3 front = this.position().add(direction.scale(BLIZZARD_LENGTH * 0.5)).add(0.0, 1.0, 0.0);
        level.sendParticles(ParticleTypes.SNOWFLAKE, front.x, front.y, front.z, 24, 4.0, 1.5, 4.0, 0.05);
    }

    private void shootFeathers(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 origin = new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.7, this.getZ());
        Vec3 aim = new Vec3(target.getX(), target.getY(0.5), target.getZ()).subtract(origin).normalize();
        for (int index = -1; index <= 1; index++) {
            Vec3 direction = aim.yRot((float) (FEATHER_FAN * index));
            FeatherProjectile feather = new FeatherProjectile(level, this);
            feather.setPos(origin.x, origin.y, origin.z);
            feather.shoot(direction.x, direction.y, direction.z, FEATHER_SPEED, FEATHER_SPREAD);
            level.addFreshEntity(feather);
        }
        this.playSound(SoundEvents.ARROW_SHOOT, 0.8f, 0.9f + this.random.nextFloat() * 0.2f);
    }

    private class FeatherGoal extends Goal {

        private LivingEntity target;
        private int windup;
        private boolean released;
        private long nextTime;

        FeatherGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Shrike shrike = Shrike.this;
            LivingEntity candidate = shrike.getTarget();
            boolean waiting = shrike.isBusy() || shrike.level().getGameTime() < this.nextTime;
            if (waiting || candidate == null || !candidate.isAlive()) {
                return false;
            }
            double distance = shrike.distanceTo(candidate);
            if (distance < FEATHER_MIN_RANGE || distance > FEATHER_MAX_RANGE || !shrike.hasLineOfSight(candidate)) {
                return false;
            }
            this.target = candidate;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.released && this.target != null && this.target.isAlive() && !Shrike.this.isFlying();
        }

        @Override
        public void start() {
            Shrike.this.getNavigation().stop();
            GeoTriggers.trigger(Shrike.this, "attack1");
            this.windup = FEATHER_WINDUP;
            this.released = false;
        }

        @Override
        public void tick() {
            Shrike.this.getNavigation().stop();
            Shrike.this.getLookControl().setLookAt(this.target, 30.0f, 30.0f);
            if (!this.released && --this.windup <= 0) {
                Shrike.this.shootFeathers(this.target);
                this.released = true;
                this.nextTime = Shrike.this.level().getGameTime() + FEATHER_COOLDOWN;
            }
        }

        @Override
        public void stop() {
            this.target = null;
        }
    }
}
