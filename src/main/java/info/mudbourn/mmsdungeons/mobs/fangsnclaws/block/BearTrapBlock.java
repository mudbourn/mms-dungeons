package info.mudbourn.mmsdungeons.mobs.fangsnclaws.block;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEffects;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// A floor trap that snaps shut on the first creature to step on it and opens again when used.
public class BearTrapBlock extends Block {

    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");

    private static final ResourceKey<DamageType> BEAR_TRAP = ResourceKey.create(Registries.DAMAGE_TYPE, MmsDungeons.id("bear_trap"));
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    private static final double DETECT_HEIGHT = 0.5;
    private static final int TICK_INTERVAL = 5;
    private static final float DAMAGE = 8.0f;
    private static final int BLEED_TICKS = 200;
    private static final int BLEED_AMPLIFIER = 1;

    public BearTrapBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CLOSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CLOSED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.getValue(CLOSED)) {
            level.scheduleTick(pos, this, TICK_INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(CLOSED)) {
            return;
        }
        AABB area = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + DETECT_HEIGHT, pos.getZ() + 1);
        List<LivingEntity> victims = level.getEntitiesOfClass(
            LivingEntity.class,
            area,
            entity -> !entity.isSpectator() && !(entity instanceof Player player && player.getAbilities().instabuild)
        );
        if (victims.isEmpty()) {
            level.scheduleTick(pos, this, TICK_INTERVAL);
            return;
        }
        LivingEntity victim = victims.getFirst();
        level.setBlock(pos, state.setValue(CLOSED, true), 3);
        victim.hurtServer(level, level.damageSources().source(BEAR_TRAP), DAMAGE);
        FangsNClawsEffects.bleed(victim, BLEED_TICKS, BLEED_AMPLIFIER);
        victim.setDeltaMovement(victim.getDeltaMovement().multiply(0.1, 1.0, 0.1));
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 0.5f);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(CLOSED)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(CLOSED, false), 3);
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.2f);
            level.scheduleTick(pos, this, TICK_INTERVAL);
        }
        return InteractionResult.SUCCESS;
    }
}
