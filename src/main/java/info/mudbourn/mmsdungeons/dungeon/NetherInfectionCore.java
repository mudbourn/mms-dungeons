package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

// Places a Nether Infection cavern's ruined portal core and its ancient debris, chunk by chunk.
public final class NetherInfectionCore implements Feature {

    private static final MapCodec<NetherInfectionCore> CODEC = MapCodec.unit(NetherInfectionCore::new);
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

    private static final int PORTAL_VARIANTS = 10;
    private static final int WRITE_REACH = 16;
    private static final int DEBRIS_MIN_RADIUS = 16;
    private static final int DEBRIS_RADIUS_SPREAD = 16;
    private static final int SCAN_ABOVE = 4;

    public static void register() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MmsDungeons.id("nether_infection_core"), CODEC);
        BiomeModifications.addFeature(
            BiomeSelectors.includeByKey(
                Biomes.NETHER_WASTES,
                Biomes.CRIMSON_FOREST,
                Biomes.WARPED_FOREST,
                Biomes.SOUL_SAND_VALLEY,
                Biomes.BASALT_DELTAS
            ),
            GenerationStep.Decoration.UNDERGROUND_DECORATION,
            PLACED
        );
        BiomeModifications.addFeature(
            BiomeSelectors.includeByKey(NETHER_DESCENT_BIOMES),
            GenerationStep.Decoration.UNDERGROUND_DECORATION,
            PLACED
        );
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
        FillCaverns.Cavern cavern = FillCaverns.cavernAtColumn(minX, minZ);
        if (cavern == null || !FillCaverns.isNetherInfection(cavern.biome())) {
            return false;
        }
        boolean placed = false;
        if (cavern.x() >> 4 == minX >> 4 && cavern.z() >> 4 == minZ >> 4) {
            placed = placeCore(level, cavern, minX, minZ);
        }
        return placeDebris(level, cavern, minX, minZ) || placed;
    }

    private static boolean placeCore(WorldGenLevel level, FillCaverns.Cavern cavern, int minX, int minZ) {
        int floor = floorBelow(
            level,
            cavern.x(),
            cavern.z(),
            cavern.y() + SCAN_ABOVE,
            cavern.y() - FillCaverns.HALF_HEIGHT
        );
        if (floor == Integer.MIN_VALUE) {
            return false;
        }
        RandomSource random = RandomSource.create(mix(cavern.hash(), 0));
        int variant = random.nextInt(PORTAL_VARIANTS) + 1;
        Identifier id = Identifier.withDefaultNamespace("ruined_portal/portal_" + variant);
        StructureTemplate template = level.getLevel().getStructureTemplateManager().getOrCreate(id);
        Vec3i size = template.getSize(Rotation.NONE);
        BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
        BlockPos start = new BlockPos(cavern.x(), floor + 1, cavern.z()).subtract(pivot);
        StructurePlaceSettings settings = new StructurePlaceSettings()
            .setRotation(Rotation.getRandom(random))
            .setMirror(Mirror.NONE)
            .setRotationPivot(pivot)
            .setIgnoreEntities(true)
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
            .setBoundingBox(new BoundingBox(
                minX - WRITE_REACH,
                level.getMinY(),
                minZ - WRITE_REACH,
                minX + WRITE_REACH + 15,
                level.getMaxY(),
                minZ + WRITE_REACH + 15
            ));
        return template.placeInWorld(level, start, start, settings, random, 2);
    }

    private static boolean placeDebris(WorldGenLevel level, FillCaverns.Cavern cavern, int minX, int minZ) {
        int count = 2 + (int) (mix(cavern.hash(), 1) >>> 63);
        double base = (mix(cavern.hash(), 2) >>> 11) * 0x1.0p-53 * Math.PI * 2;
        boolean placed = false;
        for (int i = 0; i < count; i++) {
            double angle = base + i * Math.PI * 2 / count;
            double radius = DEBRIS_MIN_RADIUS + (mix(cavern.hash(), 3 + i) >>> 11) * 0x1.0p-53 * DEBRIS_RADIUS_SPREAD;
            int x = cavern.x() + (int) Math.round(Math.cos(angle) * radius);
            int z = cavern.z() + (int) Math.round(Math.sin(angle) * radius);
            if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) {
                continue;
            }
            double ratio = radius / FillCaverns.RADIUS;
            int reach = (int) Math.ceil(FillCaverns.HALF_HEIGHT * Math.sqrt(1.0 - ratio * ratio));
            int floor = floorBelow(level, x, z, cavern.y() + reach, cavern.y() - reach - 2);
            if (floor == Integer.MIN_VALUE) {
                continue;
            }
            BlockPos target = new BlockPos(x, floor - 1, z);
            if (!level.getBlockState(target).isAir() && !level.getBlockState(target).is(Blocks.BEDROCK)) {
                level.setBlock(target, Blocks.ANCIENT_DEBRIS.defaultBlockState(), 2);
                placed = true;
            }
        }
        return placed;
    }

    private static int floorBelow(WorldGenLevel level, int x, int z, int top, int bottom) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
        for (int y = top; y >= bottom; y--) {
            pos.setY(y);
            boolean solid = !level.getBlockState(pos).isAir() && level.getFluidState(pos).isEmpty();
            if (solid && level.getBlockState(pos.above()).isAir()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static long mix(long hash, int salt) {
        long h = hash + (salt + 1) * 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }
}
