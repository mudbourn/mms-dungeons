package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

// Large open caverns carved into almost solid Periculum stretches, each with a fill pool biome.
public final class FillCaverns {

    // Candidate grid: one jittered cavern center per cell.
    private static final int CELL = 256;
    public static final int RADIUS = 40;
    public static final int HALF_HEIGHT = 30;
    private static final int MARGIN = RADIUS + 8;

    // Vertical limits of a cavern center.
    private static final int BEDROCK_FLOOR = -128;
    private static final int FLOOR_CLEARANCE = 8;
    private static final int MIN_CENTER_Y = BEDROCK_FLOOR + FLOOR_CLEARANCE + HALF_HEIGHT;
    private static final int MAX_CENTER_Y = DungeonDimension.PERICULUM_TOP - HALF_HEIGHT - 1;

    // Horizontal distance from every Aerial Hell cavern center.
    private static final int AERIAL_HELL_CLEARANCE = 160;

    // Sparseness test grid step and the most open share a cell may have.
    private static final int SAMPLE_STEP = 8;
    private static final double MAX_OPEN_SHARE = 0.15;

    // Same-biome spacing and the cell radius that covers it.
    private static final int SPACING = 768;
    private static final int REACH = (SPACING + CELL - 1) / CELL;

    // Biome volume as a multiple of the carved rim.
    private static final double BIOME_LIMIT = 1.05;

    private static final float NONE = 1.0E9F;
    private static final float DISTANCE_SCALE = Math.min(RADIUS, HALF_HEIGHT);
    private static final String ALEXS_CAVES = "alexscaves";
    private static final TagKey<Biome> POOL = TagKey.create(
        Registries.BIOME,
        MmsDungeons.id("periculum_fill")
    );
    private static final TagKey<Biome> NETHER_INFECTION = TagKey.create(
        Registries.BIOME,
        MmsDungeons.id("nether_infestation")
    );
    private static final int[][] OFFSETS = sampleOffsets();

    private static volatile State state;
    private static volatile List<List<Holder<Biome>>> preferred = List.of();
    private static volatile List<Holder<Biome>> fallback = List.of();

    private FillCaverns() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            loadPool(server);
            state = null;
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            state = null;
            preferred = List.of();
            fallback = List.of();
        });
    }

    // Stores the Periculum's density without the fill carve for the sparseness test.
    static synchronized void bind(DensitySampler sampler, long worldSeed) {
        State current = state;
        if (current == null || current.seed != worldSeed) {
            state = new State(sampler, worldSeed, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
            return;
        }
        state = new State(sampler, worldSeed, current.cells, current.assigned);
    }

    // Signed distance in blocks to the cavern around a position, or a huge value when none reaches it.
    static float distance(int x, int y, int z) {
        State current = state;
        Cell cell = cavernAt(current, x, y, z, 1.0);
        if (cell == null) {
            return NONE;
        }
        return (float) ((normalized(cell, x, y, z) - 1.0) * DISTANCE_SCALE);
    }

    // The fill biome at a block position, or null outside every fill cavern.
    public static Holder<Biome> biomeAt(int x, int y, int z) {
        State current = state;
        Cell cell = cavernAt(current, x, y, z, BIOME_LIMIT);
        return cell == null ? null : biomeOf(current, cell).orElse(null);
    }

    // The cavern whose cell holds a block column, or null when the cell has no fill cavern.
    public static Cavern cavernAtColumn(int x, int z) {
        State current = state;
        if (current == null) {
            return null;
        }
        Cell cell = cell(current, Math.floorDiv(x, CELL), Math.floorDiv(z, CELL));
        if (cell == null || !cell.qualifies) {
            return null;
        }
        return biomeOf(current, cell)
            .map(biome -> new Cavern(cell.x, cell.y, cell.z, cell.hash, biome))
            .orElse(null);
    }

    // Whether a fill biome is one of the Nether Infection biomes.
    public static boolean isNetherInfection(Holder<Biome> biome) {
        return biome.is(NETHER_INFECTION);
    }

    private static void loadPool(MinecraftServer server) {
        List<List<Holder<Biome>>> own = new ArrayList<>();
        List<Holder<Biome>> nether = new ArrayList<>();
        List<Holder<Biome>> alexs = new ArrayList<>();
        Set<Holder<Biome>> producible = producibleBiomes(server);
        server.registryAccess().lookupOrThrow(Registries.BIOME).get(POOL).ifPresent(set -> {
            for (Holder<Biome> holder : set) {
                if (!producible.contains(holder)) {
                    continue;
                }
                if (isNetherInfection(holder)) {
                    if (nether.isEmpty()) {
                        own.add(nether);
                    }
                    nether.add(holder);
                    continue;
                }
                boolean fromAlexsCaves = holder.unwrapKey()
                    .map(key -> key.identifier().getNamespace().equals(ALEXS_CAVES))
                    .orElse(false);
                if (fromAlexsCaves) {
                    alexs.add(holder);
                } else {
                    own.add(List.of(holder));
                }
            }
        });
        preferred = List.copyOf(own.stream().map(List::copyOf).toList());
        fallback = List.copyOf(alexs);
    }

    private static Set<Holder<Biome>> producibleBiomes(MinecraftServer server) {
        ResourceKey<LevelStem> stemKey = ResourceKey.create(
            Registries.LEVEL_STEM,
            DungeonDimension.LEVEL.identifier()
        );
        return server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM).get(stemKey)
            .map(stem -> stem.value().generator().getBiomeSource().possibleBiomes())
            .orElse(Set.of());
    }

    private static Cell cavernAt(State current, int x, int y, int z, double limit) {
        if (current == null || y < BEDROCK_FLOOR || y > DungeonDimension.PERICULUM_TOP) {
            return null;
        }
        int cellX = Math.floorDiv(x, CELL);
        int cellZ = Math.floorDiv(z, CELL);
        long hash = hash(current.seed, cellX, cellZ);
        Cell candidate = new Cell(cellX, cellZ, hash, false);
        if (normalized(candidate, x, y, z) > limit) {
            return null;
        }
        Cell cell = cell(current, cellX, cellZ);
        return cell != null && cell.qualifies ? cell : null;
    }

    private static Cell cell(State current, int cellX, int cellZ) {
        long key = pack(cellX, cellZ);
        Cell cached = current.cells.get(key);
        if (cached != null) {
            return cached;
        }
        Cell shape = new Cell(cellX, cellZ, hash(current.seed, cellX, cellZ), false);
        if (CavernGrid.nearestCenter(shape.x, shape.z) == CavernGrid.NONE) {
            return null;
        }
        boolean clear = CavernGrid.distanceToCenter(shape.x, shape.z) >= AERIAL_HELL_CLEARANCE;
        boolean qualifies = clear && sparse(current.base, shape);
        Cell cell = new Cell(cellX, cellZ, shape.hash, qualifies);
        Cell prior = current.cells.putIfAbsent(key, cell);
        return prior == null ? cell : prior;
    }

    private static boolean sparse(DensitySampler sampler, Cell cell) {
        SamplerContext context = SamplerContext.builder().build();
        int allowed = (int) Math.floor(OFFSETS.length * MAX_OPEN_SHARE);
        int open = 0;
        for (int[] offset : OFFSETS) {
            float density = sampler.sampleValue(
                context,
                cell.x + offset[0],
                cell.y + offset[1],
                cell.z + offset[2]
            );
            if (density < 0.0F && ++open > allowed) {
                return false;
            }
        }
        return true;
    }

    private static Optional<Holder<Biome>> biomeOf(State current, Cell cell) {
        long key = pack(cell.cellX, cell.cellZ);
        Optional<Holder<Biome>> cached = current.assigned.get(key);
        if (cached != null) {
            return cached;
        }
        Optional<Holder<Biome>> chosen = choose(current, cell);
        Optional<Holder<Biome>> prior = current.assigned.putIfAbsent(key, chosen);
        return prior == null ? chosen : prior;
    }

    private static Optional<Holder<Biome>> choose(State current, Cell cell) {
        List<List<Holder<Biome>>> own = preferred;
        for (int i = 0; i < own.size(); i++) {
            List<Holder<Biome>> entry = own.get(Math.floorMod((int) (cell.hash >>> 20) + i, own.size()));
            if (!claimedNearby(current, cell, entry)) {
                int member = (int) (cell.hash * 0x9E3779B97F4A7C15L >>> 33);
                return Optional.of(entry.get(Math.floorMod(member, entry.size())));
            }
        }
        List<Holder<Biome>> alexs = fallback;
        if (alexs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(alexs.get(Math.floorMod((int) (cell.hash >>> 32), alexs.size())));
    }

    private static boolean claimedNearby(State current, Cell cell, List<Holder<Biome>> entry) {
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dz = -REACH; dz <= REACH; dz++) {
                Cell other = dx == 0 && dz == 0 ? null : cell(current, cell.cellX + dx, cell.cellZ + dz);
                if (other == null || !other.qualifies || !outranks(other, cell)) {
                    continue;
                }
                double gapX = other.x - cell.x;
                double gapZ = other.z - cell.z;
                boolean near = gapX * gapX + gapZ * gapZ < (double) SPACING * SPACING;
                if (near && biomeOf(current, other).filter(entry::contains).isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean outranks(Cell other, Cell cell) {
        if (other.hash != cell.hash) {
            return Long.compareUnsigned(other.hash, cell.hash) < 0;
        }
        return pack(other.cellX, other.cellZ) < pack(cell.cellX, cell.cellZ);
    }

    private static double normalized(Cell cell, int x, int y, int z) {
        double dx = x - cell.x;
        double dy = y - cell.y;
        double dz = z - cell.z;
        double horizontal = (dx * dx + dz * dz) / ((double) RADIUS * RADIUS);
        double vertical = dy * dy / ((double) HALF_HEIGHT * HALF_HEIGHT);
        return Math.sqrt(horizontal + vertical);
    }

    private static int[][] sampleOffsets() {
        List<int[]> offsets = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx += SAMPLE_STEP) {
            for (int dy = -HALF_HEIGHT; dy <= HALF_HEIGHT; dy += SAMPLE_STEP) {
                for (int dz = -RADIUS; dz <= RADIUS; dz += SAMPLE_STEP) {
                    double horizontal = (double) (dx * dx + dz * dz) / (RADIUS * RADIUS);
                    double vertical = (double) (dy * dy) / (HALF_HEIGHT * HALF_HEIGHT);
                    double inside = horizontal + vertical;
                    if (inside <= 1.0) {
                        offsets.add(new int[] {dx, dy, dz});
                    }
                }
            }
        }
        return offsets.toArray(new int[0][]);
    }

    private static long hash(long seed, int cellX, int cellZ) {
        long h = pack(cellX, cellZ) ^ seed * 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    private static long pack(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }

    // A fill cavern's center, hash and assigned biome.
    public record Cavern(int x, int y, int z, long hash, Holder<Biome> biome) {
    }

    // The bound density, seed and caches, swapped as one unit.
    private record State(
        DensitySampler base,
        long seed,
        Map<Long, Cell> cells,
        Map<Long, Optional<Holder<Biome>>> assigned
    ) {
    }

    // One candidate cavern: cell, jittered center, hash and whether the cell qualifies.
    private static final class Cell {

        final int cellX;
        final int cellZ;
        final long hash;
        final boolean qualifies;
        final int x;
        final int y;
        final int z;

        Cell(int cellX, int cellZ, long hash, boolean qualifies) {
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.hash = hash;
            this.qualifies = qualifies;
            this.x = cellX * CELL + MARGIN + (int) ((hash >>> 8 & 0xFFFFFFL) % (CELL - 2 * MARGIN));
            this.z = cellZ * CELL + MARGIN + (int) ((hash >>> 32 & 0xFFFFFFL) % (CELL - 2 * MARGIN));
            this.y = MIN_CENTER_Y + (int) ((hash >>> 56 & 0xFFL) * (MAX_CENTER_Y - MIN_CENTER_Y + 1) / 256);
        }
    }
}
