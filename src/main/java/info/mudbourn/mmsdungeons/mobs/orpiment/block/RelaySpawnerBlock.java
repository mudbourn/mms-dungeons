package info.mudbourn.mmsdungeons.mobs.orpiment.block;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentBlocks;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.entity.RelaySpawnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// Mirrors the nearest vanilla spawner, spawning its mob on the same timings from a second spot.
public class RelaySpawnerBlock extends Block implements EntityBlock {

    public static final BooleanProperty LINKED = BooleanProperty.create("linked");

    public RelaySpawnerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LINKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LINKED);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RelaySpawnerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != OrpimentBlocks.RELAY_SPAWNER_ENTITY) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> RelaySpawnerBlockEntity.serverTick(
            (ServerLevel) tickLevel,
            pos,
            tickState,
            (RelaySpawnerBlockEntity) entity
        );
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (dropExperience) {
            this.popExperience(level, pos, 15 + level.getRandom().nextInt(15) + level.getRandom().nextInt(15));
        }
    }
}
