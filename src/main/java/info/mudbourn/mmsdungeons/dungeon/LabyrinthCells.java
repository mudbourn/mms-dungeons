package info.mudbourn.mmsdungeons.dungeon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.level.chunk.CarverOutput;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

// Splits the Deep Region into square cells and picks, from the world seed, which ones hold a labyrinth instead of caves.
public final class LabyrinthCells {

    public static final int CELL_CHUNKS = 12;
    public static final int CELL_BLOCKS = CELL_CHUNKS * 16;
    // Deep Region top: everything below the Sanctuary Region.
    public static final int TOP = DungeonDimension.SANCTUARY_BOTTOM - 1;
    // Sanctuary Region top: the last block under the roof.
    public static final int SANCTUARY_TOP = 156;

    // Share of Deep Region cells that roll as labyrinths; the rest are caves.
    public static final double LABYRINTH_SHARE = 0.65;

    private static final long SALT = 0x6C616279726E7468L;
    private static final long SANCTUARY_SALT = 0x73616E6374756172L;
    // Sanctuary cells this close to the dimension's center stay open.
    private static final double OPEN_CENTER_RADIUS = 128.0;
    // Cells whose center is this close to an Aerial Hell cavern center stay caves.
    private static final double CAVERN_CLEARANCE = CELL_BLOCKS * 0.71 + 120.0;

    private static volatile long seed;
    private static volatile Map<Long, Boolean> cells = new ConcurrentHashMap<>();
    // True once the cavern grid has loaded and answers can be cached.
    private static volatile boolean ready;

    private LabyrinthCells() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            seed = server.getWorldGenSettings().options().seed();
            ready = false;
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            cells = new ConcurrentHashMap<>();
            LabyrinthPalette.clearAllChoices();
            ready = true;
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ready = false);
    }

    // Drops the cached cell rolls and palettes after a Deep Region rebuild.
    public static void reset() {
        cells = new ConcurrentHashMap<>();
        LabyrinthPalette.clearChoices();
    }

    public static long seed() {
        return seed;
    }

    public static int cellOfChunk(int chunk) {
        return Math.floorDiv(chunk, CELL_CHUNKS);
    }

    public static int cellOfBlock(int block) {
        return Math.floorDiv(block, CELL_BLOCKS);
    }

    // Whether the block column at x and z lies in a labyrinth cell of the loaded world.
    public static boolean isLabyrinthColumn(int x, int z) {
        return isLabyrinth(seed, cellOfBlock(x), cellOfBlock(z));
    }

    // Whether the block column at x and z lies in a Sanctuary labyrinth cell.
    public static boolean isSanctuaryColumn(int x, int z) {
        return isSanctuaryLabyrinth(seed, cellOfBlock(x), cellOfBlock(z));
    }

    // Whether a position is labyrinth rock: in a Deep labyrinth cell inside the Deep Region, or in a Sanctuary labyrinth cell inside the Sanctuary Region.
    public static boolean isSolid(int x, int y, int z) {
        if (y <= TOP) {
            return isLabyrinthColumn(x, z);
        }
        return y <= SANCTUARY_TOP && isSanctuaryColumn(x, z);
    }

    public static boolean insideCell(BoundingBox box, int cellX, int cellZ) {
        return cellOfBlock(box.minX()) == cellX && cellOfBlock(box.maxX()) == cellX
            && cellOfBlock(box.minZ()) == cellZ && cellOfBlock(box.maxZ()) == cellZ;
    }

    // Whether any part of a box lies in a labyrinth cell of the region it reaches.
    public static boolean touchesLabyrinth(BoundingBox box) {
        boolean deep = box.minY() <= TOP;
        boolean sanctuary = box.maxY() > TOP;
        for (int cx = cellOfBlock(box.minX()); cx <= cellOfBlock(box.maxX()); cx++) {
            for (int cz = cellOfBlock(box.minZ()); cz <= cellOfBlock(box.maxZ()); cz++) {
                if (deep && isLabyrinth(seed, cx, cz) || sanctuary && isSanctuaryLabyrinth(seed, cx, cz)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Whether a Sanctuary cell is a labyrinth: fixed by the world seed alone, so Deep Region rebuilds never move it, and never within the open center.
    public static boolean isSanctuaryLabyrinth(long worldSeed, int cellX, int cellZ) {
        if (nearCenter(cellX, cellZ)) {
            return false;
        }
        long h = mix(worldSeed ^ SANCTUARY_SALT);
        h = mix(h + cellX * 0x9E3779B97F4A7C15L);
        h = mix(h + cellZ * 0xC2B2AE3D27D4EB4FL);
        return (h >>> 11) * 0x1.0p-53 < LABYRINTH_SHARE;
    }

    private static boolean nearCenter(int cellX, int cellZ) {
        double nearestX = Math.clamp(0, cellX * CELL_BLOCKS, cellX * CELL_BLOCKS + CELL_BLOCKS - 1);
        double nearestZ = Math.clamp(0, cellZ * CELL_BLOCKS, cellZ * CELL_BLOCKS + CELL_BLOCKS - 1);
        return Math.hypot(nearestX, nearestZ) <= OPEN_CENTER_RADIUS;
    }

    public static boolean isLabyrinth(long worldSeed, int cellX, int cellZ) {
        if (!ready) {
            return rolledLabyrinth(worldSeed, cellX, cellZ) && clearOfCaverns(cellX, cellZ);
        }
        long key = ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
        return cells.computeIfAbsent(key, k -> rolledLabyrinth(worldSeed, cellX, cellZ) && clearOfCaverns(cellX, cellZ));
    }

    private static boolean rolledLabyrinth(long worldSeed, int cellX, int cellZ) {
        long h = DungeonGeneration.layoutSeed(worldSeed) ^ SALT;
        h = mix(h + cellX * 0x9E3779B97F4A7C15L);
        h = mix(h + cellZ * 0xC2B2AE3D27D4EB4FL);
        return (h >>> 11) * 0x1.0p-53 < LABYRINTH_SHARE;
    }

    private static boolean clearOfCaverns(int cellX, int cellZ) {
        int centerX = cellX * CELL_BLOCKS + CELL_BLOCKS / 2;
        int centerZ = cellZ * CELL_BLOCKS + CELL_BLOCKS / 2;
        return CavernGrid.distanceToCenter(centerX, centerZ) > CAVERN_CLEARANCE;
    }

    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    // Passes on only the carving outside the labyrinth rock of the regions it covers.
    public record OutsideLabyrinth(CarverOutput output, boolean deep, boolean sanctuary) implements CarverOutput {

        @Override
        public void carve(int x, int y, int z) {
            boolean rock = y <= TOP ? deep : sanctuary;
            if (!rock) {
                output.carve(x, y, z);
            }
        }

        @Override
        public int minY() {
            return output.minY();
        }

        @Override
        public int maxY() {
            return output.maxY();
        }
    }
}
