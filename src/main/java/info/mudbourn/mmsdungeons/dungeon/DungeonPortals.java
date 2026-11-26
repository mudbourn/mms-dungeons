package info.mudbourn.mmsdungeons.dungeon;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

// Moves players between a surface portal and its linked exit portal in the dungeon, building the exit on first use.
public final class DungeonPortals {

    // Blocks around an arrival searched for open floor.
    private static final int ARRIVAL_SEARCH = 32;
    // Spacing of the columns checked in that search.
    private static final int ARRIVAL_STEP = 4;
    private static final Component GATEKEEPER_LAUGHS = Component.literal("The gatekeeper notices your effort, and laughs.");
    // Flood fill cap when finding a portal's anchor block.
    private static final int MAX_PORTAL_BLOCKS = 256;
    // The roof's lowest bedrock layer.
    private static final int ROOF_Y = 157;
    private static final int ROOF_GAP = 2;
    private static final int MIN_HEADROOM = 5;
    // The highest feet position whose headroom still stops ROOF_GAP blocks under the roof.
    private static final int SEARCH_TOP = ROOF_Y - ROOF_GAP - MIN_HEADROOM;
    // Arrivals stay in the Sanctuary Region, never deeper.
    private static final int SEARCH_BOTTOM = DungeonDimension.SANCTUARY_BOTTOM;
    // Where an exit portal and its chamber are carved when no open space is found.
    private static final int FALLBACK_Y = 140;
    // Width and height of a portal's opening.
    private static final int OPENING = 3;
    // How far from the dungeon's centre a new exit portal may be placed.
    private static final int SITE_RADIUS = 4096;
    // Random spots tried before carving a chamber at the last one.
    private static final int SITE_ATTEMPTS = 24;
    // Open floor kept on every side of an exit portal's frame.
    private static final int ROOM_MARGIN = 2;
    // How far around a portal to look for somewhere to stand.
    private static final int STAND_RADIUS = 4;

    // Players a portal is moving, so ForcedArrivals leaves that move alone.
    private static final Set<UUID> PORTAL_MOVES = new HashSet<>();

    private DungeonPortals() {
    }

    // True once for a player a portal is moving, clearing the mark.
    static boolean takePortalMove(ServerPlayer player) {
        return PORTAL_MOVES.remove(player.getUUID());
    }

    // Where a player forced into the dungeon at this column lands: the open Sanctuary floor an exit portal would
    // use, or a carved chamber with an exit portal when the column has no room.
    static Vec3 arrivalSpot(ServerLevel dungeon, int x, int z) {
        for (int ring = 0; ring <= ARRIVAL_SEARCH; ring += ARRIVAL_STEP) {
            for (int dx = -ring; dx <= ring; dx += ARRIVAL_STEP) {
                for (int dz = -ring; dz <= ring; dz += ARRIVAL_STEP) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    dungeon.getChunk((x + dx) >> 4, (z + dz) >> 4);
                    int floor = highestOpenFloor(dungeon, x + dx, z + dz);
                    BlockPos feet = new BlockPos(x + dx, floor, z + dz);
                    if (hasHeadroom(dungeon, feet) && dungeon.getBlockState(feet.below()).isFaceSturdy(dungeon, feet.below(), Direction.UP)) {
                        return Vec3.atBottomCenterOf(feet);
                    }
                }
            }
        }
        return Vec3.atBottomCenterOf(carveRoom(dungeon, new BlockPos(x, FALLBACK_Y, z)));
    }

    // Opens a small room with a stone floor and no portal.
    private static BlockPos carveRoom(ServerLevel dungeon, BlockPos feet) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                dungeon.setBlockAndUpdate(feet.offset(dx, -1, dz), stone);
                for (int dy = 0; dy < MIN_HEADROOM; dy++) {
                    dungeon.setBlockAndUpdate(feet.offset(dx, dy, dz), air);
                }
            }
        }
        return feet;
    }

    // Where the portal at pos sends the player, or null when it sends them nowhere.
    public static TeleportTransition destination(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockPos anchor = anchorOf(level, pos);
        if (DungeonDimension.isDungeon(level)) {
            return leaveDungeon(player, anchor);
        }
        return enterDungeon(player, level, anchor);
    }

    private static TeleportTransition enterDungeon(ServerPlayer player, ServerLevel level, BlockPos anchor) {
        if (!GateCheck.passes(player)) {
            player.sendSystemMessage(GATEKEEPER_LAUGHS, true);
            Vec3 spot = Vec3.atBottomCenterOf(standingSpotNear(level, anchor));
            player.teleportTo(spot.x, spot.y, spot.z);
            return null;
        }
        ServerLevel dungeon = level.getServer().getLevel(DungeonDimension.LEVEL);
        if (dungeon == null) {
            return null;
        }
        PortalLinks links = PortalLinks.get(level.getServer());
        GlobalPos surface = GlobalPos.of(level.dimension(), anchor);
        BlockPos exit = links.exitFor(surface);
        if (exit == null || !dungeon.getBlockState(exit).is(DungeonBlocks.DUNGEON_PORTAL)) {
            exit = buildExit(dungeon, findExitSite(dungeon));
            links.link(surface, exit);
        }
        return transition(player, dungeon, Vec3.atBottomCenterOf(standingSpotNear(dungeon, exit)));
    }

    private static TeleportTransition leaveDungeon(ServerPlayer player, BlockPos anchor) {
        GlobalPos surface = PortalLinks.get(player.level().getServer()).surfaceFor(anchor);
        ServerLevel target = surface == null ? null : player.level().getServer().getLevel(surface.dimension());
        PORTAL_MOVES.add(player.getUUID());
        if (target == null) {
            return player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.PLAY_PORTAL_SOUND);
        }
        return transition(player, target, Vec3.atBottomCenterOf(standingSpotNear(target, surface.pos())));
    }

    private static TeleportTransition transition(ServerPlayer player, ServerLevel level, Vec3 position) {
        PORTAL_MOVES.add(player.getUUID());
        return new TeleportTransition(
            level,
            position,
            Vec3.ZERO,
            player.getYRot(),
            player.getXRot(),
            TeleportTransition.PLAY_PORTAL_SOUND.then(moved -> PORTAL_MOVES.remove(moved.getUUID()))
        );
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

    // A random open spot in the Sanctuary Region with room for an exit portal, or a spot to carve one when none is found.
    private static BlockPos findExitSite(ServerLevel dungeon) {
        RandomSource random = dungeon.getRandom();
        BlockPos site = null;
        for (int attempt = 0; attempt < SITE_ATTEMPTS; attempt++) {
            int x = random.nextIntBetweenInclusive(-SITE_RADIUS, SITE_RADIUS);
            int z = random.nextIntBetweenInclusive(-SITE_RADIUS, SITE_RADIUS);
            dungeon.getChunk(x >> 4, z >> 4);
            site = new BlockPos(x, highestOpenFloor(dungeon, x, z), z);
            if (roomIsSafe(dungeon, site)) {
                return site;
            }
        }
        return site;
    }

    // True when every column of the exit room has headroom and a solid, dry floor.
    private static boolean roomIsSafe(ServerLevel dungeon, BlockPos site) {
        for (int dx = -1 - ROOM_MARGIN; dx <= OPENING + ROOM_MARGIN; dx++) {
            for (int dz = -ROOM_MARGIN; dz <= ROOM_MARGIN; dz++) {
                BlockPos feet = site.offset(dx, 0, dz);
                BlockState floor = dungeon.getBlockState(feet.below());
                boolean dry = floor.getFluidState().isEmpty();
                if (!dry || !hasHeadroom(dungeon, feet) || !floor.isFaceSturdy(dungeon, feet.below(), Direction.UP)) {
                    return false;
                }
            }
        }
        return true;
    }

    // Builds a bedrock-framed 3x3 exit portal standing on site, clears its room, and returns its anchor.
    private static BlockPos buildExit(ServerLevel dungeon, BlockPos site) {
        int x = site.getX();
        int z = site.getZ();
        int floor = site.getY();
        dungeon.getChunk(x >> 4, z >> 4);
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int dx = -1 - ROOM_MARGIN; dx <= OPENING + ROOM_MARGIN; dx++) {
            for (int dz = -ROOM_MARGIN; dz <= ROOM_MARGIN; dz++) {
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
