package info.mudbourn.mmsdungeons.mobs.luminous.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// A skeleton that cannot drown, in a blue, red or yellow coral hue picked at spawn.
public class SunkenSkeleton extends Skeleton {

    public static final int VARIANT_BLUE = 0;
    public static final int VARIANT_RED = 1;
    public static final int VARIANT_YELLOW = 2;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(
        SunkenSkeleton.class,
        EntityDataSerializers.INT
    );

    public SunkenSkeleton(EntityType<? extends SunkenSkeleton> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, VARIANT_BLUE);
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVariant(input.getIntOr("Variant", VARIANT_BLUE));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        this.setVariant(this.random.nextInt(3));
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return !source.is(DamageTypes.DROWN) && super.hurtServer(level, source, amount);
    }
}
