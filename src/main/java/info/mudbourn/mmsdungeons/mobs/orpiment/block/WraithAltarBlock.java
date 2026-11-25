package info.mudbourn.mmsdungeons.mobs.orpiment.block;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentItems;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentMobs;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentParticles;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// An altar that a Deepslate Seal wakes, summoning a Wraith two seconds later.
public class WraithAltarBlock extends Block {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty SUMMONED = BooleanProperty.create("summoned");

    private static final int SUMMON_DELAY = 40;

    public WraithAltarBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(SUMMONED, false)
        );
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, SUMMONED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
        if (!stack.is(OrpimentItems.SOUL_SEAL) || state.getValue(POWERED) || state.getValue(SUMMONED)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            level.setBlock(pos, state.setValue(POWERED, true), 3);
            level.playSound(null, pos, SoundEvents.DEEPSLATE_BRICKS_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 1.0, 1.0, 1.0, 0.1);
            level.scheduleTick(pos, this, SUMMON_DELAY);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 2.0;
        double z = pos.getZ() + 0.5;
        OrpimentMobs.WRAITH.spawn(level, pos.above(2), EntitySpawnReason.MOB_SUMMONED);
        level.sendParticles(ParticleTypes.POOF, x, y, z, 5, 1.0, 1.0, 1.0, 0.1);
        level.sendParticles(OrpimentParticles.WRAITH_SKULL, x, y, z, 10, 1.0, 1.0, 1.0, 0.1);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 5, 1.0, 1.0, 1.0, 0.1);
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 5, 1.0, 1.0, 1.0, 0.1);
        level.playSound(null, pos, OrpimentSounds.WRAITH_SPAWN, SoundSource.HOSTILE, 1.0f, 1.0f);
        level.setBlock(pos, state.setValue(SUMMONED, true).setValue(POWERED, false), 3);
    }
}
