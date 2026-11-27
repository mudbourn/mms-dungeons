package info.mudbourn.mmsdungeons.locator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.levelgen.structure.Structure;

// Works out which of the dungeon's three regions each biome and structure occurs in, once per server.
public final class LocatorRegions {

    public static final int SANCTUARY = 0;
    public static final int DEEP = 1;
    public static final int PERICULUM = 2;
    public static final int ALL = 7;

    private static final int[] BAND_MIN = {81, 0, -128};
    private static final int[] BAND_MAX = {156, 80, -1};
    private static final int[][] SAMPLE_Y = {
        {90, 120, 150},
        {10, 30, 50, 70},
        {-10, -40, -70, -100, -120}
    };
    private static final int GRID_RADIUS = 2048;
    private static final int GRID_STEP = 128;

    private static Map<Holder<Biome>, Integer> biomeMasks;
    private static final Map<Identifier, Integer> HEIGHT_MASKS = new HashMap<>();

    private LocatorRegions() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            biomeMasks = null;
            HEIGHT_MASKS.clear();
        });
    }

    public static int current(ServerPlayer player) {
        if (player.level().dimension() != DungeonDimension.LEVEL) {
            return DEEP;
        }
        int y = player.blockPosition().getY();
        return y >= BAND_MIN[SANCTUARY] ? SANCTUARY : y >= BAND_MIN[DEEP] ? DEEP : PERICULUM;
    }

    static int mask(ServerLevel dungeon, LocatorTarget target) {
        if (target.structures() != null) {
            Registry<Structure> registry = dungeon.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            return registry.get(target.structures())
                .map(set -> set.stream().mapToInt(holder -> structure(dungeon, holder)).reduce(0, (a, b) -> a | b))
                .orElse(0);
        }
        if (target.structure() != null) {
            return dungeon.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(target.structure())
                .map(holder -> structure(dungeon, holder))
                .orElse(0);
        }
        return dungeon.registryAccess().lookupOrThrow(Registries.BIOME).get(target.biome())
            .map(holder -> biome(dungeon, holder))
            .orElse(0);
    }

    // A biome the dungeon can hold but the sample grid never met is too rare to place, so it shows in every region.
    static int biome(ServerLevel dungeon, Holder<Biome> biome) {
        Integer sampled = biomes(dungeon).get(biome);
        if (sampled != null) {
            return sampled;
        }
        return dungeon.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().contains(biome) ? ALL : 0;
    }

    static int structure(ServerLevel dungeon, Holder<Structure> structure) {
        int regions = 0;
        for (Holder<Biome> biome : dungeon.getChunkSource().getGenerator().getBiomeSource().possibleBiomes()) {
            if (structure.value().biomes().contains(biome)) {
                regions |= biome(dungeon, biome);
            }
        }
        return regions & heights(dungeon, structure);
    }

    private static Map<Holder<Biome>, Integer> biomes(ServerLevel dungeon) {
        if (biomeMasks == null) {
            Map<Holder<Biome>, Integer> found = new HashMap<>();
            BiomeResolver resolver = dungeon.getChunkSource().getGenerator().getBiomeSource().createUncachedResolver(dungeon.getChunkSource().randomState());
            for (int x = -GRID_RADIUS; x <= GRID_RADIUS; x += GRID_STEP) {
                for (int z = -GRID_RADIUS; z <= GRID_RADIUS; z += GRID_STEP) {
                    for (int band = 0; band < SAMPLE_Y.length; band++) {
                        for (int y : SAMPLE_Y[band]) {
                            found.merge(resolver.getNoiseBiome(x >> 2, y >> 2, z >> 2), 1 << band, (a, b) -> a | b);
                        }
                    }
                }
            }
            biomeMasks = found;
        }
        return biomeMasks;
    }

    private static int heights(ServerLevel dungeon, Holder<Structure> structure) {
        Identifier id = structure.unwrapKey().map(key -> key.identifier()).orElse(null);
        if (id == null || !id.getNamespace().equals("mms_dungeons")) {
            return ALL;
        }
        return HEIGHT_MASKS.computeIfAbsent(id, key -> readHeights(dungeon, key));
    }

    private static int readHeights(ServerLevel dungeon, Identifier id) {
        Identifier file = Identifier.fromNamespaceAndPath(id.getNamespace(), "worldgen/structure/" + id.getPath() + ".json");
        try (BufferedReader reader = dungeon.getServer().getResourceManager().openAsReader(file)) {
            JsonObject height = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("start_height");
            int min = absolute(height.get("min_inclusive"));
            int max = absolute(height.get("max_inclusive"));
            int mask = 0;
            for (int band = 0; band < BAND_MIN.length; band++) {
                if (min <= BAND_MAX[band] && max >= BAND_MIN[band]) {
                    mask |= 1 << band;
                }
            }
            return mask;
        } catch (Exception e) {
            return ALL;
        }
    }

    private static int absolute(JsonElement element) {
        return element.getAsJsonObject().get("absolute").getAsInt();
    }
}
