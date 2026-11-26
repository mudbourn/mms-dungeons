package info.mudbourn.mmsdungeons.mobs.knightquest.block;

import info.mudbourn.mmsdungeons.mobs.knightquest.KnightQuestItems;
import info.mudbourn.mmsdungeons.mobs.knightquest.KnightQuestMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// A chalice that takes Great Essence one at a time, then fills a Goblet or wakes the Netherman.
public class GreatChaliceBlock extends Block {

    public static final int CAPACITY = 4;
    public static final IntegerProperty ESSENCE = IntegerProperty.create("essence", 0, CAPACITY);

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(2.0, 0.0, 2.0, 14.0, 2.0, 14.0),
        Block.box(6.0, 2.0, 6.0, 10.0, 9.0, 10.0),
        Block.box(4.0, 9.0, 4.0, 12.0, 15.0, 12.0)
    );

    public GreatChaliceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ESSENCE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ESSENCE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        player.sendOverlayMessage(Component.translatable("block.mms_dungeons.great_chalice.filled", state.getValue(ESSENCE), CAPACITY));
        return InteractionResult.CONSUME;
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
        int essence = state.getValue(ESSENCE);
        if (stack.is(KnightQuestItems.GREAT_ESSENCE) && essence < CAPACITY) {
            return this.deposit(stack, state, level, pos, player, essence);
        }
        if (stack.is(KnightQuestItems.EMPTY_GOBLET) && essence == CAPACITY) {
            return this.fillGoblet(stack, state, level, pos, player, hand);
        }
        if (stack.is(KnightQuestItems.RADIANT_ESSENCE) && essence == CAPACITY) {
            return this.summon(stack, state, level, pos, player);
        }
        return InteractionResult.PASS;
    }

    private InteractionResult deposit(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, int essence) {
        if (level instanceof ServerLevel server) {
            level.setBlock(pos, state.setValue(ESSENCE, essence + 1), 3);
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 1.0f);
            server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.5);
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult fillGoblet(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel server) {
            level.setBlock(pos, state.setValue(ESSENCE, 0), 3);
            ItemStack filled = new ItemStack(KnightQuestItems.FILLED_GOBLET);
            stack.consume(1, player);
            if (stack.isEmpty()) {
                player.setItemInHand(hand, filled);
            } else if (!player.getInventory().add(filled)) {
                player.spawnAtLocation(server, filled);
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            server.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.1);
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult summon(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server) {
            level.setBlock(pos, state.setValue(ESSENCE, 0), 3);
            stack.consume(1, player);
            KnightQuestMobs.NETHERMAN.spawn(server, pos.above(), EntitySpawnReason.MOB_SUMMONED);
            level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 1.0f);
            server.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 60, 0.6, 1.0, 0.6, 0.5);
        }
        return InteractionResult.SUCCESS;
    }
}
