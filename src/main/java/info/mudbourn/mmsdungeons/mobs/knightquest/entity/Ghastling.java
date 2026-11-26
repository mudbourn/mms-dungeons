package info.mudbourn.mmsdungeons.mobs.knightquest.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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

// A shield spirit that circles an angry Gremlin and pops into a shove when hit.
public class Ghastling extends Monster {

    private static final double ORBIT_RADIUS = 2.0;
    private static final double ORBIT_SPEED = 0.1;
    private static final double OWNER_RANGE = 4.0;
    private static final double SHOVE_RANGE = 3.0;

    private double angle;

    public Ghastling(EntityType<? extends Ghastling> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.ATTACK_SPEED, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.63)
            .add(Attributes.FOLLOW_RANGE, 35.0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, this.blockPosition(), SoundEvents.VEX_HURT, SoundSource.HOSTILE, 1.0f, 1.0f);
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(SHOVE_RANGE))) {
            Vec3 push = player.position().subtract(this.position()).normalize().scale(0.4);
            player.push(push.x, push.y + 0.5, push.z);
            player.needsSync = true;
        }
        this.remove(Entity.RemovalReason.KILLED);
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), 0.0, 0.0, 0.0);
            return;
        }
        Gremlin owner = this.closestGremlin();
        if (owner == null || owner.isDeadOrDying() || owner.getPhase() == 1) {
            this.remove(Entity.RemovalReason.KILLED);
            return;
        }
        Vec3 center = owner.position();
        this.setPos(
            center.x + Math.cos(this.angle) * ORBIT_RADIUS,
            center.y + 0.5 + Math.sin(this.angle * 2.0) * 0.5,
            center.z + Math.sin(this.angle) * ORBIT_RADIUS
        );
        Player watched = this.level().getNearestPlayer(this, -1.0);
        if (watched != null) {
            this.lookAt(watched, 360.0f, 360.0f);
        }
        this.angle = (this.angle + ORBIT_SPEED) % (Math.PI * 2);
    }

    private @Nullable Gremlin closestGremlin() {
        Gremlin closest = null;
        double best = Double.MAX_VALUE;
        for (Gremlin gremlin : this.level().getEntitiesOfClass(Gremlin.class, this.getBoundingBox().inflate(OWNER_RANGE))) {
            double distance = this.distanceToSqr(gremlin);
            if (distance < best) {
                best = distance;
                closest = gremlin;
            }
        }
        return closest;
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
