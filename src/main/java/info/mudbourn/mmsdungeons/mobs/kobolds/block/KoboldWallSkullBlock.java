package info.mudbourn.mmsdungeons.mobs.kobolds.block;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class KoboldWallSkullBlock extends WallSkullBlock {

    private final Supplier<EntityType<?>> summoned;

    public KoboldWallSkullBlock(SkullBlock.Type type, Properties properties, Supplier<EntityType<?>> summoned) {
        super(type, properties);
        this.summoned = summoned;
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block block,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        KoboldSkulls.summonIfPowered(level, pos, this.summoned.get());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        KoboldSkulls.summonIfOpen(level, pos, this.summoned.get());
    }
}
