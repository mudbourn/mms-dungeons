package info.mudbourn.mmsdungeons.dungeon;

import java.util.List;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

// Picks how high a surface tower is kept: a few blocks past the first cave air along its height.
public final class DddTowerTrim {

    private static final int COLUMN_STEP = 3;
    private static final int MIN_OPEN_COLUMNS = 2;
    private static final int INTO_AIR = 3;
    private static final int MIN_HEIGHT = 5;

    private DddTowerTrim() {
    }

    // The top block height to keep, or TowerPiece.UNSET when the tower never meets cave air.
    public static int trimTop(BoundingBox tower, List<BoundingBox> reachBoxes, RandomState randomState, LevelHeightAccessor heights) {
        DensityFunction density = TerrainAir.finalDensity(randomState);
        int last = Math.min(tower.maxY(), heights.getMaxY() - 2);
        for (int y = Math.max(tower.minY(), heights.getMinY()); y <= last; y++) {
            if (openColumns(tower, y, reachBoxes, randomState, density) >= MIN_OPEN_COLUMNS) {
                return Math.min(tower.maxY(), Math.max(y + INTO_AIR, tower.minY() + MIN_HEIGHT - 1));
            }
        }
        return TowerPiece.UNSET;
    }

    private static int openColumns(BoundingBox tower, int y, List<BoundingBox> reachBoxes, RandomState randomState, DensityFunction density) {
        int open = 0;
        for (int x = tower.minX(); x <= tower.maxX(); x += COLUMN_STEP) {
            for (int z = tower.minZ(); z <= tower.maxZ(); z += COLUMN_STEP) {
                boolean air = !DddPruning.insideAny(reachBoxes, x, y, z)
                    && TerrainAir.isAir(randomState, density, x, y, z)
                    && TerrainAir.isAir(randomState, density, x, y + 1, z);
                if (air) {
                    open++;
                }
            }
        }
        return open;
    }
}
