package info.mudbourn.mmsdungeons.mobs.orpiment.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

// Stone roots that hang from a ceiling, a tip below a run of middle pieces.
public class PetrifiedRootsBlock extends Block implements BonemealableBlock {

    public static final EnumProperty<Thickness> THICKNESS = EnumProperty.create("thickness", Thickness.class);

    public PetrifiedRootsBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(THICKNESS, Thickness.TIP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Thickness thickness = thickness(context.getLevel(), context.getClickedPos());
        return thickness == null ? null : this.defaultBlockState().setValue(THICKNESS, thickness);
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess ticks,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random
    ) {
        if (direction != Direction.UP && direction != Direction.DOWN) {
            return state;
        }
        Thickness thickness = thickness(level, pos);
        return thickness == null ? Blocks.AIR.defaultBlockState() : state.setValue(THICKNESS, thickness);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        return aboveState.is(this) || aboveState.getBlock() instanceof LeavesBlock || aboveState.isFaceSturdy(level, above, Direction.DOWN);
    }

    private @Nullable Thickness thickness(LevelReader level, BlockPos pos) {
        if (!this.canSurvive(this.defaultBlockState(), level, pos)) {
            return null;
        }
        return level.getBlockState(pos.below()).is(this) ? Thickness.MIDDLE : Thickness.TIP;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return state.getValue(THICKNESS) == Thickness.TIP;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.isAir() || belowState.canBeReplaced()) {
            level.setBlock(below, this.defaultBlockState().setValue(THICKNESS, Thickness.TIP), 3);
            level.setBlock(pos, state.setValue(THICKNESS, Thickness.MIDDLE), 3);
        }
    }

    public enum Thickness implements StringRepresentable {
        TIP("tip"),
        MIDDLE("middle");

        private final String name;

        Thickness(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
