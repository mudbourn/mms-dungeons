package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.corpse.Corpses;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// Lights a dungeon portal when a Recovery Cross is dropped into a 3x3 opening framed by bedrock, lying flat or standing up.
public final class DungeonPortalFrames {

    private static final int SIZE = 3;
    private static final int CHECK_INTERVAL = 10;

    private DungeonPortalFrames() {
    }

    public static void onItemTick(ItemEntity item) {
        if (item.tickCount % CHECK_INTERVAL != 0 || !(item.level() instanceof ServerLevel level)) {
            return;
        }
        if (DungeonDimension.isDungeon(level) || !item.getItem().is(Corpses.RECOVERY_CROSS)) {
            return;
        }
        BlockPos pos = item.blockPosition();
        for (BlockPos start : List.of(pos, pos.below())) {
            for (Direction.Axis normal : Direction.Axis.values()) {
                List<BlockPos> opening = findOpening(level, start, normal);
                if (opening != null) {
                    light(level, item, opening, normal);
                    return;
                }
            }
        }
    }

    // The 3x3 opening around pos in the plane facing normal, or null when it is not open or not framed.
    private static List<BlockPos> findOpening(ServerLevel level, BlockPos pos, Direction.Axis normal) {
        Direction.Axis[] plane = planeOf(normal);
        for (int offsetU = 0; offsetU < SIZE; offsetU++) {
            for (int offsetV = 0; offsetV < SIZE; offsetV++) {
                BlockPos corner = pos.relative(plane[0], -offsetU).relative(plane[1], -offsetV);
                List<BlockPos> opening = openingAt(level, corner, plane);
                if (opening != null) {
                    return opening;
                }
            }
        }
        return null;
    }

    private static List<BlockPos> openingAt(ServerLevel level, BlockPos corner, Direction.Axis[] plane) {
        List<BlockPos> opening = new ArrayList<>();
        for (int u = -1; u <= SIZE; u++) {
            for (int v = -1; v <= SIZE; v++) {
                boolean edgeU = u == -1 || u == SIZE;
                boolean edgeV = v == -1 || v == SIZE;
                if (edgeU && edgeV) {
                    continue;
                }
                BlockPos cell = corner.relative(plane[0], u).relative(plane[1], v);
                BlockState state = level.getBlockState(cell);
                if (edgeU || edgeV) {
                    if (!state.is(Blocks.BEDROCK)) {
                        return null;
                    }
                } else if (state.isAir() || state.is(Blocks.WATER)) {
                    opening.add(cell);
                } else {
                    return null;
                }
            }
        }
        return opening;
    }

    private static Direction.Axis[] planeOf(Direction.Axis normal) {
        return switch (normal) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
    }

    private static void light(ServerLevel level, ItemEntity item, List<BlockPos> opening, Direction.Axis normal) {
        for (BlockPos cell : opening) {
            level.setBlockAndUpdate(cell, DungeonBlocks.DUNGEON_PORTAL.defaultBlockState().setValue(DungeonPortalBlock.AXIS, normal));
        }
        item.getItem().shrink(1);
        if (item.getItem().isEmpty()) {
            item.discard();
        }
        level.playSound(null, opening.get(SIZE * SIZE / 2), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
    }
}
