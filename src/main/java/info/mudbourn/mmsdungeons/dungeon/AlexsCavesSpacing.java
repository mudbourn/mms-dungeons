package info.mudbourn.mmsdungeons.dungeon;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import com.github.alexmodguy.alexscaves.server.misc.VoronoiGenerator.VoronoiInfo;
import info.mudbourn.mmsdungeons.mixin.dungeon.VoronoiGeneratorAccessor;

// Keeps two copies of the same Alex's Caves biome at least 1.75 times that biome's spawn distance apart,
// or the Periculum's looser share of that in the Periculum layer.
public final class AlexsCavesSpacing {

    private static final int PRIME_X = 501125321;
    private static final int PRIME_Z = 1720413743;
    private static final int HASH_MULTIPLIER = 668265261;
    private static final double JITTER = 0.43701595F;
    private static final double HASH_SCALE = 4.656613E-10F;

    private AlexsCavesSpacing() {
    }

    // Whether a nearby cell of the same biome outranks this one, so this one stays empty.
    public static boolean yields(long worldSeed, VoronoiInfo info, boolean periculum) {
        double cellBlocks = AlexsCaves.COMMON_CONFIG.caveBiomeMeanWidth.get() + AlexsCaves.COMMON_CONFIG.caveBiomeMeanSeparation.get();
        double jitter = JITTER * AlexsCaves.COMMON_CONFIG.caveBiomeSpacingRandomness.get();
        int cellX = (int) Math.round(info.cellPos().x);
        int cellZ = (int) Math.round(info.cellPos().z);
        int hash = hash(worldSeed, cellX, cellZ);
        int biome = biomeOf(hash, cellX, cellZ, periculum);
        double scale = periculum ? PericulumBiomes.LENIENCY : 1.0;
        double spawnDistance = AlexsCavesRotation.distanceFor(biome) * scale;
        double spacing = AlexsCavesRotation.spacingFor(biome) * scale;
        double[] center = center(cellX, cellZ, hash, jitter, cellBlocks);
        int reach = (int) Math.ceil(spacing / cellBlocks) + 1;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                int otherHash = hash(worldSeed, cellX + dx, cellZ + dz);
                if (biomeOf(otherHash, cellX + dx, cellZ + dz, periculum) != biome || !outranks(otherHash, dx, dz, hash)) {
                    continue;
                }
                double[] other = center(cellX + dx, cellZ + dz, otherHash, jitter, cellBlocks);
                boolean eligible = other[0] * other[0] + other[1] * other[1] >= spawnDistance * spawnDistance;
                double gapX = other[0] - center[0];
                double gapZ = other[1] - center[1];
                if (eligible && gapX * gapX + gapZ * gapZ < spacing * spacing) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean outranks(int otherHash, int dx, int dz, int hash) {
        if (otherHash != hash) {
            return otherHash < hash;
        }
        return dx < 0 || (dx == 0 && dz < 0);
    }

    private static int hash(long worldSeed, int cellX, int cellZ) {
        return ((int) worldSeed ^ cellX * PRIME_X ^ cellZ * PRIME_Z) * HASH_MULTIPLIER;
    }

    private static int biomeOf(int hash, int cellX, int cellZ, boolean periculum) {
        int biome = (int) ((hash * HASH_SCALE + 1.0) * 0.5 * BiomeGenerationConfig.getBiomeCount());
        return periculum ? PericulumBiomes.shift(biome, cellX, cellZ) : biome;
    }

    // The cell's jittered center in blocks.
    private static double[] center(int cellX, int cellZ, int hash, double jitter, double cellBlocks) {
        double[] vectors = VoronoiGeneratorAccessor.mmsDungeons$randVecs2D();
        int index = hash & 510;
        return new double[] {
            (cellX + vectors[index] * jitter) * cellBlocks,
            (cellZ + vectors[index | 1] * jitter) * cellBlocks
        };
    }
}
