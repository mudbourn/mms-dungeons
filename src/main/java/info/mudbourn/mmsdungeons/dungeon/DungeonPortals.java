package info.mudbourn.mmsdungeons.dungeon;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

// Moves players between a surface portal and its linked exit portal in the dungeon, building the exit on first use.
public final class DungeonPortals {

    private static final Component GATEKEEPER_LAUGHS = Component.literal("The gatekeeper notices your effort, and laughs.");
    private static final int COOLDOWN_TICKS = 100;
    // Flood fill cap when finding a portal's anchor block.
    private static final int MAX_PORTAL_BLOCKS = 256;
    // The roof's lowest bedrock layer.
    private static final int ROOF_Y = 157;
    private static final int ROOF_GAP = 5;
    private static final int MIN_HEADROOM = 10;
    // The highest feet position whose headroom still stops ROOF_GAP blocks under the roof.
    private static final int SEARCH_TOP = ROOF_Y - ROOF_GAP - MIN_HEADROOM;
    // Arrivals stay in the Sanctuary Region, never deeper.
    private static final int SEARCH_BOTTOM = DungeonDimension.SANCTUARY_BOTTOM;
    // Where an exit portal and its chamber are carved when the Sanctuary Region has no open space in this column.
    private static final int FALLBACK_Y = 111;
    // Width and height of a portal's opening.
    private static final int OPENING = 3;
    // How far around a portal to look for somewhere to stand.
    private static final int STAND_RADIUS = 4;

    private DungeonPortals() {
    }

    public static void onEnter(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (player.isOnPortalCooldown()) {
            return;
        }
        player.setPortalCooldown(COOLDOWN_TICKS);
        BlockPos anchor = anchorOf(level, pos);
        if (DungeonDimension.isDungeon(level)) {
            leaveDungeon(player, anchor);
        } else {
            enterDungeon(player, level, anchor);
        }
    }

    private static void enterDungeon(ServerPlayer player, ServerLevel level, BlockPos anchor) {
        if (!GateCheck.passes(player)) {
            player.sendSystemMessage(GATEKEEPER_LAUGHS, true);
            Vec3 spot = Vec3.atBottomCenterOf(standingSpotNear(level, anchor));
            player.teleportTo(spot.x, spot.y, spot.z);
            return;
        }
        ServerLevel dungeon = level.getServer().getLevel(DungeonDimension.LEVEL);
        if (dungeon == null) {
            return;
        }
        PortalLinks links = PortalLinks.get(level.getServer());
        GlobalPos surface = GlobalPos.of(level.dimension(), anchor);
        BlockPos exit = links.exitFor(surface);
        if (exit == null || !dungeon.getBlockState(exit).is(DungeonBlocks.DUNGEON_PORTAL)) {
            exit = buildExit(dungeon, anchor.getX(), anchor.getZ());
            links.link(surface, exit);
        }
        Vec3 landing = Vec3.atBottomCenterOf(exit.offset(1, 0, 1));
        teleport(player, dungeon, landing);
    }

    private static void leaveDungeon(ServerPlayer player, BlockPos anchor) {
        GlobalPos surface = PortalLinks.get(player.level().getServer()).surfaceFor(anchor);
        ServerLevel target = surface == null ? null : player.level().getServer().getLevel(surface.dimension());
        if (target == null) {
            player.teleport(TeleportTransition.createDefault(player, TeleportTransition.PLAY_PORTAL_SOUND));
            return;
        }
        teleport(player, target, Vec3.atBottomCenterOf(standingSpotNear(target, surface.pos())));
    }

    private static void teleport(ServerPlayer player, ServerLevel level, Vec3 position) {
        player.teleport(new TeleportTransition(
            level,
            position,
            Vec3.ZERO,
            player.getYRot(),
            player.getXRot(),
            TeleportTransition.PLAY_PORTAL_SOUND
        ));
    }

    // The lowest, then most north-west portal block connected to this one, which names the whole portal.
    private static BlockPos anchorOf(ServerLevel level, BlockPos start) {
        BlockPos anchor = start;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < MAX_PORTAL_BLOCKS) {
            BlockPos pos = queue.poll();
            if (pos.compareTo(anchor) < 0) {
                anchor = pos;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!seen.contains(next) && level.getBlockState(next).is(DungeonBlocks.DUNGEON_PORTAL)) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
        return anchor;
    }

    // Builds a bedrock-framed 3x3 exit portal at the highest open point of the column and returns its anchor.
    private static BlockPos buildExit(ServerLevel dungeon, int x, int z) {
        dungeon.getChunk(x >> 4, z >> 4);
        int floor = highestOpenFloor(dungeon, x, z);
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int dx = -2; dx <= 3; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                dungeon.setBlockAndUpdate(new BlockPos(x + dx, floor - 1, z + dz), stone);
                for (int dy = 0; dy < MIN_HEADROOM; dy++) {
                    dungeon.setBlockAndUpdate(new BlockPos(x + dx, floor + dy, z + dz), air);
                }
            }
        }
        for (int dx = -1; dx <= OPENING; dx++) {
            for (int dy = -1; dy <= OPENING; dy++) {
                boolean frame = dx == -1 || dx == OPENING || dy == -1 || dy == OPENING;
                BlockState state = frame ? bedrock : DungeonBlocks.DUNGEON_PORTAL.defaultBlockState();
                dungeon.setBlockAndUpdate(new BlockPos(x + dx, floor + dy, z), state);
            }
        }
        return new BlockPos(x, floor, z);
    }

    // The highest Y in the column with MIN_HEADROOM open blocks above something solid.
    private static int highestOpenFloor(ServerLevel level, int x, int z) {
        for (int y = SEARCH_TOP; y > SEARCH_BOTTOM; y--) {
            BlockPos feet = new BlockPos(x, y, z);
            if (hasHeadroom(level, feet) && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) {
                return y;
            }
        }
        return FALLBACK_Y;
    }

    private static boolean hasHeadroom(ServerLevel level, BlockPos feet) {
        for (int dy = 0; dy < MIN_HEADROOM; dy++) {
            if (!level.getBlockState(feet.above(dy)).isAir()) {
                return false;
            }
        }
        return true;
    }

    // The nearest open two-block space with a solid floor around the portal, so a player does not land inside it.
    private static BlockPos standingSpotNear(ServerLevel level, BlockPos anchor) {
        BlockPos best = anchor.above(OPENING + 1);
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos spot : BlockPos.betweenClosed(anchor.offset(-STAND_RADIUS, -1, -STAND_RADIUS), anchor.offset(STAND_RADIUS, OPENING, STAND_RADIUS))) {
            BlockPos floor = spot.below();
            boolean open = level.getBlockState(spot).isAir() && level.getBlockState(spot.above()).isAir();
            boolean closer = spot.distSqr(anchor) < bestDistance;
            if (open && closer && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                best = spot.immutable();
                bestDistance = spot.distSqr(anchor);
            }
        }
        return best;
    }
}
