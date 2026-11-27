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

    // Share of Deep Region cells that roll as labyrinths; the rest are caves.
    public static final double LABYRINTH_SHARE = 2.0 / 3.0;

    private static final long SALT = 0x6C616279726E7468L;
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
            LabyrinthPalette.clearChoices();
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

    // Whether a position is labyrinth rock: in a labyrinth cell and inside the Deep Region.
    public static boolean isSolid(int x, int y, int z) {
        return y <= TOP && isLabyrinthColumn(x, z);
    }

    public static boolean insideCell(BoundingBox box, int cellX, int cellZ) {
        return cellOfBlock(box.minX()) == cellX && cellOfBlock(box.maxX()) == cellX
            && cellOfBlock(box.minZ()) == cellZ && cellOfBlock(box.maxZ()) == cellZ;
    }

    // Whether any part of a box below the Sanctuary Region lies in a labyrinth cell.
    public static boolean touchesLabyrinth(BoundingBox box) {
        if (box.minY() > TOP) {
            return false;
        }
        for (int cx = cellOfBlock(box.minX()); cx <= cellOfBlock(box.maxX()); cx++) {
            for (int cz = cellOfBlock(box.minZ()); cz <= cellOfBlock(box.maxZ()); cz++) {
                if (isLabyrinth(seed, cx, cz)) {
                    return true;
                }
            }
        }
        return false;
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

    // Passes on only the carving above the Deep Region.
    public record AboveDeepRegion(CarverOutput output) implements CarverOutput {

        @Override
        public void carve(int x, int y, int z) {
            if (y > TOP) {
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
