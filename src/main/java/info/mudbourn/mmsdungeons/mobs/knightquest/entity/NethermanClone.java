package info.mudbourn.mmsdungeons.mobs.knightquest.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// A frail copy of the Netherman that runs at players and bursts into frost when one gets close.
public class NethermanClone extends Monster {

    private static final double BURST_RANGE = 2.0;
    private static final double FREEZE_RANGE = 3.0;
    private static final int FREEZE_TICKS = 200;

    public NethermanClone(EntityType<? extends NethermanClone> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.ATTACK_SPEED, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 1.0)
            .add(Attributes.FOLLOW_RANGE, 100.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 5.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.4, false));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level && this.isAlive()) {
            for (Player player : level.players()) {
                if (!player.isSpectator() && this.distanceTo(player) <= BURST_RANGE) {
                    this.kill(level);
                    return;
                }
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.5, 0.8, 0.5, 0.1);
        for (Player player : level.players()) {
            if (this.distanceTo(player) <= FREEZE_RANGE) {
                player.setTicksFrozen(player.getTicksFrozen() + FREEZE_TICKS);
            }
        }
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
            level.broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_BLOCK_BREAK;
    }
}
