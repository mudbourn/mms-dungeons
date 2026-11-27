package info.mudbourn.mmsdungeons.mobs.fangsnclaws.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// A block living things walk through that takes the look of any solid block used on it.
public class GhostBlock extends Block implements EntityBlock {

    public static final BooleanProperty HAS_MIMIC = BooleanProperty.create("has_mimic");

    private static final Vec3 SLOWDOWN = new Vec3(0.4, 0.4, 0.4);

    public GhostBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HAS_MIMIC, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_MIMIC);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GhostBlockEntity(pos, state);
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
        if (!(stack.getItem() instanceof BlockItem item)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        BlockState look = item.getBlock().defaultBlockState();
        if (!look.isSolid() || item.getBlock() instanceof GhostBlock) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GhostBlockEntity ghost) {
            ghost.setMimic(look);
            BlockState shown = state.setValue(HAS_MIMIC, true);
            level.setBlock(pos, shown, 3);
            level.sendBlockUpdated(pos, state, shown, 3);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(HAS_MIMIC)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof GhostBlockEntity ghost) {
                ghost.setMimic(null);
            }
            level.setBlock(pos, state.setValue(HAS_MIMIC, false), 3);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void entityInside(
        BlockState state,
        Level level,
        BlockPos pos,
        Entity entity,
        InsideBlockEffectApplier effects,
        boolean intersects
    ) {
        if (entity instanceof LivingEntity) {
            entity.makeStuckInBlock(state, SLOWDOWN);
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof LivingEntity) {
            return Shapes.empty();
        }
        return state.getShape(level, pos);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        return neighbor.is(this) || super.skipRendering(state, neighbor, direction);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
}
