package info.mudbourn.mmsdungeons.dungeon;

import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;

// Gives the Periculum its own Alex's Caves layout, with a different biome per cell and tighter spacing.
public final class PericulumBiomes {

    // Periculum spawn distances and same-biome spacing, as a share of the Deep Region's.
    public static final double LENIENCY = 0.5;

    private static final ThreadLocal<Boolean> SAMPLING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IN_DUNGEON = ThreadLocal.withInitial(() -> false);

    private PericulumBiomes() {
    }

    // Set while the dungeon's biome source samples an Alex's Caves cell at a Periculum height.
    public static void sampling(boolean periculum) {
        SAMPLING.set(periculum);
    }

    public static boolean sampling() {
        return SAMPLING.get();
    }

    // Set while the dungeon's biome source samples an Alex's Caves cell at any Deep Region height.
    public static void inDungeon(boolean dungeon) {
        IN_DUNGEON.set(dungeon);
    }

    public static boolean inDungeon() {
        return IN_DUNGEON.get();
    }

    // The Periculum biome for a cell: the Deep Region's biome shifted by 1 to count - 1 places, so never the same one.
    public static int shift(int biome, int cellX, int cellZ) {
        int count = BiomeGenerationConfig.getBiomeCount();
        if (count < 2) {
            return biome;
        }
        int mix = (cellX * 73856093) ^ (cellZ * 19349663) ^ 0x5BD1E995 ^ DungeonGeneration.current() * 0x2545F491;
        mix ^= mix >>> 15;
        return Math.floorMod(biome + 1 + Math.floorMod(mix, count - 1), count);
    }
}
