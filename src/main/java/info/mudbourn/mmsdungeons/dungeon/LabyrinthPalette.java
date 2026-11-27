package info.mudbourn.mmsdungeons.dungeon;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;

// Block sets and template themes for labyrinth dungeons, picked by the biome at the cell's core.
public final class LabyrinthPalette {

    private enum Role {
        BRICKS(null),
        CRACKED(BRICKS),
        MOSSY(BRICKS),
        CHISELED(BRICKS),
        BRICK_STAIRS(null),
        BRICK_SLAB(null),
        BRICK_WALL(null),
        COBBLE(BRICKS),
        COBBLE_STAIRS(BRICK_STAIRS),
        COBBLE_SLAB(BRICK_SLAB),
        COBBLE_WALL(BRICK_WALL),
        MOSSY_COBBLE(COBBLE),
        MOSSY_COBBLE_STAIRS(COBBLE_STAIRS),
        MOSSY_COBBLE_SLAB(COBBLE_SLAB),
        MOSSY_COBBLE_WALL(COBBLE_WALL),
        SMOOTH(BRICKS),
        SMOOTH_SLAB(BRICK_SLAB),
        STONE(COBBLE),
        STONE_STAIRS(COBBLE_STAIRS),
        STONE_SLAB(COBBLE_SLAB),
        DECOR(null),
        ACCENT(SMOOTH),
        ACCENT_STAIRS(BRICK_STAIRS),
        ACCENT_SLAB(SMOOTH_SLAB),
        ACCENT_WALL(BRICK_WALL),
        PLANKS(BRICKS),
        WOOD_STAIRS(BRICK_STAIRS),
        WOOD_SLAB(BRICK_SLAB),
        FENCE(DECOR),
        FENCE_GATE(DECOR),
        TRAPDOOR(DECOR),
        LOG(PLANKS),
        STRIPPED_LOG(LOG),
        WOOD(LOG),
        STRIPPED_WOOD(STRIPPED_LOG),
        PRESSURE_PLATE(DECOR),
        BUTTON(DECOR),
        WALL_SIGN(DECOR),
        BOOKSHELF(BRICKS),
        LECTERN(DECOR),
        TORCH(null),
        WALL_TORCH(null),
        LANTERN(null);

        private final Role fallback;

        Role(Role fallback) {
            this.fallback = fallback;
        }
    }

    private record Family(Map<Role, String> ids) {

        private Family with(Map<Role, String> more) {
            Map<Role, String> merged = new EnumMap<>(Role.class);
            merged.putAll(ids);
            merged.putAll(more);
            return new Family(merged);
        }

        // The first block on the role's fallback chain that exists, void air (removed) for decor, or null to keep the original.
        private Block resolve(Role role) {
            for (Role current = role; current != null; current = current.fallback) {
                String id = ids.get(current);
                if (id != null) {
                    Block block = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(null);
                    if (block != null) {
                        return block;
                    }
                }
                if (current == Role.DECOR) {
                    return Blocks.VOID_AIR;
                }
            }
            return null;
        }
    }

    private static final Map<Block, Role> SOURCES = sources();
    private static final Map<String, Family> BY_BIOME = byBiome();
    private static final Map<Family, DddMaterial> MATERIALS = new ConcurrentHashMap<>();
    private static final String LUSH = "minecraft:lush_caves";
    private static final String DESERT = "undergroundworlds:underground_desert";
    private static final String TUNDRA = "undergroundworlds:underground_tundra";
    private static final String NETHER = "minecraft:nether_wastes";
    private static final List<Family> FAMILIES = families();
    // Choice index for DDD's own blocks; family choices start at 1.
    private static final int DEFAULT = 0;
    // Chebyshev distance in cells inside which two labyrinths avoid sharing a palette.
    private static final int SPACING = 3;
    // Height at which a cell's preferred biome is read, the same for both halves.
    private static final int SAMPLE_Y = 30;
    // Heights at which a cell is checked for Nether Infection, one per level.
    private static final int[] NETHER_SAMPLE_YS = {SAMPLE_Y, -60};
    // Blocks from a cell's center to the ring where Nether Infection still counts as next to it.
    private static final int NETHER_REACH = 80;
    private static final long RANK_SALT = 0x70616C65747465L;
    private static final Map<Long, Integer> CHOICES = new ConcurrentHashMap<>();
    private static final Map<DddMaterial, DddMaterial> LUSH_MATERIALS = new ConcurrentHashMap<>();

    private LabyrinthPalette() {
    }

    // The block set for the labyrinth in the cell holding the given core position.
    public static DddMaterial at(ServerLevel level, ChunkGenerator generator, BlockPos core, DddRuin ruin) {
        BiomeResolver resolver = generator.getBiomeSource().createUncachedResolver(level.getChunkSource().randomState());
        int choice = choice(resolver, LabyrinthCells.cellOfBlock(core.getX()), LabyrinthCells.cellOfBlock(core.getZ()));
        if (choice == DEFAULT) {
            return DddMaterial.of(ruin);
        }
        Family family = FAMILIES.get(choice - 1);
        DddMaterial material = MATERIALS.computeIfAbsent(family, LabyrinthPalette::build);
        return family == BY_BIOME.get(LUSH) ? LUSH_MATERIALS.computeIfAbsent(material, plain -> plain.withAccent(LushAccent.INSTANCE)) : material;
    }

    // The template theme for the labyrinth in a cell: desert and tundra palettes use their own templates, everything else plains.
    public static DddRuin.Theme theme(BiomeResolver resolver, int cellX, int cellZ) {
        int choice = choice(resolver, cellX, cellZ);
        if (choice == DEFAULT) {
            return DddRuin.Theme.PLAINS;
        }
        Family family = FAMILIES.get(choice - 1);
        if (family == BY_BIOME.get(DESERT)) {
            return DddRuin.Theme.DESERT;
        }
        return family == BY_BIOME.get(TUNDRA) ? DddRuin.Theme.FROZEN : DddRuin.Theme.PLAINS;
    }

    public static void clearChoices() {
        CHOICES.clear();
    }

    // The cell's palette: its biome's own unless a higher ranked labyrinth cell within SPACING already took it.
    private static int choice(BiomeResolver resolver, int cellX, int cellZ) {
        long key = ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
        Integer known = CHOICES.get(key);
        if (known != null) {
            return known;
        }
        long seed = LabyrinthCells.seed();
        long rank = rank(seed, cellX, cellZ);
        int options = FAMILIES.size() + 1;
        int[] nearest = new int[options];
        Arrays.fill(nearest, Integer.MAX_VALUE);
        for (int dx = -SPACING; dx <= SPACING; dx++) {
            for (int dz = -SPACING; dz <= SPACING; dz++) {
                int otherX = cellX + dx;
                int otherZ = cellZ + dz;
                if ((dx == 0 && dz == 0) || rank(seed, otherX, otherZ) <= rank || !LabyrinthCells.isLabyrinth(seed, otherX, otherZ)) {
                    continue;
                }
                int taken = choice(resolver, otherX, otherZ);
                nearest[taken] = Math.min(nearest[taken], Math.max(Math.abs(dx), Math.abs(dz)));
            }
        }
        int preferred = preferred(resolver, cellX, cellZ);
        int picked = preferred;
        for (int step = 0; step < options; step++) {
            int candidate = (preferred + step) % options;
            if (nearest[candidate] == Integer.MAX_VALUE) {
                picked = candidate;
                break;
            }
            if (nearest[candidate] > nearest[picked]) {
                picked = candidate;
            }
        }
        CHOICES.put(key, picked);
        return picked;
    }

    private static int preferred(BiomeResolver resolver, int cellX, int cellZ) {
        int centerX = cellX * LabyrinthCells.CELL_BLOCKS + LabyrinthCells.CELL_BLOCKS / 2;
        int centerZ = cellZ * LabyrinthCells.CELL_BLOCKS + LabyrinthCells.CELL_BLOCKS / 2;
        if (nearNether(resolver, centerX, centerZ)) {
            return FAMILIES.indexOf(BY_BIOME.get(NETHER)) + 1;
        }
        Holder<Biome> biome = resolver.getNoiseBiome(
            QuartPos.fromBlock(centerX),
            QuartPos.fromBlock(SAMPLE_Y),
            QuartPos.fromBlock(centerZ)
        );
        Family family = biome.unwrapKey().map(key -> BY_BIOME.get(key.identifier().toString())).orElse(null);
        return family == null ? DEFAULT : FAMILIES.indexOf(family) + 1;
    }

    // Whether a Nether Infection biome sits at the cell's center or around it, at an upper or a lower height.
    private static boolean nearNether(BiomeResolver resolver, int centerX, int centerZ) {
        Family nether = BY_BIOME.get(NETHER);
        for (int y : NETHER_SAMPLE_YS) {
            for (int dx = -NETHER_REACH; dx <= NETHER_REACH; dx += NETHER_REACH) {
                for (int dz = -NETHER_REACH; dz <= NETHER_REACH; dz += NETHER_REACH) {
                    Holder<Biome> biome = resolver.getNoiseBiome(
                        QuartPos.fromBlock(centerX + dx),
                        QuartPos.fromBlock(y),
                        QuartPos.fromBlock(centerZ + dz)
                    );
                    if (biome.unwrapKey().map(key -> BY_BIOME.get(key.identifier().toString())).orElse(null) == nether) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Rank used to settle which of two nearby cells keeps a palette; ties fall to the coordinates.
    private static long rank(long seed, int cellX, int cellZ) {
        long hash = LabyrinthCells.mix(DungeonGeneration.layoutSeed(seed) ^ RANK_SALT ^ LabyrinthCells.mix(cellX * 0x9E3779B97F4A7C15L + cellZ));
        return (hash & ~0xFFFFL) | ((cellX * 31L + cellZ) & 0xFFFFL);
    }

    private static List<Family> families() {
        Map<Family, String> firstBiome = new HashMap<>();
        for (Map.Entry<String, Family> entry : BY_BIOME.entrySet()) {
            firstBiome.merge(entry.getValue(), entry.getKey(), (a, b) -> a.compareTo(b) <= 0 ? a : b);
        }
        return firstBiome.keySet().stream().sorted(Comparator.comparing(firstBiome::get)).toList();
    }

    private static DddMaterial build(Family family) {
        Map<Block, Block> mapping = new HashMap<>();
        for (Map.Entry<Block, Role> source : SOURCES.entrySet()) {
            Block target = family.resolve(source.getValue());
            if (target != null && target != source.getKey()) {
                mapping.put(source.getKey(), target);
            }
        }
        return new DddMaterial(mapping);
    }

    private static Map<Block, Role> sources() {
        Map<Block, Role> map = new HashMap<>();
        map.put(Blocks.STONE, Role.STONE);
        map.put(Blocks.STONE_STAIRS, Role.STONE_STAIRS);
        map.put(Blocks.STONE_SLAB, Role.STONE_SLAB);
        map.put(Blocks.SMOOTH_STONE, Role.SMOOTH);
        map.put(Blocks.SMOOTH_STONE_SLAB, Role.SMOOTH_SLAB);
        map.put(Blocks.COBBLESTONE, Role.COBBLE);
        map.put(Blocks.COBBLESTONE_STAIRS, Role.COBBLE_STAIRS);
        map.put(Blocks.COBBLESTONE_SLAB, Role.COBBLE_SLAB);
        map.put(Blocks.COBBLESTONE_WALL, Role.COBBLE_WALL);
        map.put(Blocks.MOSSY_COBBLESTONE, Role.MOSSY_COBBLE);
        map.put(Blocks.MOSSY_COBBLESTONE_STAIRS, Role.MOSSY_COBBLE_STAIRS);
        map.put(Blocks.MOSSY_COBBLESTONE_SLAB, Role.MOSSY_COBBLE_SLAB);
        map.put(Blocks.MOSSY_COBBLESTONE_WALL, Role.MOSSY_COBBLE_WALL);
        map.put(Blocks.STONE_BRICKS, Role.BRICKS);
        map.put(Blocks.CRACKED_STONE_BRICKS, Role.CRACKED);
        map.put(Blocks.MOSSY_STONE_BRICKS, Role.MOSSY);
        map.put(Blocks.CHISELED_STONE_BRICKS, Role.CHISELED);
        map.put(Blocks.STONE_BRICK_STAIRS, Role.BRICK_STAIRS);
        map.put(Blocks.STONE_BRICK_SLAB, Role.BRICK_SLAB);
        map.put(Blocks.STONE_BRICK_WALL, Role.BRICK_WALL);
        map.put(Blocks.MOSSY_STONE_BRICK_STAIRS, Role.BRICK_STAIRS);
        map.put(Blocks.MOSSY_STONE_BRICK_SLAB, Role.BRICK_SLAB);
        map.put(Blocks.MOSSY_STONE_BRICK_WALL, Role.BRICK_WALL);
        map.put(Blocks.INFESTED_STONE, Role.STONE);
        map.put(Blocks.INFESTED_COBBLESTONE, Role.COBBLE);
        map.put(Blocks.INFESTED_STONE_BRICKS, Role.BRICKS);
        map.put(Blocks.INFESTED_CRACKED_STONE_BRICKS, Role.CRACKED);
        map.put(Blocks.INFESTED_MOSSY_STONE_BRICKS, Role.MOSSY);
        map.put(Blocks.INFESTED_CHISELED_STONE_BRICKS, Role.CHISELED);
        map.put(Blocks.DEEPSLATE, Role.STONE);
        map.put(Blocks.INFESTED_DEEPSLATE, Role.STONE);
        map.put(Blocks.COBBLED_DEEPSLATE, Role.COBBLE);
        map.put(Blocks.COBBLED_DEEPSLATE_STAIRS, Role.COBBLE_STAIRS);
        map.put(Blocks.COBBLED_DEEPSLATE_SLAB, Role.COBBLE_SLAB);
        map.put(Blocks.COBBLED_DEEPSLATE_WALL, Role.COBBLE_WALL);
        map.put(Blocks.POLISHED_DEEPSLATE, Role.SMOOTH);
        map.put(Blocks.POLISHED_DEEPSLATE_SLAB, Role.SMOOTH_SLAB);
        map.put(Blocks.POLISHED_DEEPSLATE_STAIRS, Role.BRICK_STAIRS);
        map.put(Blocks.POLISHED_DEEPSLATE_WALL, Role.BRICK_WALL);
        map.put(Blocks.DEEPSLATE_BRICKS, Role.BRICKS);
        map.put(Blocks.CRACKED_DEEPSLATE_BRICKS, Role.CRACKED);
        map.put(Blocks.DEEPSLATE_BRICK_STAIRS, Role.BRICK_STAIRS);
        map.put(Blocks.DEEPSLATE_BRICK_SLAB, Role.BRICK_SLAB);
        map.put(Blocks.DEEPSLATE_BRICK_WALL, Role.BRICK_WALL);
        map.put(Blocks.DEEPSLATE_TILES, Role.BRICKS);
        map.put(Blocks.CRACKED_DEEPSLATE_TILES, Role.CRACKED);
        map.put(Blocks.DEEPSLATE_TILE_STAIRS, Role.BRICK_STAIRS);
        map.put(Blocks.DEEPSLATE_TILE_SLAB, Role.BRICK_SLAB);
        map.put(Blocks.DEEPSLATE_TILE_WALL, Role.BRICK_WALL);
        map.put(Blocks.CHISELED_DEEPSLATE, Role.CHISELED);
        map.put(Blocks.REINFORCED_DEEPSLATE, Role.BRICKS);
        for (String wood : List.of("spruce", "oak", "birch")) {
            source(map, wood + "_planks", Role.PLANKS);
            source(map, wood + "_stairs", Role.WOOD_STAIRS);
            source(map, wood + "_slab", Role.WOOD_SLAB);
            source(map, wood + "_fence", Role.FENCE);
            source(map, wood + "_fence_gate", Role.FENCE_GATE);
            source(map, wood + "_trapdoor", Role.TRAPDOOR);
            source(map, wood + "_log", Role.LOG);
            source(map, "stripped_" + wood + "_log", Role.STRIPPED_LOG);
            source(map, wood + "_wood", Role.WOOD);
            source(map, "stripped_" + wood + "_wood", Role.STRIPPED_WOOD);
            source(map, wood + "_pressure_plate", Role.PRESSURE_PLATE);
            source(map, wood + "_button", Role.BUTTON);
            source(map, wood + "_wall_sign", Role.WALL_SIGN);
        }
        for (String accent : List.of("andesite", "diorite", "granite", "polished_andesite", "polished_diorite", "polished_granite", "tuff", "polished_tuff", "sandstone", "smooth_sandstone", "cut_sandstone")) {
            source(map, accent, Role.ACCENT);
            source(map, accent + "_stairs", Role.ACCENT_STAIRS);
            source(map, accent + "_slab", Role.ACCENT_SLAB);
            source(map, accent + "_wall", Role.ACCENT_WALL);
        }
        map.put(Blocks.BRICKS, Role.ACCENT);
        map.put(Blocks.BRICK_STAIRS, Role.ACCENT_STAIRS);
        map.put(Blocks.BRICK_SLAB, Role.ACCENT_SLAB);
        map.put(Blocks.BRICK_WALL, Role.ACCENT_WALL);
        map.put(Blocks.TUFF_BRICKS, Role.ACCENT);
        map.put(Blocks.CHISELED_TUFF, Role.CHISELED);
        map.put(Blocks.CHISELED_SANDSTONE, Role.CHISELED);
        map.put(Blocks.CALCITE, Role.ACCENT);
        map.put(Blocks.GRAVEL, Role.COBBLE);
        map.put(Blocks.BOOKSHELF, Role.BOOKSHELF);
        map.put(Blocks.CHISELED_BOOKSHELF, Role.BOOKSHELF);
        map.put(Blocks.LECTERN, Role.LECTERN);
        map.put(Blocks.TORCH, Role.TORCH);
        map.put(Blocks.WALL_TORCH, Role.WALL_TORCH);
        map.put(Blocks.LANTERN, Role.LANTERN);
        return map;
    }

    private static void source(Map<Block, Role> map, String path, Role role) {
        BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(path)).ifPresent(block -> map.put(block, role));
    }

    // A full wood set named the vanilla and Oh The Biomes We've Gone way, with that set's own bookshelf if it has one.
    private static Map<Role, String> wood(String namespace, String name) {
        String n = namespace + ":" + name;
        String stripped = namespace + ":stripped_" + name;
        Map<Role, String> ids = new EnumMap<>(Role.class);
        ids.put(Role.PLANKS, n + "_planks");
        ids.put(Role.WOOD_STAIRS, n + "_stairs");
        ids.put(Role.WOOD_SLAB, n + "_slab");
        ids.put(Role.FENCE, n + "_fence");
        ids.put(Role.FENCE_GATE, n + "_fence_gate");
        ids.put(Role.TRAPDOOR, n + "_trapdoor");
        ids.put(Role.LOG, n + "_log");
        ids.put(Role.STRIPPED_LOG, stripped + "_log");
        ids.put(Role.WOOD, n + "_wood");
        ids.put(Role.STRIPPED_WOOD, stripped + "_wood");
        ids.put(Role.PRESSURE_PLATE, n + "_pressure_plate");
        ids.put(Role.BUTTON, n + "_button");
        ids.put(Role.WALL_SIGN, n + "_wall_sign");
        ids.put(Role.BOOKSHELF, n + "_bookshelf");
        return ids;
    }

    // A nether wood: stems and hyphae instead of logs and wood.
    private static Map<Role, String> stem(String name) {
        Map<Role, String> ids = wood("minecraft", name);
        ids.put(Role.LOG, "minecraft:" + name + "_stem");
        ids.put(Role.STRIPPED_LOG, "minecraft:stripped_" + name + "_stem");
        ids.put(Role.WOOD, "minecraft:" + name + "_hyphae");
        ids.put(Role.STRIPPED_WOOD, "minecraft:stripped_" + name + "_hyphae");
        return ids;
    }

    // A vanilla overworld wood, keeping the plain bookshelf that already suits it.
    private static Map<Role, String> vanilla(String name) {
        Map<Role, String> ids = wood("minecraft", name);
        ids.put(Role.BOOKSHELF, "minecraft:bookshelf");
        return ids;
    }

    private static Map<Role, String> aerialTree() {
        Map<Role, String> ids = wood("aerialhell", "aerial_tree");
        ids.put(Role.FENCE_GATE, "aerialhell:aerial_tree_gate");
        return ids;
    }

    private static Map<Role, String> bamboo() {
        Map<Role, String> ids = wood("minecraft", "bamboo");
        ids.put(Role.LOG, "minecraft:bamboo_block");
        ids.put(Role.STRIPPED_LOG, "minecraft:stripped_bamboo_block");
        ids.put(Role.WOOD, "minecraft:bamboo_block");
        ids.put(Role.STRIPPED_WOOD, "minecraft:stripped_bamboo_block");
        return ids;
    }

    private static Family family(String... pairs) {
        Map<Role, String> ids = new EnumMap<>(Role.class);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            ids.put(Role.valueOf(pairs[i]), pairs[i + 1]);
        }
        return new Family(ids);
    }

    private static void put(Map<String, Family> map, Family family, List<String> biomes) {
        for (String biome : biomes) {
            map.put(biome, family);
        }
    }

    private static Map<String, Family> byBiome() {
        Map<String, Family> map = new HashMap<>();
        put(map, family(
            "BRICKS", "minecraft:deepslate_bricks",
            "CRACKED", "minecraft:cracked_deepslate_bricks",
            "MOSSY", "minecraft:deepslate_tiles",
            "CHISELED", "minecraft:chiseled_deepslate",
            "BRICK_STAIRS", "minecraft:deepslate_brick_stairs",
            "BRICK_SLAB", "minecraft:deepslate_brick_slab",
            "BRICK_WALL", "minecraft:deepslate_brick_wall",
            "COBBLE", "minecraft:cobbled_deepslate",
            "COBBLE_STAIRS", "minecraft:cobbled_deepslate_stairs",
            "COBBLE_SLAB", "minecraft:cobbled_deepslate_slab",
            "COBBLE_WALL", "minecraft:cobbled_deepslate_wall",
            "MOSSY_COBBLE", "minecraft:deepslate_tiles",
            "MOSSY_COBBLE_STAIRS", "minecraft:deepslate_tile_stairs",
            "MOSSY_COBBLE_SLAB", "minecraft:deepslate_tile_slab",
            "MOSSY_COBBLE_WALL", "minecraft:deepslate_tile_wall",
            "SMOOTH", "minecraft:polished_deepslate",
            "SMOOTH_SLAB", "minecraft:polished_deepslate_slab",
            "STONE", "minecraft:polished_deepslate",
            "STONE_STAIRS", "minecraft:polished_deepslate_stairs",
            "STONE_SLAB", "minecraft:polished_deepslate_slab"
        ).with(wood("biomeswevegone", "ebony")), List.of("minecraft:deep_dark"));
        put(map, family(
            "BRICKS", "minecraft:tuff_bricks",
            "MOSSY", "minecraft:polished_tuff",
            "CHISELED", "minecraft:chiseled_tuff_bricks",
            "BRICK_STAIRS", "minecraft:tuff_brick_stairs",
            "BRICK_SLAB", "minecraft:tuff_brick_slab",
            "BRICK_WALL", "minecraft:tuff_brick_wall",
            "COBBLE", "minecraft:tuff",
            "COBBLE_STAIRS", "minecraft:tuff_stairs",
            "COBBLE_SLAB", "minecraft:tuff_slab",
            "COBBLE_WALL", "minecraft:tuff_wall",
            "MOSSY_COBBLE", "minecraft:chiseled_tuff",
            "MOSSY_COBBLE_STAIRS", "minecraft:polished_tuff_stairs",
            "MOSSY_COBBLE_SLAB", "minecraft:polished_tuff_slab",
            "MOSSY_COBBLE_WALL", "minecraft:polished_tuff_wall",
            "SMOOTH", "minecraft:polished_tuff",
            "SMOOTH_SLAB", "minecraft:polished_tuff_slab"
        ).with(vanilla("spruce")), List.of("minecraft:dripstone_caves"));
        put(map, family(
            "BRICKS", "minecraft:stone_bricks",
            "CRACKED", "minecraft:cracked_stone_bricks",
            "MOSSY", "minecraft:stone_bricks",
            "CHISELED", "minecraft:chiseled_stone_bricks",
            "BRICK_STAIRS", "minecraft:stone_brick_stairs",
            "BRICK_SLAB", "minecraft:stone_brick_slab",
            "BRICK_WALL", "minecraft:stone_brick_wall",
            "COBBLE", "minecraft:cobblestone",
            "COBBLE_STAIRS", "minecraft:cobblestone_stairs",
            "COBBLE_SLAB", "minecraft:cobblestone_slab",
            "COBBLE_WALL", "minecraft:cobblestone_wall",
            "MOSSY_COBBLE", "minecraft:cobblestone",
            "MOSSY_COBBLE_STAIRS", "minecraft:cobblestone_stairs",
            "MOSSY_COBBLE_SLAB", "minecraft:cobblestone_slab",
            "MOSSY_COBBLE_WALL", "minecraft:cobblestone_wall",
            "SMOOTH", "minecraft:stone_bricks",
            "SMOOTH_SLAB", "minecraft:stone_brick_slab"
        ).with(wood("biomeswevegone", "willow")), List.of("minecraft:lush_caves"));
        put(map, family(
            "BRICKS", "minecraft:sulfur_bricks",
            "CRACKED", "minecraft:cinnabar_bricks",
            "MOSSY", "minecraft:polished_cinnabar",
            "CHISELED", "minecraft:chiseled_sulfur",
            "BRICK_STAIRS", "minecraft:sulfur_brick_stairs",
            "BRICK_SLAB", "minecraft:sulfur_brick_slab",
            "BRICK_WALL", "minecraft:sulfur_brick_wall",
            "COBBLE", "minecraft:sulfur",
            "COBBLE_STAIRS", "minecraft:sulfur_stairs",
            "COBBLE_SLAB", "minecraft:sulfur_slab",
            "COBBLE_WALL", "minecraft:sulfur_wall",
            "MOSSY_COBBLE", "minecraft:cinnabar",
            "MOSSY_COBBLE_STAIRS", "minecraft:cinnabar_stairs",
            "MOSSY_COBBLE_SLAB", "minecraft:cinnabar_slab",
            "MOSSY_COBBLE_WALL", "minecraft:cinnabar_wall",
            "SMOOTH", "minecraft:polished_sulfur",
            "SMOOTH_SLAB", "minecraft:polished_sulfur_slab"
        ).with(bamboo()), List.of("minecraft:sulfur_caves"));
        put(map, family(
            "BRICKS", "mms_dungeons:orpiment_amber_bricks",
            "CRACKED", "mms_dungeons:orpiment_hollowed_amber_bricks",
            "CHISELED", "mms_dungeons:orpiment_chiseled_amber",
            "BRICK_STAIRS", "mms_dungeons:orpiment_amber_brick_stairs",
            "BRICK_SLAB", "mms_dungeons:orpiment_amber_brick_slab",
            "BRICK_WALL", "mms_dungeons:orpiment_amber_brick_wall",
            "COBBLE", "mms_dungeons:orpiment_amber",
            "COBBLE_STAIRS", "mms_dungeons:orpiment_amber_stairs",
            "COBBLE_SLAB", "mms_dungeons:orpiment_amber_slab",
            "COBBLE_WALL", "mms_dungeons:orpiment_amber_wall",
            "SMOOTH", "mms_dungeons:orpiment_amber_pillar",
            "STONE", "mms_dungeons:orpiment_lignite"
        ).with(wood("biomeswevegone", "zelkova")), List.of("mms_dungeons:amber_caves"));
        put(map, family(
            "BRICKS", "minecraft:nether_bricks",
            "CRACKED", "minecraft:cracked_nether_bricks",
            "MOSSY", "minecraft:red_nether_bricks",
            "CHISELED", "minecraft:chiseled_nether_bricks",
            "BRICK_STAIRS", "minecraft:nether_brick_stairs",
            "BRICK_SLAB", "minecraft:nether_brick_slab",
            "BRICK_WALL", "minecraft:nether_brick_wall",
            "COBBLE", "minecraft:blackstone",
            "COBBLE_STAIRS", "minecraft:blackstone_stairs",
            "COBBLE_SLAB", "minecraft:blackstone_slab",
            "COBBLE_WALL", "minecraft:blackstone_wall",
            "MOSSY_COBBLE", "minecraft:red_nether_bricks",
            "MOSSY_COBBLE_STAIRS", "minecraft:red_nether_brick_stairs",
            "MOSSY_COBBLE_SLAB", "minecraft:red_nether_brick_slab",
            "MOSSY_COBBLE_WALL", "minecraft:red_nether_brick_wall",
            "SMOOTH", "minecraft:polished_blackstone",
            "SMOOTH_SLAB", "minecraft:polished_blackstone_slab",
            "STONE", "minecraft:netherrack",
            "STONE_STAIRS", "minecraft:polished_blackstone_stairs",
            "STONE_SLAB", "minecraft:polished_blackstone_slab",
            "ACCENT", "minecraft:polished_basalt",
            "TORCH", "minecraft:soul_torch",
            "WALL_TORCH", "minecraft:soul_wall_torch",
            "LANTERN", "minecraft:soul_lantern"
        ).with(stem("crimson")), List.of(
            "minecraft:nether_wastes",
            "minecraft:crimson_forest",
            "minecraft:warped_forest",
            "minecraft:soul_sand_valley",
            "minecraft:basalt_deltas",
            "netherdescent:arisian_undergrowth",
            "netherdescent:crimson_gardens",
            "netherdescent:embur_bog",
            "netherdescent:sythian_torrids",
            "netherdescent:wailing_garth"
        ));
        put(map, family(
            "BRICKS", "undergroundworlds:pyramid_bricks",
            "CRACKED", "undergroundworlds:cracked_pyramid_bricks",
            "BRICK_STAIRS", "undergroundworlds:pyramid_brick_stairs",
            "BRICK_SLAB", "undergroundworlds:pyramid_brick_slab",
            "BRICK_WALL", "undergroundworlds:pyramid_brick_wall"
        ).with(wood("biomeswevegone", "palm")), List.of(DESERT));
        put(map, family(
            "BRICKS", "undergroundworlds:temple_bricks",
            "CRACKED", "undergroundworlds:cracked_temple_bricks",
            "MOSSY", "undergroundworlds:mossy_temple_bricks",
            "CHISELED", "undergroundworlds:chiseled_temple_bricks",
            "BRICK_STAIRS", "undergroundworlds:temple_brick_stairs",
            "BRICK_SLAB", "undergroundworlds:temple_brick_slab",
            "BRICK_WALL", "undergroundworlds:temple_brick_wall",
            "COBBLE", "undergroundworlds:temple_brick_tiles",
            "COBBLE_STAIRS", "undergroundworlds:temple_brick_tile_stairs",
            "COBBLE_SLAB", "undergroundworlds:temple_brick_tile_slab",
            "COBBLE_WALL", "undergroundworlds:temple_brick_tile_wall"
        ).with(vanilla("jungle")), List.of("undergroundworlds:underground_jungle"));
        put(map, family(
            "BRICKS", "undergroundworlds:ice_bricks",
            "BRICK_STAIRS", "undergroundworlds:ice_brick_stairs",
            "BRICK_SLAB", "undergroundworlds:ice_brick_slab",
            "BRICK_WALL", "undergroundworlds:ice_brick_wall",
            "COBBLE", "minecraft:packed_ice",
            "SMOOTH", "minecraft:packed_ice",
            "STONE", "minecraft:packed_ice"
        ).with(vanilla("pale_oak")), List.of(TUNDRA));
        put(map, family(
            "BRICKS", "minecraft:mud_bricks",
            "BRICK_STAIRS", "minecraft:mud_brick_stairs",
            "BRICK_SLAB", "minecraft:mud_brick_slab",
            "BRICK_WALL", "minecraft:mud_brick_wall",
            "STONE", "minecraft:packed_mud"
        ).with(vanilla("mangrove")), List.of("undergroundworlds:mushroom_cave", "undergroundworlds:glowshroom_cave"));
        put(map, family(
            "BRICKS", "minecraft:mossy_cobblestone",
            "CHISELED", "minecraft:mossy_stone_bricks",
            "BRICK_STAIRS", "minecraft:mossy_cobblestone_stairs",
            "BRICK_SLAB", "minecraft:mossy_cobblestone_slab",
            "BRICK_WALL", "minecraft:mossy_cobblestone_wall"
        ).with(vanilla("dark_oak")), List.of("undergroundworlds:spider_cave"));
        put(map, family(
            "BRICKS", "alexscaves:galena_bricks",
            "CHISELED", "alexscaves:galena_pillar",
            "BRICK_STAIRS", "alexscaves:galena_brick_stairs",
            "BRICK_SLAB", "alexscaves:galena_brick_slab",
            "BRICK_WALL", "alexscaves:galena_brick_wall",
            "COBBLE", "alexscaves:galena",
            "COBBLE_STAIRS", "alexscaves:galena_stairs",
            "COBBLE_SLAB", "alexscaves:galena_slab",
            "COBBLE_WALL", "alexscaves:galena_wall",
            "SMOOTH", "alexscaves:packed_galena"
        ).with(wood("biomeswevegone", "ironwood")), List.of("alexscaves:magnetic_caves"));
        put(map, family(
            "BRICKS", "alexscaves:smooth_limestone",
            "CHISELED", "alexscaves:limestone_chiseled",
            "MOSSY", "alexscaves:limestone_pillar",
            "BRICK_STAIRS", "alexscaves:smooth_limestone_stairs",
            "BRICK_SLAB", "alexscaves:smooth_limestone_slab",
            "BRICK_WALL", "alexscaves:smooth_limestone_wall",
            "COBBLE", "alexscaves:limestone",
            "COBBLE_STAIRS", "alexscaves:limestone_stairs",
            "COBBLE_SLAB", "alexscaves:limestone_slab",
            "COBBLE_WALL", "alexscaves:limestone_wall"
        ).with(wood("alexscaves", "pewen")), List.of("alexscaves:primordial_caves"));
        put(map, family(
            "BRICKS", "alexscaves:radrock_bricks",
            "CHISELED", "alexscaves:radrock_chiseled",
            "BRICK_STAIRS", "alexscaves:radrock_brick_stairs",
            "BRICK_SLAB", "alexscaves:radrock_brick_slab",
            "BRICK_WALL", "alexscaves:radrock_brick_wall",
            "COBBLE", "alexscaves:radrock",
            "COBBLE_STAIRS", "alexscaves:radrock_stairs",
            "COBBLE_SLAB", "alexscaves:radrock_slab",
            "COBBLE_WALL", "alexscaves:radrock_wall"
        ).with(wood("biomeswevegone", "ebony")), List.of("alexscaves:toxic_caves"));
        put(map, family(
            "BRICKS", "alexscaves:abyssmarine_bricks",
            "CRACKED", "alexscaves:abyssmarine_tiles",
            "CHISELED", "alexscaves:abyssmarine_pillar",
            "BRICK_STAIRS", "alexscaves:abyssmarine_brick_stairs",
            "BRICK_SLAB", "alexscaves:abyssmarine_brick_slab",
            "BRICK_WALL", "alexscaves:abyssmarine_brick_wall",
            "COBBLE", "alexscaves:abyssmarine",
            "COBBLE_STAIRS", "alexscaves:abyssmarine_stairs",
            "COBBLE_SLAB", "alexscaves:abyssmarine_slab",
            "COBBLE_WALL", "alexscaves:abyssmarine_wall"
        ).with(wood("biomeswevegone", "skyris")), List.of("alexscaves:abyssal_chasm"));
        put(map, family(
            "BRICKS", "alexscaves:guanostone_bricks",
            "CRACKED", "alexscaves:guanostone_tiles",
            "CHISELED", "alexscaves:guanostone_chiseled",
            "BRICK_STAIRS", "alexscaves:guanostone_brick_stairs",
            "BRICK_SLAB", "alexscaves:guanostone_brick_slab",
            "BRICK_WALL", "alexscaves:guanostone_brick_wall",
            "COBBLE", "alexscaves:guanostone",
            "COBBLE_STAIRS", "alexscaves:guanostone_stairs",
            "COBBLE_SLAB", "alexscaves:guanostone_slab",
            "COBBLE_WALL", "alexscaves:guanostone_wall"
        ).with(wood("alexscaves", "thornwood")), List.of("alexscaves:forlorn_hollows"));
        put(map, family(
            "BRICKS", "alexscaves:gingerbread_bricks",
            "MOSSY", "alexscaves:frosted_gingerbread_bricks",
            "BRICK_STAIRS", "alexscaves:gingerbread_brick_stairs",
            "BRICK_SLAB", "alexscaves:gingerbread_brick_slab",
            "BRICK_WALL", "alexscaves:gingerbread_brick_wall",
            "COBBLE", "alexscaves:gingerbread_block",
            "COBBLE_STAIRS", "alexscaves:gingerbread_stairs",
            "COBBLE_SLAB", "alexscaves:gingerbread_slab",
            "COBBLE_WALL", "alexscaves:gingerbread_wall",
            "MOSSY_COBBLE", "alexscaves:frosted_gingerbread_block",
            "MOSSY_COBBLE_STAIRS", "alexscaves:frosted_gingerbread_stairs",
            "MOSSY_COBBLE_SLAB", "alexscaves:frosted_gingerbread_slab",
            "MOSSY_COBBLE_WALL", "alexscaves:frosted_gingerbread_wall"
        ).with(vanilla("cherry")), List.of("alexscaves:candy_cavity"));
        put(map, family(
            "BRICKS", "aerialhell:stellar_stone_bricks",
            "BRICK_STAIRS", "aerialhell:stellar_stone_bricks_stairs",
            "BRICK_SLAB", "aerialhell:stellar_stone_bricks_slab",
            "BRICK_WALL", "aerialhell:stellar_stone_bricks_wall",
            "COBBLE", "aerialhell:stellar_stone",
            "COBBLE_STAIRS", "aerialhell:stellar_stone_stairs",
            "COBBLE_SLAB", "aerialhell:stellar_stone_slab",
            "COBBLE_WALL", "aerialhell:stellar_stone_wall",
            "MOSSY_COBBLE", "aerialhell:mossy_stellar_stone",
            "MOSSY_COBBLE_STAIRS", "aerialhell:mossy_stellar_stone_stairs",
            "MOSSY_COBBLE_SLAB", "aerialhell:mossy_stellar_stone_slab",
            "MOSSY_COBBLE_WALL", "aerialhell:mossy_stellar_stone_wall"
        ).with(aerialTree()), List.of("mms_dungeons:aerial_hell_cavern"));
        return map;
    }
}
