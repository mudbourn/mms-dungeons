package info.mudbourn.mmsdungeons.mobs.knightquest.entity;

import info.mudbourn.mmsdungeons.dungeon.BossBarAggro;
import java.util.Set;
import java.util.HashSet;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.netherman.NethermanBarrageGoal;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.netherman.NethermanCastGoal;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.netherman.NethermanPullGoal;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.netherman.NethermanReturnGoal;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.netherman.NethermanSpots;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.netherman.NethermanTargetGoal;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The Netherman: a boss that changes form at two-thirds and one-third health, casting a new set of attacks in each.
public class Netherman extends Monster {

    public static final int SWITCH_2_TICKS = 130;
    public static final int SWITCH_3_TICKS = 160;
    public static final int ACTION_NONE = 0;
    public static final int ACTION_FLAME = 1;
    public static final int ACTION_CLONES = 2;
    public static final int ACTION_DARKNESS = 3;

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(Netherman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SUMMONING = SynchedEntityData.defineId(Netherman.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(Netherman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SWITCH_2 = SynchedEntityData.defineId(Netherman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SWITCH_3 = SynchedEntityData.defineId(Netherman.class, EntityDataSerializers.INT);

    private static final int SUMMON_TICKS = 100;
    private static final int GRACE_TICKS = 40;
    private static final int ACTION_TICKS = 30;
    private static final int DEATH_TICKS = 80;
    private static final int XP_PULSE_TICKS = 10;
    private static final int XP_PER_PULSE = 40;
    private static final double PHASE_2_HEALTH = 0.66;
    private static final double PHASE_3_HEALTH = 0.33;
    private static final double HIT_TELEPORT_RANGE = 20.0;

    private record TempLava(BlockPos pos, BlockState original, int ticksLeft) {

        private static final Codec<TempLava> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(TempLava::pos),
                BlockState.CODEC.fieldOf("original").forGetter(TempLava::original),
                Codec.INT.fieldOf("ticks_left").forGetter(TempLava::ticksLeft)
            ).apply(instance, TempLava::new)
        );
    }

    private static final int LAVA_TICKS = 100;

    private final ServerBossEvent bossEvent;
    private final Set<UUID> fought = new HashSet<>();
    private final List<TempLava> tempLava = new ArrayList<>();
    private boolean sealed = true;
    private boolean rooted = true;
    private boolean searching;
    private int actionTicks;

    public Netherman(EntityType<? extends Netherman> type, Level level) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(UUID.randomUUID(), this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        this.bossEvent.setDarkenScreen(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 450.0)
            .add(Attributes.ATTACK_DAMAGE, 16.0)
            .add(Attributes.ATTACK_SPEED, 1.2)
            .add(Attributes.MOVEMENT_SPEED, 0.8)
            .add(Attributes.FOLLOW_RANGE, 75.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 5.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
        builder.define(SUMMONING, true);
        builder.define(ACTION, ACTION_NONE);
        builder.define(SWITCH_2, 0);
        builder.define(SWITCH_3, 0);
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    public boolean isSummoning() {
        return this.entityData.get(SUMMONING);
    }

    public int getAction() {
        return this.entityData.get(ACTION);
    }

    public int getSwitch2() {
        return this.entityData.get(SWITCH_2);
    }

    public int getSwitch3() {
        return this.entityData.get(SWITCH_3);
    }

    // Whether the given phase is current and its form switch has finished.
    public boolean isFormReady(int phase) {
        return switch (phase) {
            case 1 -> this.getPhase() == 1;
            case 2 -> this.getPhase() == 2 && this.getSwitch2() >= SWITCH_2_TICKS;
            default -> this.getPhase() == 3 && this.getSwitch3() >= SWITCH_3_TICKS;
        };
    }

    // Whether any phase is current and not mid-switch.
    public boolean isSettled() {
        return this.isFormReady(1) || this.isFormReady(2) || this.isFormReady(3);
    }

    public boolean isSearching() {
        return this.searching;
    }

    // Holds the boss in place for a cast, with the animation of the given action.
    public void beginAction(int action) {
        this.rooted = true;
        this.actionTicks = 0;
        this.entityData.set(ACTION, action);
    }

    public void teleportNear(LivingEntity target, double minRange, double maxRange) {
        Vec3 spot = NethermanSpots.ringSpot(this.level(), this, target, minRange, maxRange);
        if (spot != null) {
            NethermanSpots.teleport(this, spot);
            this.getNavigation().stop();
        }
    }

    // Turns a plain solid block to lava for a few seconds, then back.
    public void placeTempLava(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasBlockEntity() || state.isAir() || !state.isSolid() || state.getDestroySpeed(level, pos) < 0.0f) {
            return;
        }
        this.tempLava.add(new TempLava(pos.immutable(), state, LAVA_TICKS));
        level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
    }

    private void tickLava(ServerLevel level) {
        List<TempLava> next = new ArrayList<>();
        for (TempLava lava : this.tempLava) {
            if (lava.ticksLeft() <= 1) {
                this.revert(level, lava);
            } else {
                next.add(new TempLava(lava.pos(), lava.original(), lava.ticksLeft() - 1));
            }
        }
        this.tempLava.clear();
        this.tempLava.addAll(next);
    }

    private void revertAllLava(ServerLevel level) {
        this.tempLava.forEach(lava -> this.revert(level, lava));
        this.tempLava.clear();
    }

    private void revert(ServerLevel level, TempLava lava) {
        if (level.getBlockState(lava.pos()).is(Blocks.LAVA)) {
            level.setBlock(lava.pos(), lava.original(), 3);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.5, true));
        this.goalSelector.addGoal(2, new NethermanReturnGoal(this));
        this.goalSelector.addGoal(2, new NethermanPullGoal(this));
        this.goalSelector.addGoal(2, new NethermanBarrageGoal(this, NethermanBarrageGoal.Kind.LAVA));
        this.goalSelector.addGoal(2, new NethermanBarrageGoal(this, NethermanBarrageGoal.Kind.CHARGES));
        this.goalSelector.addGoal(3, new NethermanCastGoal(this, NethermanCastGoal.Spell.FLAME));
        this.goalSelector.addGoal(3, new NethermanCastGoal(this, NethermanCastGoal.Spell.ICE));
        this.goalSelector.addGoal(3, new NethermanCastGoal(this, NethermanCastGoal.Spell.CLONES));
        this.goalSelector.addGoal(3, new NethermanCastGoal(this, NethermanCastGoal.Spell.DARKNESS));
        this.targetSelector.addGoal(1, new NethermanTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.tickLava(level);
        if (this.tickCount == SUMMON_TICKS) {
            this.entityData.set(SUMMONING, false);
            this.rooted = false;
            this.sealed = false;
            this.searching = true;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        BossBarAggro.sync(this.bossEvent, this, this.fought, player -> true);
        if (this.isOnFire()) {
            this.clearFire();
        }
        if (this.rooted) {
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
        }
        this.tickAction();
        this.tickPhases();
    }

    private void tickAction() {
        if (this.getAction() == ACTION_NONE || !this.rooted) {
            return;
        }
        this.actionTicks++;
        if (this.actionTicks >= ACTION_TICKS) {
            this.rooted = false;
            this.actionTicks = 0;
            this.entityData.set(ACTION, ACTION_NONE);
        }
    }

    private void tickPhases() {
        double health = this.getHealth() / this.getMaxHealth();
        if (health < PHASE_2_HEALTH && this.getPhase() == 1) {
            this.beginSwitch(2);
        }
        if (this.getPhase() == 2 && this.getSwitch2() < SWITCH_2_TICKS) {
            this.entityData.set(SWITCH_2, this.getSwitch2() + 1);
            if (this.getSwitch2() == SWITCH_2_TICKS) {
                this.endSwitch();
            }
        }
        if (health < PHASE_3_HEALTH && this.getPhase() != 3) {
            this.beginSwitch(3);
        }
        if (this.getPhase() == 3 && this.getSwitch3() < SWITCH_3_TICKS) {
            this.entityData.set(SWITCH_3, this.getSwitch3() + 1);
            if (this.getSwitch3() == SWITCH_3_TICKS) {
                this.endSwitch();
            }
        }
    }

    private void beginSwitch(int phase) {
        this.entityData.set(PHASE, phase);
        this.entityData.set(ACTION, ACTION_NONE);
        this.actionTicks = 0;
        this.rooted = true;
        this.searching = false;
        this.sealed = true;
        this.setTarget(null);
    }

    private void endSwitch() {
        this.searching = true;
        this.rooted = false;
        this.sealed = false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        if (this.sealed || this.tickCount < GRACE_TICKS || this.resists(source)) {
            return false;
        }
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && this.isAlive()) {
            this.teleportAway(level);
        }
        return hurt;
    }

    private boolean resists(DamageSource source) {
        boolean fire = source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.LAVA);
        return source.is(DamageTypes.EXPLOSION)
            || source.is(DamageTypes.PLAYER_EXPLOSION)
            || fire && this.getPhase() == 1
            || source.is(DamageTypes.LIGHTNING_BOLT) && this.getPhase() == 3;
    }

    private void teleportAway(ServerLevel level) {
        Vec3 spot = NethermanSpots.standingSpot(
            level,
            this,
            this.getX() + (this.random.nextDouble() - 0.5) * 2.0 * HIT_TELEPORT_RANGE,
            this.getY() + this.random.nextInt(16),
            this.getZ() + (this.random.nextDouble() - 0.5) * 2.0 * HIT_TELEPORT_RANGE,
            24
        );
        if (spot != null) {
            NethermanSpots.teleport(this, spot);
            this.getNavigation().stop();
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            this.revertAllLava(level);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason != RemovalReason.UNLOADED_TO_CHUNK && reason != RemovalReason.UNLOADED_WITH_PLAYER && this.level() instanceof ServerLevel level) {
            this.revertAllLava(level);
        }
        super.remove(reason);
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) {
            return;
        }
        if (this.deathTime % XP_PULSE_TICKS == 0) {
            ExperienceOrb.award(level, this.position(), XP_PER_PULSE);
        }
        if (this.deathTime >= DEATH_TICKS) {
            level.broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Age", this.tickCount);
        output.putInt("Phase", this.getPhase());
        output.putInt("Switch2", this.getSwitch2());
        output.putInt("Switch3", this.getSwitch3());
        output.putBoolean("Summoning", this.isSummoning());
        output.putBoolean("Sealed", this.sealed);
        output.putBoolean("Rooted", this.rooted);
        output.putBoolean("Searching", this.searching);
        output.putInt("Action", this.getAction());
        output.putInt("ActionTicks", this.actionTicks);
        output.store("TempLava", TempLava.CODEC.listOf(), this.tempLava);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.tickCount = input.getIntOr("Age", 0);
        this.entityData.set(PHASE, input.getIntOr("Phase", 1));
        this.entityData.set(SWITCH_2, input.getIntOr("Switch2", 0));
        this.entityData.set(SWITCH_3, input.getIntOr("Switch3", 0));
        this.entityData.set(SUMMONING, input.getBooleanOr("Summoning", true));
        this.sealed = input.getBooleanOr("Sealed", true);
        this.rooted = input.getBooleanOr("Rooted", true);
        this.searching = input.getBooleanOr("Searching", false);
        this.entityData.set(ACTION, input.getIntOr("Action", ACTION_NONE));
        this.actionTicks = input.getIntOr("ActionTicks", 0);
        this.tempLava.clear();
        input.read("TempLava", TempLava.CODEC.listOf()).ifPresent(this.tempLava::addAll);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }
}
