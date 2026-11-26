package info.mudbourn.mmsdungeons.mobs.knightquest.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A drifting ghost that shields nearby monsters, bumps players, and pops at any hit.
public class Ghosty extends Monster {

    private static final double SHIELD_RANGE = 7.0;
    private static final double CHASE_RANGE = 20.0;
    private static final double CHASE_SPEED = 0.1;
    private static final int PULSE_TICKS = 15;
    private static final int GLOW_TICKS = 25;
    private static final float BUMP_DAMAGE = 1.0f;

    private double angle;

    public Ghosty(EntityType<? extends Ghosty> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.ATTACK_DAMAGE, 0.0)
            .add(Attributes.ATTACK_SPEED, 0.0)
            .add(Attributes.MOVEMENT_SPEED, 0.63)
            .add(Attributes.FOLLOW_RANGE, 35.0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, this.blockPosition(), SoundEvents.FOX_SCREECH, SoundSource.HOSTILE, 1.0f, 1.0f);
        this.remove(Entity.RemovalReason.KILLED);
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            if (this.tickCount % PULSE_TICKS == 0) {
                this.level().addParticle(ParticleTypes.SOUL, this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), 0.0, 0.02, 0.0);
            }
            return;
        }
        if (level.isBrightOutside() && level.canSeeSky(this.blockPosition())) {
            this.setRemainingFireTicks(40);
        }
        if (this.tickCount % PULSE_TICKS == 0) {
            this.bumpNearbyPlayers(level);
        }
        this.shieldNearbyMonsters(level);
        this.drift(level);
    }

    private void bumpNearbyPlayers(ServerLevel level) {
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(1.0))) {
            if (!player.isCreative()) {
                player.hurtServer(level, this.damageSources().generic(), BUMP_DAMAGE);
                Vec3 push = player.position().subtract(this.position()).normalize().scale(0.1);
                player.push(push.x, push.y + 0.5, push.z);
                player.needsSync = true;
            }
        }
    }

    private void shieldNearbyMonsters(ServerLevel level) {
        for (Monster monster : level.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(SHIELD_RANGE))) {
            monster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1, 4, false, false, false));
            if (!(monster instanceof Ghosty) && this.tickCount % GLOW_TICKS == 0) {
                level.sendParticles(ParticleTypes.ENCHANT, monster.getX(), monster.getY() + 1.0, monster.getZ(), 4, 0.4, 0.6, 0.4, 0.0);
            }
        }
    }

    private void drift(ServerLevel level) {
        double bob = Math.sin(this.angle * 2.0) * 0.1;
        double x = this.getX();
        double y = this.getY() + bob;
        double z = this.getZ();
        Player target = level.getNearestPlayer(this, CHASE_RANGE);
        if (target != null && !target.isCreative()) {
            double dx = target.getX() - x;
            double dy = target.getY() + 0.5 - this.getY();
            double dz = target.getZ() - z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > 1.0) {
                x += dx / distance * CHASE_SPEED;
                y += dy / distance * CHASE_SPEED;
                z += dz / distance * CHASE_SPEED;
            }
        }
        this.setPos(x, y, z);
        Player watched = level.getNearestPlayer(this, -1.0);
        if (watched != null) {
            this.lookAt(watched, 360.0f, 360.0f);
        }
        this.angle = (this.angle + 0.1) % (Math.PI * 2);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean isInLava() {
        return false;
    }

    @Override
    public boolean isInWater() {
        return false;
    }

    @Override
    public boolean isInWall() {
        return false;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
    }

    @Override
    protected void playHurtSound(DamageSource source) {
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }
}
