package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

// Places ruined portal cores and ancient debris on cave floors in Nether Infection biomes, chunk by chunk.
public final class NetherInfectionCore implements Feature {

    private static final MapCodec<NetherInfectionCore> CODEC = MapCodec.unit(NetherInfectionCore::new);
    // Nether ores placed at Periculum heights, which the Nether's own placements barely reach.
    private static final List<String> PERICULUM_ORES = List.of(
        "periculum_quartz_ore",
        "periculum_nether_gold_ore",
        "periculum_magma"
    );
    // The same ores for the Deep Region, run from the core feature so the real Nether never gets them.
    private static final List<ResourceKey<PlacedFeature>> DEEP_ORES = Stream.of(
        "deep_quartz_ore",
        "deep_nether_gold_ore"
    ).map(name -> ResourceKey.create(Registries.PLACED_FEATURE, MmsDungeons.id(name))).toList();
    private static final ResourceKey<PlacedFeature> PLACED = ResourceKey.create(
        Registries.PLACED_FEATURE,
        MmsDungeons.id("nether_infection_core")
    );

    private static final String NETHER_DESCENT = "netherdescent";
    private static final List<ResourceKey<Biome>> NETHER_DESCENT_BIOMES = Stream.of(
        "arisian_undergrowth",
        "crimson_gardens",
        "embur_bog",
        "sythian_torrids",
        "wailing_garth"
    ).map(name -> ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(NETHER_DESCENT, name))).toList();

    private static final TagKey<Biome> NETHER_INFESTATION = TagKey.create(
        Registries.BIOME,
        MmsDungeons.id("nether_infestation")
    );

    private static final int PORTAL_VARIANTS = 10;
    private static final int CORE_ONE_IN = 48;
    private static final int DEBRIS_ONE_IN = 64;
    private static final int CORE_SALT = 1;
    private static final int DEBRIS_SALT = 2;
    private static final int CORE_HEADROOM = 4;
    private static final int CORE_EDGE_MARGIN = 4;
    private static final int COLUMN_ATTEMPTS = 8;
    private static final int BEDROCK_CLEARANCE = 8;

    public static void register() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MmsDungeons.id("nether_infection_core"), CODEC);
        Predicate<BiomeSelectionContext> netherBiomes = BiomeSelectors.includeByKey(
            Biomes.NETHER_WASTES,
            Biomes.CRIMSON_FOREST,
            Biomes.WARPED_FOREST,
            Biomes.SOUL_SAND_VALLEY,
            Biomes.BASALT_DELTAS
        ).or(BiomeSelectors.includeByKey(NETHER_DESCENT_BIOMES));
        BiomeModifications.addFeature(netherBiomes, GenerationStep.Decoration.UNDERGROUND_DECORATION, PLACED);
        for (String ore : PERICULUM_ORES) {
            BiomeModifications.addFeature(
                netherBiomes,
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, MmsDungeons.id(ore))
            );
        }
        if (FabricLoader.getInstance().isModLoaded(NETHER_DESCENT)) {
            ResourceLoader.registerBuiltinPack(
                MmsDungeons.id("nether_descent"),
                FabricLoader.getInstance().getModContainer(MmsDungeons.MOD_ID).orElseThrow(),
                PackActivationType.ALWAYS_ENABLED
            );
        }
    }

    @Override
    public MapCodec<NetherInfectionCore> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!level.getLevel().dimension().equals(DungeonDimension.LEVEL)) {
            return false;
        }
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        long hash = DungeonGeneration.layoutSeed(level.getSeed()) ^ (minX >> 4) * 341873128712L ^ (minZ >> 4) * 132897987541L;
        boolean placed = waterToLava(level, minX, minZ);
        RandomSource coreRandom = RandomSource.create(mix(hash, CORE_SALT));
        if (coreRandom.nextInt(CORE_ONE_IN) == 0) {
            placed = placeCore(level, coreRandom, minX, minZ);
        }
        RandomSource debrisRandom = RandomSource.create(mix(hash, DEBRIS_SALT));
        if (debrisRandom.nextInt(DEBRIS_ONE_IN) == 0) {
            placed |= placeDebris(level, debrisRandom, minX, minZ);
        }
        BlockPos chunkOrigin = new BlockPos(minX, level.getMinY(), minZ);
        for (ResourceKey<PlacedFeature> ore : DEEP_ORES) {
            level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(ore)
                .ifPresent(feature -> feature.value().place(level, generator, random, chunkOrigin));
        }
        return placed;
    }

    // Turns every water block in the chunk's Nether Infection biomes into lava.
    private static boolean waterToLava(WorldGenLevel level, int minX, int minZ) {
        boolean changed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                for (int y = level.getMinY(); y < DungeonDimension.SANCTUARY_BOTTOM; y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    boolean water = state.getFluidState().is(FluidTags.WATER);
                    if (!water || !level.getBiome(pos).is(NETHER_INFESTATION)) {
                        continue;
                    }
                    if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                        level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), 2);
                    } else {
                        level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 2);
                    }
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean placeCore(WorldGenLevel level, RandomSource random, int minX, int minZ) {
        BlockPos floor = randomFloor(level, random, minX, minZ, CORE_EDGE_MARGIN, CORE_HEADROOM);
        if (floor == null) {
            return false;
        }
        int variant = random.nextInt(PORTAL_VARIANTS) + 1;
        Identifier id = Identifier.withDefaultNamespace("ruined_portal/portal_" + variant);
        StructureTemplate template = level.getLevel().getStructureTemplateManager().getOrCreate(id);
        Vec3i size = template.getSize(Rotation.NONE);
        BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
        BlockPos start = floor.above().subtract(pivot);
        StructurePlaceSettings settings = new StructurePlaceSettings()
            .setRotation(Rotation.getRandom(random))
            .setMirror(Mirror.NONE)
            .setRotationPivot(pivot)
            .setIgnoreEntities(true)
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
            .setBoundingBox(new BoundingBox(
                minX,
                level.getMinY(),
                minZ,
                minX + 15,
                level.getMaxY(),
                minZ + 15
            ));
        return template.placeInWorld(level, start, start, settings, random, 2);
    }

    private static boolean placeDebris(WorldGenLevel level, RandomSource random, int minX, int minZ) {
        BlockPos floor = randomFloor(level, random, minX, minZ, 0, 1);
        if (floor == null) {
            return false;
        }
        BlockPos target = floor.below();
        if (level.getBlockState(target).isAir()
            || level.getBlockState(target).is(Blocks.BEDROCK)
            || !level.getFluidState(target).isEmpty()) {
            return false;
        }
        level.setBlock(target, Blocks.ANCIENT_DEBRIS.defaultBlockState(), 2);
        return true;
    }

    // A random cave floor in a Nether Infection biome, from a few random columns of the chunk.
    private static BlockPos randomFloor(
        WorldGenLevel level,
        RandomSource random,
        int minX,
        int minZ,
        int margin,
        int headroom
    ) {
        for (int attempt = 0; attempt < COLUMN_ATTEMPTS; attempt++) {
            int x = minX + margin + random.nextInt(16 - 2 * margin);
            int z = minZ + margin + random.nextInt(16 - 2 * margin);
            List<BlockPos> floors = floorsIn(level, x, z, headroom);
            if (!floors.isEmpty()) {
                return floors.get(random.nextInt(floors.size()));
            }
        }
        return null;
    }

    private static List<BlockPos> floorsIn(WorldGenLevel level, int x, int z, int headroom) {
        List<BlockPos> floors = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, DungeonDimension.SANCTUARY_BOTTOM - 1, z);
        for (int y = DungeonDimension.SANCTUARY_BOTTOM - 1; y >= level.getMinY() + BEDROCK_CLEARANCE; y--) {
            pos.setY(y);
            if (isFloor(level, pos, headroom) && level.getBiome(pos).is(NETHER_INFESTATION)) {
                floors.add(pos.immutable());
            }
        }
        return floors;
    }

    private static boolean isFloor(WorldGenLevel level, BlockPos pos, int headroom) {
        if (level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        for (int i = 1; i <= headroom; i++) {
            if (!level.getBlockState(pos.above(i)).isAir()) {
                return false;
            }
        }
        return true;
    }

    private static long mix(long hash, int salt) {
        long h = hash + (salt + 1) * 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }
}
