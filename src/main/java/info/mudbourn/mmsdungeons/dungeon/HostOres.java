package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

// Places vanilla ores into the non-stone host blocks of the dungeon's biomes and the overworld's cave biomes.
public final class HostOres implements Feature {

    private static final MapCodec<HostOres> CODEC = MapCodec.unit(HostOres::new);
    private static final TagKey<Biome> OVERWORLD_CAVES = TagKey.create(Registries.BIOME, MmsDungeons.id("host_ore_caves"));
    private static final ResourceKey<PlacedFeature> PLACED = ResourceKey.create(Registries.PLACED_FEATURE, MmsDungeons.id("host_ores"));
    private static final List<String> HOST_NAMES = List.of(
        "coal_upper", "coal_lower",
        "iron_upper", "iron_middle", "iron_small",
        "gold", "gold_lower",
        "redstone", "redstone_lower",
        "diamond", "diamond_medium", "diamond_large", "diamond_buried",
        "lapis", "lapis_buried",
        "copper"
    );
    private static final List<ResourceKey<PlacedFeature>> ORES = keys("host_ores/", HOST_NAMES);
    private static final List<ResourceKey<PlacedFeature>> DUNGEON_ORES = Stream.concat(
        keys("dungeon_host_ores/", HOST_NAMES).stream(),
        keys("dungeon_ores/", List.of(
            "iron", "gold", "redstone", "diamond", "lapis", "copper",
            "exposed_iron", "exposed_gold", "exposed_redstone", "exposed_diamond", "exposed_lapis", "exposed_copper"
        )).stream()
    ).toList();

    private static List<ResourceKey<PlacedFeature>> keys(String folder, List<String> names) {
        return names.stream().map(name -> ResourceKey.create(Registries.PLACED_FEATURE, MmsDungeons.id(folder + name))).toList();
    }

    public static void register() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MmsDungeons.id("host_ores"), CODEC);
        DepthThinPlacement.register();
        BiomeModifications.addFeature(BiomeSelectors.all(), GenerationStep.Decoration.UNDERGROUND_ORES, PLACED);
    }

    @Override
    public MapCodec<HostOres> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        ResourceKey<Level> dimension = level.getLevel().dimension();
        boolean overworldCave = dimension.equals(Level.OVERWORLD) && level.getBiome(origin).is(OVERWORLD_CAVES);
        if (!dimension.equals(DungeonDimension.LEVEL) && !overworldCave) {
            return false;
        }
        Registry<PlacedFeature> placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        boolean any = false;
        List<ResourceKey<PlacedFeature>> ores = dimension.equals(DungeonDimension.LEVEL) ? DUNGEON_ORES : ORES;
        for (ResourceKey<PlacedFeature> ore : ores) {
            PlacedFeature feature = placed.getValue(ore);
            if (feature != null) {
                any |= feature.place(level, generator, random, origin);
            }
        }
        return any;
    }
}
