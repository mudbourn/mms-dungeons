package info.mudbourn.mmsdungeons.guild;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

// The quest board: bound to a guild when its guildmaster places it, opens that guild's quests and contracts.
public class QuestBoardBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public QuestBoardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuestBoardBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
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
