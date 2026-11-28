package info.mudbourn.mmsdungeons.dungeon;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.levelgen.RandomState;
import org.jspecify.annotations.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.CarverOutput;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
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
    public static final double LABYRINTH_SHARE = 0.40;

    private static final long SALT = 0x6C616279726E7468L;
    private static final long SANCTUARY_SALT = 0x73616E6374756172L;
    // Sanctuary cells this close to the dimension's center stay open.
    private static final double OPEN_CENTER_RADIUS = 128.0;
    // Cells whose center is this close to an Aerial Hell cavern center stay caves.
    private static final double CAVERN_CLEARANCE = CELL_BLOCKS * 0.71 + 120.0;

    // Blocks over which labyrinth rock fades into a neighbouring caves cell, and how far noise shifts that fade.
    private static final double EDGE_FADE = 20.0;
    private static final double EDGE_JITTER = 0.6;
    private static final SimplexNoise EDGE_NOISE = new SimplexNoise(RandomSource.create(SALT));

    // Block heights in the Deep Region where a cell is checked for an Alex's Caves biome, and the namespace of those biomes.
    private static final int[] BIOME_CHECK_HEIGHTS = {-112, -64, -16, 32, 72};
    private static final String ALEXS_CAVES = "alexscaves";

    private static volatile long seed;
    private static volatile @Nullable BiomeSource biomes;
    private static volatile @Nullable RandomState randomState;
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
            ServerLevel dungeon = server.getLevel(DungeonDimension.LEVEL);
            biomes = dungeon == null ? null : dungeon.getChunkSource().getGenerator().getBiomeSource();
            randomState = dungeon == null ? null : dungeon.getChunkSource().randomState();
            cells = new ConcurrentHashMap<>();
            LabyrinthPalette.clearAllChoices();
            ready = true;
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ready = false;
            biomes = null;
            randomState = null;
        });
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

    // Labyrinth rock density: 1 inside a labyrinth cell, fading with noise to -1 near a side that borders caves, and -1000 outside.
    public static float rockDensity(int x, int y, int z) {
        if (!isSolid(x, y, z)) {
            return -1000.0F;
        }
        boolean sanctuary = y > TOP;
        int cellX = cellOfBlock(x);
        int cellZ = cellOfBlock(z);
        int localX = x - cellX * CELL_BLOCKS;
        int localZ = z - cellZ * CELL_BLOCKS;
        double edge = EDGE_FADE;
        if (localX < EDGE_FADE && !labyrinthCell(sanctuary, cellX - 1, cellZ)) {
            edge = Math.min(edge, localX);
        }
        if (CELL_BLOCKS - 1 - localX < EDGE_FADE && !labyrinthCell(sanctuary, cellX + 1, cellZ)) {
            edge = Math.min(edge, CELL_BLOCKS - 1 - localX);
        }
        if (localZ < EDGE_FADE && !labyrinthCell(sanctuary, cellX, cellZ - 1)) {
            edge = Math.min(edge, localZ);
        }
        if (CELL_BLOCKS - 1 - localZ < EDGE_FADE && !labyrinthCell(sanctuary, cellX, cellZ + 1)) {
            edge = Math.min(edge, CELL_BLOCKS - 1 - localZ);
        }
        if (edge >= EDGE_FADE) {
            return 1.0F;
        }
        double fade = edge / EDGE_FADE + EDGE_NOISE.get(x / 16.0, y / 12.0, z / 16.0) * EDGE_JITTER;
        return (float) (Math.clamp(fade, 0.0, 1.0) * 2.0 - 1.0);
    }

    private static boolean labyrinthCell(boolean sanctuary, int cellX, int cellZ) {
        return sanctuary ? isSanctuaryLabyrinth(seed, cellX, cellZ) : isLabyrinth(seed, cellX, cellZ);
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
            return rolledLabyrinth(worldSeed, cellX, cellZ) && clearOfCaverns(cellX, cellZ) && clearOfAlexsCaves(cellX, cellZ);
        }
        long key = ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
        return cells.computeIfAbsent(key, k -> rolledLabyrinth(worldSeed, cellX, cellZ) && clearOfCaverns(cellX, cellZ) && clearOfAlexsCaves(cellX, cellZ));
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

    // Whether no Alex's Caves biome shows at the cell's center or quarter points at any checked height, so those biomes keep their own caves.
    private static boolean clearOfAlexsCaves(int cellX, int cellZ) {
        BiomeSource source = biomes;
        RandomState state = randomState;
        if (source == null || state == null) {
            return true;
        }
        BiomeResolver resolver = source.createUncachedResolver(state);
        for (int qx = 1; qx <= 3; qx++) {
            for (int qz = 1; qz <= 3; qz++) {
                if ((qx + qz) % 2 != 0) {
                    continue;
                }
                int x = cellX * CELL_BLOCKS + CELL_BLOCKS * qx / 4;
                int z = cellZ * CELL_BLOCKS + CELL_BLOCKS * qz / 4;
                for (int y : BIOME_CHECK_HEIGHTS) {
                    Holder<Biome> biome = resolver.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
                    if (biome.unwrapKey().map(key -> key.identifier().getNamespace().equals(ALEXS_CAVES)).orElse(false)) {
                        return false;
                    }
                }
            }
        }
        return true;
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
