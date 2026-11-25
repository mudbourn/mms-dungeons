package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

// Aerial Hell cavern centers, taken from the portal structure set's own spread so the portal sits at the middle.
public final class CavernGrid {

    private static final ResourceKey<StructureSet> PORTAL_SET = ResourceKey.create(Registries.STRUCTURE_SET, MmsDungeons.id("cavern_abandoned_portal"));

    public static final long NONE = Long.MIN_VALUE;

    private static volatile RandomSpreadStructurePlacement placement;
    private static volatile long seed;
    private static volatile Map<Long, Long> centers = new ConcurrentHashMap<>();

    private CavernGrid() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerLevel level = server.getLevel(DungeonDimension.LEVEL);
            if (level == null) {
                return;
            }
            StructureSet set = server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(PORTAL_SET).value();
            seed = level.getSeed();
            centers = new ConcurrentHashMap<>();
            placement = (RandomSpreadStructurePlacement) set.placement();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> placement = null);
    }

    // Horizontal distance in blocks to the nearest cavern center, or infinity before the dungeon has loaded.
    public static double distanceToCenter(int x, int z) {
        long center = nearestCenter(x, z);
        if (center == NONE) {
            return Double.POSITIVE_INFINITY;
        }
        double offsetX = x - centerX(center);
        double offsetZ = z - centerZ(center);
        return Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
    }

    // The nearest cavern center packed as x and z, or NONE before the dungeon has loaded.
    public static long nearestCenter(int x, int z) {
        RandomSpreadStructurePlacement spread = placement;
        if (spread == null) {
            return NONE;
        }
        int spacing = spread.spacing();
        int regionX = Math.floorDiv(x >> 4, spacing);
        int regionZ = Math.floorDiv(z >> 4, spacing);
        double best = Double.POSITIVE_INFINITY;
        long nearest = NONE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int rx = regionX + dx;
                int rz = regionZ + dz;
                long packed = centers.computeIfAbsent(pack(rx, rz), key -> {
                    ChunkPos chunk = spread.getPotentialStructureChunk(seed, rx * spacing, rz * spacing);
                    return pack(chunk.getMinBlockX() + 8, chunk.getMinBlockZ() + 8);
                });
                double offsetX = x - centerX(packed);
                double offsetZ = z - centerZ(packed);
                double distance = offsetX * offsetX + offsetZ * offsetZ;
                if (distance < best) {
                    best = distance;
                    nearest = packed;
                }
            }
        }
        return nearest;
    }

    public static int centerX(long center) {
        return (int) (center >> 32);
    }

    public static int centerZ(long center) {
        return (int) center;
    }

    // A stable per-cavern number mixing the world seed with the center.
    public static long hash(long center) {
        long h = center ^ seed * 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    private static long pack(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }
}
