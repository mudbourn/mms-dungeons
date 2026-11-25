package info.mudbourn.mmsdungeons.mobs.orpiment.block;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentBlocks;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentEffects;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentMobs;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.TarCube;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public class TarBlock extends LiquidBlock {

    private static final float CUBE_CHANCE = 6.8e-4f;

    public TarBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (random.nextFloat() < CUBE_CHANCE && isPool(level, pos)) {
            OrpimentMobs.TAR_CUBE.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
        }
    }

    @Override
    protected void entityInside(
        BlockState state,
        Level level,
        BlockPos pos,
        Entity entity,
        InsideBlockEffectApplier applier,
        boolean intersects
    ) {
        super.entityInside(state, level, pos, entity, applier, intersects);
        if (!level.isClientSide() && entity instanceof LivingEntity living && !(entity instanceof TarCube)) {
            living.addEffect(new MobEffectInstance(OrpimentEffects.TARRED, 100, 0));
        }
    }

    private static boolean isPool(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(pos.relative(direction)).is(OrpimentBlocks.TAR)) {
                return false;
            }
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction, 2)).is(OrpimentBlocks.TARRED_SAND)) {
                return true;
            }
        }
        return false;
    }
}
