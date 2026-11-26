package info.mudbourn.mmsdungeons.dungeon;

import java.util.OptionalLong;
import net.minecraft.util.Mth;

// Splits the Periculum into warped cells that each hold one pair of Nether Infection biomes, spaced apart by their leading biome.
public final class NetherPatches {

    // Cell side in quarts.
    public static final int CELL = 32;
    // Quarts at each cell edge where no Nether Infection biome is kept.
    public static final int MARGIN = 4;
    // Least distance between two patches led by the same biome, in quarts.
    public static final int SPACING = 128;
    // Lattice spacing of the edge warp, in quarts.
    private static final int WARP_GRID = 8;
    // Largest edge warp, in quarts.
    private static final float WARP = 3.0f;

    private NetherPatches() {
    }

    // Hash of the cell holding this quart, or empty inside a cell's edge gap or a cell that yields to a nearby one.
    public static OptionalLong cell(int quartX, int quartZ, int leaders) {
        float x = quartX + warp(quartX, quartZ, 0x5F3759DFL) * WARP;
        float z = quartZ + warp(quartX, quartZ, 0x1B873593L) * WARP;
        int cellX = Mth.floor(x / CELL);
        int cellZ = Mth.floor(z / CELL);
        float inX = x - cellX * CELL;
        float inZ = z - cellZ * CELL;
        if (inX < MARGIN || inZ < MARGIN || inX >= CELL - MARGIN || inZ >= CELL - MARGIN) {
            return OptionalLong.empty();
        }
        long hash = hash(cellX, cellZ);
        return yields(cellX, cellZ, hash, leaders) ? OptionalLong.empty() : OptionalLong.of(hash);
    }

    // Index of the biome that leads a cell.
    public static int leader(long hash, int leaders) {
        return (int) Math.floorMod(hash, (long) leaders);
    }

    private static boolean yields(int cellX, int cellZ, long hash, int leaders) {
        int leader = leader(hash, leaders);
        int reach = Mth.ceil((float) SPACING / CELL);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx == 0 && dz == 0 || (dx * dx + dz * dz) * CELL * CELL >= SPACING * SPACING) {
                    continue;
                }
                long other = hash(cellX + dx, cellZ + dz);
                if (leader(other, leaders) == leader && (other < hash || other == hash && (dx < 0 || dx == 0 && dz < 0))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long hash(int cellX, int cellZ) {
        return mix(cellX * 0x9E3779B97F4A7C15L ^ cellZ * 0xC2B2AE3D27D4EB4FL);
    }

    private static float warp(int quartX, int quartZ, long salt) {
        int gx = Math.floorDiv(quartX, WARP_GRID);
        int gz = Math.floorDiv(quartZ, WARP_GRID);
        float fx = smooth((float) Math.floorMod(quartX, WARP_GRID) / WARP_GRID);
        float fz = smooth((float) Math.floorMod(quartZ, WARP_GRID) / WARP_GRID);
        float top = Mth.lerp(fx, lattice(gx, gz, salt), lattice(gx + 1, gz, salt));
        float bottom = Mth.lerp(fx, lattice(gx, gz + 1, salt), lattice(gx + 1, gz + 1, salt));
        return Mth.lerp(fz, top, bottom);
    }

    private static float lattice(int gx, int gz, long salt) {
        long hash = mix(gx * 0x9E3779B97F4A7C15L ^ gz * 0xC2B2AE3D27D4EB4FL ^ salt);
        return (hash >>> 40) / (float) (1 << 23) - 1.0f;
    }

    private static float smooth(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xFF51AFD7ED558CCDL;
        value ^= value >>> 33;
        value *= 0xC4CEB9FE1A85EC53L;
        return value ^ value >>> 33;
    }
}
