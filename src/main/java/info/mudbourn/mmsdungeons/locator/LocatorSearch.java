package info.mudbourn.mmsdungeons.locator;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.jspecify.annotations.Nullable;

// Finds the nearest of the tuned targets, on the search thread, without making the server thread wait.
final class LocatorSearch {

    private static final int STRUCTURE_RADIUS_CHUNKS = 48;
    // Candidate chunks asked for their structure starts at once.
    private static final int BATCH = 4;
    // At most this many candidate chunks are checked, nearest first.
    private static final int MAX_CANDIDATES = 256;
    private static final int CHUNK_TIMEOUT_SECONDS = 20;
    private static final int BIOME_RADIUS = 1024;
    private static final int BIOME_STEP = 32;
    private static final int BIOME_VERTICAL_STEP = 64;

    private LocatorSearch() {
    }

    record Hit(BlockPos pos, LocatorTarget target) {
    }

    static Hit find(ServerLevel level, List<LocatorTarget> targets, BlockPos origin) {
        Hit structure = structure(level, targets, origin);
        Hit biome = biome(level, targets, origin);
        if (structure == null || biome == null) {
            return structure == null ? biome : structure;
        }
        return distance(structure.pos(), origin) <= distance(biome.pos(), origin) ? structure : biome;
    }

    private static double distance(BlockPos pos, BlockPos origin) {
        double dx = pos.getX() - origin.getX();
        double dz = pos.getZ() - origin.getZ();
        return dx * dx + dz * dz;
    }

    // Runs on the search thread. Candidate chunks come from each structure's placement rule, which is pure maths,
    // and only those chunks are asked for their structure starts, nearest first and a few at a time. The chunk
    // requests are made from this thread, so the server thread schedules them and never waits on one.
    private static Hit structure(ServerLevel level, List<LocatorTarget> targets, BlockPos origin) {
        Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Map<ResourceKey<Structure>, LocatorTarget> owners = new HashMap<>();
        List<Holder<Structure>> holders = new ArrayList<>();
        for (LocatorTarget target : targets) {
            if (target.structures() != null) {
                registry.get(target.structures()).ifPresent(set -> set.forEach(holder -> claim(holder, target, owners, holders)));
            } else if (target.structure() != null) {
                registry.get(target.structure()).ifPresent(holder -> claim(holder, target, owners, holders));
            }
        }
        if (holders.isEmpty()) {
            return null;
        }
        List<Candidate> candidates = candidates(level, holders, origin);
        for (int from = 0; from < candidates.size() && from < MAX_CANDIDATES; from += BATCH) {
            List<Candidate> batch = candidates.subList(from, Math.min(candidates.size(), Math.min(from + BATCH, MAX_CANDIDATES)));
            List<CompletableFuture<ChunkResult<ChunkAccess>>> futures = new ArrayList<>();
            for (Candidate candidate : batch) {
                futures.add(level.getChunkSource().getChunkFuture(candidate.x(), candidate.z(), ChunkStatus.STRUCTURE_STARTS, true));
            }
            for (int i = 0; i < batch.size(); i++) {
                ChunkAccess chunk = await(futures.get(i));
                if (chunk == null) {
                    continue;
                }
                for (Holder<Structure> holder : batch.get(i).structures()) {
                    StructureStart start = chunk.getStartForStructure(holder.value());
                    if (start != null && start.isValid()) {
                        LocatorTarget owner = holder.unwrapKey().map(owners::get).orElse(null);
                        return new Hit(start.getBoundingBox().getCenter(), owner != null ? owner : targets.get(0));
                    }
                }
            }
        }
        return null;
    }

    private record Candidate(int x, int z, long distance, List<Holder<Structure>> structures) {
    }

    // Every chunk within range where a target structure's placement allows a start, nearest first. Structures
    // that share a placement, such as the members of one structure set, are tested together.
    private static List<Candidate> candidates(ServerLevel level, List<Holder<Structure>> holders, BlockPos origin) {
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        Map<StructurePlacement, List<Holder<Structure>>> byPlacement = new IdentityHashMap<>();
        for (Holder<Structure> holder : holders) {
            for (StructurePlacement placement : state.getPlacementsForStructure(holder)) {
                byPlacement.computeIfAbsent(placement, key -> new ArrayList<>()).add(holder);
            }
        }
        int originX = origin.getX() >> 4;
        int originZ = origin.getZ() >> 4;
        Map<Long, Candidate> found = new HashMap<>();
        for (Map.Entry<StructurePlacement, List<Holder<Structure>>> entry : byPlacement.entrySet()) {
            for (int dx = -STRUCTURE_RADIUS_CHUNKS; dx <= STRUCTURE_RADIUS_CHUNKS; dx++) {
                for (int dz = -STRUCTURE_RADIUS_CHUNKS; dz <= STRUCTURE_RADIUS_CHUNKS; dz++) {
                    int x = originX + dx;
                    int z = originZ + dz;
                    if (!entry.getKey().isStructureChunk(state, x, z)) {
                        continue;
                    }
                    long key = ChunkPos.pack(x, z);
                    Candidate old = found.get(key);
                    List<Holder<Structure>> structures = new ArrayList<>(entry.getValue());
                    if (old != null) {
                        structures.addAll(old.structures());
                    }
                    found.put(key, new Candidate(x, z, (long) dx * dx + (long) dz * dz, structures));
                }
            }
        }
        List<Candidate> sorted = new ArrayList<>(found.values());
        sorted.sort(Comparator.comparingLong(Candidate::distance));
        return sorted;
    }

    // The chunk once its structure starts exist, or null when it fails or takes too long.
    private static @Nullable ChunkAccess await(CompletableFuture<ChunkResult<ChunkAccess>> future) {
        try {
            return future.get(CHUNK_TIMEOUT_SECONDS, TimeUnit.SECONDS).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static void claim(
        Holder<Structure> holder,
        LocatorTarget target,
        Map<ResourceKey<Structure>, LocatorTarget> owners,
        List<Holder<Structure>> holders
    ) {
        holder.unwrapKey().ifPresent(key -> {
            if (owners.putIfAbsent(key, target) == null) {
                holders.add(holder);
            }
        });
    }

    private static Hit biome(ServerLevel level, List<LocatorTarget> targets, BlockPos origin) {
        Map<ResourceKey<Biome>, LocatorTarget> owners = new HashMap<>();
        for (LocatorTarget target : targets) {
            if (target.biome() != null) {
                owners.putIfAbsent(target.biome(), target);
            }
        }
        if (owners.isEmpty()) {
            return null;
        }
        Pair<BlockPos, Holder<Biome>> result = level.findClosestBiome3d(
            holder -> holder.unwrapKey().map(owners::containsKey).orElse(false),
            origin,
            BIOME_RADIUS,
            BIOME_STEP,
            BIOME_VERTICAL_STEP);
        if (result == null) {
            return null;
        }
        LocatorTarget owner = result.getSecond().unwrapKey().map(owners::get).orElse(null);
        return new Hit(result.getFirst(), owner != null ? owner : targets.get(0));
    }
}
