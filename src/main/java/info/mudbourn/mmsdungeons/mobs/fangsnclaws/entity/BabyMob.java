package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// A harmless young mob that flees when hurt and grows into its adult kind.
public abstract class BabyMob extends PathfinderMob {

    private static final EntityDataAccessor<Integer> VARIANT =
        SynchedEntityData.defineId(BabyMob.class, EntityDataSerializers.INT);

    private static final int GROW_TICKS = 24000;
    private static final double PANIC_SPEED = 1.5;
    private static final double STROLL_SPEED = 0.8;
    private static final float LOOK_RANGE = 6.0f;
    private static final double OFFSET = 0.4;

    private int growth;

    protected BabyMob(EntityType<? extends BabyMob> type, Level level) {
        super(type, level);
    }

    // The adult this young mob grows into.
    protected abstract EntityType<? extends Mob> adultType();

    // Hands the young mob's traits to its grown form.
    protected void onGrown(Mob adult) {
    }

    // Spawns a young mob of a type beside its parent.
    public static void spawnBeside(
        ServerLevelAccessor level,
        Mob parent,
        EntityType<? extends BabyMob> type,
        int variant
    ) {
        BabyMob baby = type.create(level.getLevel(), EntitySpawnReason.NATURAL);
        if (baby == null) {
            return;
        }
        baby.snapTo(
            parent.getX() + (parent.getRandom().nextDouble() - 0.5) * OFFSET,
            parent.getY(),
            parent.getZ() + (parent.getRandom().nextDouble() - 0.5) * OFFSET,
            parent.getYRot(),
            0.0f
        );
        baby.setVariant(variant);
        level.addFreshEntity(baby);
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, PANIC_SPEED));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, STROLL_SPEED));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, LOOK_RANGE));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel && ++this.growth >= GROW_TICKS) {
            this.growUp(this.adultType());
        }
    }

    private <T extends Mob> void growUp(EntityType<T> type) {
        if (this.isDeadOrDying()) {
            return;
        }
        this.convertTo(type, ConversionParams.single(this, true, false), EntitySpawnReason.CONVERSION, this::onGrown);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Growth", this.growth);
        output.putInt("Variant", this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.growth = input.getIntOr("Growth", 0);
        this.setVariant(input.getIntOr("Variant", 0));
    }
}
