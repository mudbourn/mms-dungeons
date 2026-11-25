package info.mudbourn.mmsdungeons.corpse;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Consumed on use: takes every item from the user's corpse and places chests holding them in front of the user.
public final class RecoveryCrossItem extends Item {

    private static final int CHEST_SLOTS = 27;

    public RecoveryCrossItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (level.isClientSide() || !(user instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        CorpseData.Corpse corpse = Corpses.take(serverLevel.getServer(), player.getUUID());
        if (corpse == null || corpse.items.isEmpty()) {
            player.sendSystemMessage(Component.literal("You have nothing to recover."), true);
            return InteractionResult.FAIL;
        }
        int chests = (corpse.items.size() + CHEST_SLOTS - 1) / CHEST_SLOTS;
        List<BlockPos> spots = findSpots(serverLevel, player, chests);
        if (spots.size() < chests) {
            Corpses.restore(player.getUUID(), corpse);
            player.sendSystemMessage(Component.literal("There is no room here to set down your belongings."), true);
            return InteractionResult.FAIL;
        }
        place(serverLevel, player, spots, corpse.items);
        player.getItemInHand(hand).consume(1, player);
        player.sendSystemMessage(Component.literal("Recovered belongings"), true);
        return InteractionResult.SUCCESS;
    }

    private static void place(ServerLevel level, ServerPlayer player, List<BlockPos> spots, List<ItemStack> items) {
        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, player.getDirection().getOpposite());
        int next = 0;
        for (BlockPos spot : spots) {
            level.setBlock(spot, chest, 3);
            if (level.getBlockEntity(spot) instanceof ChestBlockEntity entity) {
                for (int slot = 0; slot < CHEST_SLOTS && next < items.size(); slot++) {
                    entity.setItem(slot, items.get(next++));
                }
            }
        }
    }

    // Free spots with solid ground, searched outward in front of the player, left and right of their line of sight.
    private static List<BlockPos> findSpots(ServerLevel level, ServerPlayer player, int count) {
        Direction facing = player.getDirection();
        Direction right = facing.getClockWise();
        BlockPos base = player.blockPosition();
        int[] lateral = {0, 1, -1, 2, -2, 3, -3};
        int[] vertical = {0, 1, -1};
        List<BlockPos> spots = new ArrayList<>();
        for (int forward = 2; forward <= 5 && spots.size() < count; forward++) {
            for (int side : lateral) {
                for (int rise : vertical) {
                    BlockPos pos = base.relative(facing, forward).relative(right, side).above(rise);
                    if (spots.size() < count && isFree(level, pos)) {
                        spots.add(pos);
                        break;
                    }
                }
            }
        }
        return spots;
    }

    private static boolean isFree(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockPos below = pos.below();
        return state.canBeReplaced()
            && state.getFluidState().isEmpty()
            && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
