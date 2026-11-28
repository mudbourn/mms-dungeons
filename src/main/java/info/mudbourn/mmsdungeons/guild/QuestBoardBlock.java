package info.mudbourn.mmsdungeons.guild;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

// A 3 wide, 2 tall guild quest board whose bottom middle block holds its data and five invisible parts.
public class QuestBoardBlock extends HorizontalDirectionalBlock implements EntityBlock {

    // Column across the board, 0 to 2, with 1 the middle, and row, 0 or 1, of each block of the board.
    public static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, 2);
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, 1);
    private static final int MIDDLE = 1;

    public QuestBoardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(COLUMN, MIDDLE).setValue(ROW, 0));
    }

    public static boolean isMain(BlockState state) {
        return state.getValue(COLUMN) == MIDDLE && state.getValue(ROW) == 0;
    }

    // Where the block of this column and row sits, for a board whose main block is at main.
    private static BlockPos partPos(BlockPos main, Direction facing, int column, int row) {
        return main.relative(facing.getClockWise(), column - MIDDLE).above(row);
    }

    // The main block of the board a part of it belongs to; the main block itself for the main block.
    public static BlockPos mainPos(BlockPos pos, BlockState state) {
        return partPos(pos, state.getValue(FACING), MIDDLE * 2 - state.getValue(COLUMN), 0).below(state.getValue(ROW));
    }

    // Places only where all six blocks of the board have room.
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos main = context.getClickedPos();
        Level level = context.getLevel();
        for (int column = 0; column <= 2; column++) {
            for (int row = 0; row <= 1; row++) {
                BlockPos part = partPos(main, facing, column, row);
                if (!part.equals(main) && (!level.getBlockState(part).canBeReplaced(context) || !level.isInsideBuildHeight(part))) {
                    return null;
                }
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLUMN, ROW);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isMain(state) ? new QuestBoardBlockEntity(pos, state) : null;
    }

    // A part goes when the main block it belongs to is gone, so the whole board falls together.
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (isMain(state)) {
            return state;
        }
        BlockState main = level.getBlockState(mainPos(pos, state));
        return main.is(this) && isMain(main) ? state : Blocks.AIR.defaultBlockState();
    }

    // Breaks the whole board from its main block so it drops one item.
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos main = mainPos(pos, state);
            Direction facing = state.getValue(FACING);
            for (int column = 0; column <= 2; column++) {
                for (int row = 0; row <= 1; row++) {
                    BlockPos part = partPos(main, facing, column, row);
                    if (!part.equals(main) && !part.equals(pos) && level.getBlockState(part).is(this)) {
                        level.setBlock(part, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            }
            if (!isMain(state)) {
                level.destroyBlock(main, !player.isCreative(), player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        for (int column = 0; column <= 2; column++) {
            for (int row = 0; row <= 1; row++) {
                if (column != MIDDLE || row != 0) {
                    level.setBlock(partPos(pos, facing, column, row), state.setValue(COLUMN, column).setValue(ROW, row), Block.UPDATE_ALL);
                }
            }
        }
        if (level.isClientSide()
            || !(placer instanceof ServerPlayer player)
            || !(level.getBlockEntity(pos) instanceof QuestBoardBlockEntity board)) {
            return;
        }
        Guild guild = Guilds.data().guildOf(player.getUUID());
        if (guild != null && player.getUUID().equals(guild.master)) {
            board.bind(guild.id);
            player.sendSystemMessage(Component.literal(
                "This quest board now lists the quests and contracts of " + guild.name + "."));
        } else {
            player.sendSystemMessage(Component.literal(
                "This quest board is not bound to a guild. A guildmaster or an admin can bind it with /guild bind."));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        pos = mainPos(pos, state);
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof QuestBoardBlockEntity board) {
            Guild guild = board.guild() == null ? null : Guilds.data().byId(board.guild());
            if (guild == null) {
                serverPlayer.sendOverlayMessage(Component.literal("This quest board is not bound to a guild."));
            } else {
                GuildNetworking.open(serverPlayer, guild, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
