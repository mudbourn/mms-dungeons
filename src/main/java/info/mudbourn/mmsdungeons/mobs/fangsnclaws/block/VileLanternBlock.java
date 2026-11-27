package info.mudbourn.mmsdungeons.mobs.fangsnclaws.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// A lantern that shoves hostile mobs within 6 blocks away from it.
public class VileLanternBlock extends LanternBlock {

    private static final int REPEL_INTERVAL = 15;
    private static final int REPEL_RADIUS = 6;
    private static final int REPEL_HEIGHT = 3;
    private static final double SHOVE = 0.5;
    private static final double FLEE_DISTANCE = 12.0;
    private static final double FLEE_SPEED = 1.5;

    public VileLanternBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            level.scheduleTick(pos, this, REPEL_INTERVAL);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, REPEL_INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        AABB area = new AABB(pos).inflate(REPEL_RADIUS, REPEL_HEIGHT, REPEL_RADIUS);
        Vec3 center = Vec3.atCenterOf(pos);
        for (Monster monster : level.getEntitiesOfClass(Monster.class, area)) {
            Vec3 away = monster.position().subtract(center).normalize();
            monster.setDeltaMovement(monster.getDeltaMovement().add(away.scale(SHOVE)));
            monster.needsSync = true;
            if (monster instanceof PathfinderMob mob) {
                Vec3 flee = monster.position().add(away.scale(FLEE_DISTANCE));
                mob.getNavigation().moveTo(flee.x, flee.y, flee.z, FLEE_SPEED);
            }
        }
        level.scheduleTick(pos, this, REPEL_INTERVAL);
    }
}
