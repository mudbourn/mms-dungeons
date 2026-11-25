package info.mudbourn.mmsdungeons.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// The portal block inside a bedrock frame, drawn like the end portal. Standing in it travels like a nether portal.
public class DungeonPortalBlock extends Block implements EntityBlock, Portal {

    // The axis the portal faces, so it can lie in any of the three planes.
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    // Four pixels thick, like a nether portal.
    private static final VoxelShape FACING_X = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    private static final VoxelShape FACING_Y = Block.box(0.0, 6.0, 0.0, 16.0, 10.0, 16.0);
    private static final VoxelShape FACING_Z = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);

    public DungeonPortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> FACING_X;
            case Y -> FACING_Y;
            case Z -> FACING_Z;
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonPortalBlockEntity(pos, state);
    }

    // The block entity renderer draws it, so the block model is never shown in the world.
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
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
        if (entity instanceof ServerPlayer && entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        if (entity instanceof ServerPlayer player) {
            int delay = player.getAbilities().invulnerable
                ? level.getGameRules().get(GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY)
                : level.getGameRules().get(GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY);
            return Math.max(0, delay);
        }
        return 0;
    }

    @Override
    public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return entity instanceof ServerPlayer player ? DungeonPortals.destination(player, level, pos) : null;
    }

    @Override
    public Transition getLocalTransition() {
        return Transition.CONFUSION;
    }
}
