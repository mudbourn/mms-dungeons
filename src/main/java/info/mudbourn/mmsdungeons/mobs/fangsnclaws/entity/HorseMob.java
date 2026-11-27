package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

// A hostile horse that rears and kicks, and sometimes spawns with a rider.
public abstract class HorseMob extends StrikeMob {

    private static final double CHASE_SPEED = 1.2;
    private static final double STROLL_SPEED = 0.8;
    private static final float LOOK_RANGE = 8.0f;
    private static final int REAR_HIT_TICK = 5;
    private static final int REAR_TOTAL_TICKS = 25;
    private static final int REAR_COOLDOWN = 15;
    private static final float RIDER_CHANCE = 0.15f;

    protected HorseMob(EntityType<? extends HorseMob> type, Level level) {
        super(type, level);
    }

    // The mob that sometimes rides this horse, or null for none.
    protected @Nullable Mob createRider(ServerLevelAccessor level) {
        return null;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.registerAttackGoals();
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, STROLL_SPEED));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, LOOK_RANGE));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    protected void registerAttackGoals() {
        this.goalSelector.addGoal(2, new StrikeGoal(this, CHASE_SPEED));
    }

    @Override
    public void startAttack(LivingEntity target) {
        this.beginStrike(target, "rear", REAR_HIT_TICK, REAR_TOTAL_TICKS, REAR_COOLDOWN);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        boolean natural = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
        if (natural && this.random.nextFloat() < RIDER_CHANCE) {
            Mob rider = this.createRider(level);
            if (rider != null) {
                rider.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0f);
                rider.finalizeSpawn(level, difficulty, EntitySpawnReason.JOCKEY, null);
                rider.startRiding(this, false, false);
            }
        }
        return data;
    }
}
