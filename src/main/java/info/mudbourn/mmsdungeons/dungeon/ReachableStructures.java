package info.mudbourn.mmsdungeons.dungeon;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

// Drops dungeon structures sealed in rock, so nothing needs x-ray to find.
public final class ReachableStructures {

    // Alex's Caves builds its structures into its own biome terrain, which the base noise does not show.
    private static final String ALEXS_CAVES = "alexscaves";
    // Columns sampled along each side of the structure's outline.
    private static final int STEP = 6;
    private static final int Y_STEP = 3;
    private static final int MAX_SAMPLES = 400;

    private ReachableStructures() {
    }

    public static boolean exempt(Identifier structure) {
        return structure.getNamespace().equals(ALEXS_CAVES) || DddPlacement.isPlacedDdd(structure);
    }

    // Whether cave air two blocks tall touches the structure's outline anywhere along its height.
    public static boolean touchesCaveAir(BoundingBox box, ChunkGenerator generator, RandomState randomState, LevelHeightAccessor heights) {
        int minX = box.minX() - 1;
        int maxX = box.maxX() + 1;
        int minZ = box.minZ() - 1;
        int maxZ = box.maxZ() + 1;
        int bottom = Math.max(box.minY(), heights.getMinY());
        int top = Math.min(box.maxY(), heights.getMaxY() - 2);
        int perColumn = (top - bottom) / Y_STEP + 1;
        int perimeter = 2 * (maxX - minX + 1) + 2 * (maxZ - minZ + 1);
        int step = Math.max(STEP, (perimeter * perColumn + MAX_SAMPLES - 1) / MAX_SAMPLES);
        DensityFunction density = TerrainAir.finalDensity(randomState);
        for (int x = minX; x <= maxX; x += step) {
            if (openColumn(x, minZ, bottom, top, randomState, density) || openColumn(x, maxZ, bottom, top, randomState, density)) {
                return true;
            }
        }
        for (int z = minZ; z <= maxZ; z += step) {
            if (openColumn(minX, z, bottom, top, randomState, density) || openColumn(maxX, z, bottom, top, randomState, density)) {
                return true;
            }
        }
        return false;
    }

    private static boolean openColumn(int x, int z, int bottom, int top, RandomState randomState, DensityFunction density) {
        for (int y = bottom; y <= top; y += Y_STEP) {
            if (TerrainAir.isAir(randomState, density, x, y, z) && TerrainAir.isAir(randomState, density, x, y + 1, z)) {
                return true;
            }
        }
        return false;
    }
}
