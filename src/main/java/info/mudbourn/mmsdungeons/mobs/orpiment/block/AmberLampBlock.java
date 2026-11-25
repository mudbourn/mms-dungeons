package info.mudbourn.mmsdungeons.mobs.orpiment.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

// A lamp that holds one torch or soul torch, which a bare hand takes back out.
public class AmberLampBlock extends Block {

    public static final BooleanProperty TORCH_LIT = BooleanProperty.create("torch_lit");
    public static final BooleanProperty SOUL_TORCH_LIT = BooleanProperty.create("soul_torch_lit");

    public AmberLampBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TORCH_LIT, false).setValue(SOUL_TORCH_LIT, false));
    }

    public static int light(BlockState state) {
        if (state.getValue(TORCH_LIT)) {
            return 15;
        }
        return state.getValue(SOUL_TORCH_LIT) ? 10 : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TORCH_LIT, SOUL_TORCH_LIT);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (light(state) > 0) {
            return InteractionResult.PASS;
        }
        BooleanProperty property = stack.is(Items.TORCH) ? TORCH_LIT : stack.is(Items.SOUL_TORCH) ? SOUL_TORCH_LIT : null;
        if (property == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(property, true), 3);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        ItemStack torch = state.getValue(TORCH_LIT) ? new ItemStack(Items.TORCH) : new ItemStack(Items.SOUL_TORCH);
        if (light(state) == 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(TORCH_LIT, false).setValue(SOUL_TORCH_LIT, false), 3);
            level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!player.getInventory().add(torch)) {
                Block.popResource(level, pos, torch);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (light(state) > 0 && !movedByPiston) {
            Block.popResource(level, pos, new ItemStack(state.getValue(TORCH_LIT) ? Items.TORCH : Items.SOUL_TORCH));
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
