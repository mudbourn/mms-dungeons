package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.mixin.dungeon.RandomStateAccessor;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

// Reads whether the base terrain noise leaves a single block open.
public final class TerrainAir {

    private TerrainAir() {
    }

    public static DensityFunction finalDensity(RandomState randomState) {
        return ((RandomStateAccessor) (Object) randomState).mmsDungeons$router().finalDensity();
    }

    public static boolean isAir(RandomState randomState, DensityFunction density, int x, int y, int z) {
        return randomState.sampleBlockValueUncached(density, x, y, z) <= 0.0F;
    }
}
