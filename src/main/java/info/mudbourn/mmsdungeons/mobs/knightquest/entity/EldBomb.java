package info.mudbourn.mmsdungeons.mobs.knightquest.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// A creeper that bursts into a poison cloud.
public class EldBomb extends Creeper {

    private static final double POISON_RANGE = 3.5;
    private static final int POISON_TICKS = 160;
    private static final float SWELL_SPAN = 28.0f;

    public EldBomb(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 7.0)
            .add(Attributes.ATTACK_DAMAGE, 0.5)
            .add(Attributes.ATTACK_SPEED, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SwellGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, PolarBear.class, 35.0f, 1.0, 0.5));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 0.5, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    // Fuse ticks built up so far.
    public float swellTicks(float partialTick) {
        return this.getSwelling(partialTick) * SWELL_SPAN;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level && this.dead && this.isRemoved() && this.getHealth() > 0.0f) {
            this.burst(level);
        }
    }

    private void burst(ServerLevel level) {
        level.sendParticles(ParticleTypes.SNEEZE, this.getX(), this.getY() + 1.0, this.getZ(), 40, 1.2, 1.2, 1.2, 0.05);
        level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() - 0.2, this.getZ(), 12, 1.0, 0.1, 1.0, 0.02);
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(POISON_RANGE))) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0), this);
        }
    }
}
