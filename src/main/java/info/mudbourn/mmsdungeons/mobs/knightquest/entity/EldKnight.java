package info.mudbourn.mmsdungeons.mobs.knightquest.entity;

import info.mudbourn.mmsdungeons.mobs.knightquest.KnightQuestMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

// A heavy armored knight that calls Eld Bombs at half health, then poisons and mends in a pulse.
public class EldKnight extends Monster {

    private static final int BOMBS = 3;
    private static final double BOMB_RING = 3.0;
    private static final int PULSE_TICKS = 80;
    private static final double POISON_RANGE = 3.5;
    private static final int POISON_TICKS = 200;
    private static final float HEAL = 3.0f;
    private static final float EXPLOSION_FACTOR = 0.15f;
    private static final float ARROW_FACTOR = 0.1f;

    private boolean summoned;
    private int pulse;

    public EldKnight(EntityType<? extends EldKnight> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 90.0)
            .add(Attributes.ATTACK_DAMAGE, 12.0)
            .add(Attributes.ATTACK_SPEED, 0.4)
            .add(Attributes.MOVEMENT_SPEED, 0.55)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.45, false));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.pulse++;
        if (!this.summoned && this.getHealth() < this.getMaxHealth() * 0.5) {
            this.summoned = true;
            this.summonBombs(level);
        }
        if (this.summoned && this.pulse > PULSE_TICKS) {
            this.poisonNearbyPlayers(level);
            if (this.getHealth() < this.getMaxHealth() * 0.75) {
                this.heal(HEAL);
            }
            this.pulse = 0;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypes.PLAYER_EXPLOSION) || source.is(DamageTypes.EXPLOSION)) {
            return super.hurtServer(level, source, amount * EXPLOSION_FACTOR);
        }
        if (source.is(DamageTypes.ARROW)) {
            return super.hurtServer(level, source, amount * ARROW_FACTOR);
        }
        return super.hurtServer(level, source, amount);
    }

    private void summonBombs(ServerLevel level) {
        double step = Math.toRadians(360.0 / BOMBS);
        boolean punched = false;
        for (int i = 0; i < BOMBS; i++) {
            EldBomb bomb = KnightQuestMobs.ELD_BOMB.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (bomb == null) {
                continue;
            }
            BlockPos spawn = this.bombSpot(level, BOMB_RING * Math.cos(step * i), BOMB_RING * Math.sin(step * i));
            bomb.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0.0f, 0.0f);
            level.addFreshEntity(bomb);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spawn.getX() + 0.5, spawn.getY() + 0.5, spawn.getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0.0);
            if (!punched) {
                this.punchPlayersAway(level, bomb);
                punched = true;
            }
        }
        level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() - 0.1, this.getZ(), 120, 4.0, 0.05, 4.0, 0.0);
        level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    private BlockPos bombSpot(ServerLevel level, double dx, double dz) {
        BlockPos spawn = this.blockPosition().offset((int) dx, 0, (int) dz);
        for (int divisor = 1; !level.getBlockState(spawn).isAir() && divisor <= BOMB_RING; divisor++) {
            spawn = this.blockPosition().offset((int) (dx / divisor), 0, (int) (dz / divisor));
        }
        return spawn;
    }

    private void punchPlayersAway(ServerLevel level, EldBomb bomb) {
        for (Player player : level.getEntitiesOfClass(Player.class, bomb.getBoundingBox().inflate(5.0))) {
            Vec3 push = player.position().subtract(this.position()).normalize().scale(1.5);
            player.push(push.x, push.y + 0.5, push.z);
            player.needsSync = true;
        }
    }

    private void poisonNearbyPlayers(ServerLevel level) {
        level.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 1.0, this.getZ(), 24, 1.5, 0.8, 1.5, 0.0);
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(POISON_RANGE))) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 1), this);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Summoned", this.summoned);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.summoned = input.getBooleanOr("Summoned", false);
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.AXOLOTL_SWIM;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 0.15f, 1.0f);
    }
}
