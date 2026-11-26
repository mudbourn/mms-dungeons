package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.List;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

// Places vanilla ores into the non-stone host blocks of the dungeon's biomes, only in the dungeon.
public final class HostOres implements Feature {

    private static final MapCodec<HostOres> CODEC = MapCodec.unit(HostOres::new);
    private static final ResourceKey<PlacedFeature> PLACED = ResourceKey.create(Registries.PLACED_FEATURE, MmsDungeons.id("host_ores"));
    private static final List<ResourceKey<PlacedFeature>> ORES = List.of(
        "coal_upper", "coal_lower",
        "iron_upper", "iron_middle", "iron_small",
        "gold", "gold_lower",
        "redstone", "redstone_lower",
        "diamond", "diamond_medium", "diamond_large", "diamond_buried",
        "lapis", "lapis_buried",
        "copper"
    ).stream().map(name -> ResourceKey.create(Registries.PLACED_FEATURE, MmsDungeons.id("host_ores/" + name))).toList();

    public static void register() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MmsDungeons.id("host_ores"), CODEC);
        BiomeModifications.addFeature(BiomeSelectors.all(), GenerationStep.Decoration.UNDERGROUND_ORES, PLACED);
    }

    @Override
    public MapCodec<HostOres> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!level.getLevel().dimension().equals(DungeonDimension.LEVEL)) {
            return false;
        }
        Registry<PlacedFeature> placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        boolean any = false;
        for (ResourceKey<PlacedFeature> ore : ORES) {
            PlacedFeature feature = placed.getValue(ore);
            if (feature != null) {
                any |= feature.place(level, generator, random, origin);
            }
        }
        return any;
    }
}
