package info.mudbourn.mmsdungeons.mobs.orpiment;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.AmberBlock;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.AmberDruseBlock;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.AmberLampBlock;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.BurstSpawnerBlock;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.CrawEggBlock;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.EctoplasmScaffoldBlock;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.PetrifiedRootsBlock;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.RelaySpawnerBlock;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.TarBlock;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.TarCauldronBlock;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.WraithAltarBlock;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.entity.BurstSpawnerBlockEntity;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.entity.CrawEggBlockEntity;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.entity.RelaySpawnerBlockEntity;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.util.ColorRGBA;

public final class OrpimentBlocks {

    public static Block TAR;
    public static Block TAR_CAULDRON;
    public static Block TARRED_SAND;
    public static Block TARRED_SANDSTONE;
    public static Block TARRED_SANDSTONE_SLAB;
    public static Block TARRED_SANDSTONE_STAIRS;
    public static Block TARRED_SANDSTONE_WALL;
    public static Block CUT_TARRED_SANDSTONE;
    public static Block CUT_TARRED_SANDSTONE_SLAB;
    public static Block SMOOTH_TARRED_SANDSTONE;
    public static Block SMOOTH_TARRED_SANDSTONE_SLAB;
    public static Block SMOOTH_TARRED_SANDSTONE_STAIRS;
    public static Block CHISELED_TARRED_SANDSTONE;
    public static Block LIGNITE;
    public static Block PETRIFIED_LOG;
    public static Block PETRIFIED_ROOTS;
    public static Block AMBER;
    public static Block AMBER_BRICKS;
    public static Block AMBER_BRICK_SLAB;
    public static Block AMBER_BRICK_STAIRS;
    public static Block AMBER_BRICK_WALL;
    public static Block AMBER_PILLAR;
    public static Block CHISELED_AMBER;
    public static Block AMBER_SLAB;
    public static Block AMBER_STAIRS;
    public static Block AMBER_WALL;
    public static Block AMBER_LAMP;
    public static Block AMBER_DRUSE;
    public static Block HOLLOWED_AMBER_BRICKS;
    public static Block WRAITH_ALTAR;
    public static Block ECTOPLASM_SCAFFOLD;
    public static Block BURST_SPAWNER;
    public static Block RELAY_SPAWNER;
    public static Block CRAW_EGG;
    public static BlockEntityType<BurstSpawnerBlockEntity> BURST_SPAWNER_ENTITY;
    public static BlockEntityType<RelaySpawnerBlockEntity> RELAY_SPAWNER_ENTITY;
    public static BlockEntityType<CrawEggBlockEntity> CRAW_EGG_ENTITY;

    private OrpimentBlocks() {
    }

    public static void register() {
        registerTar();
        registerTarredSandstone();
        registerAmber();
        registerStone();
        BURST_SPAWNER = register(
            "burst_spawner",
            properties("burst_spawner").mapColor(MapColor.STONE).strength(4.0f, 50.0f).requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.IMMOVEABLE),
            BurstSpawnerBlock::new
        );
        RELAY_SPAWNER = register(
            "relay_spawner",
            properties("relay_spawner").mapColor(MapColor.STONE).strength(50.0f, 50.0f).requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.IMMOVEABLE),
            RelaySpawnerBlock::new
        );
        CRAW_EGG = register(
            "craw_egg",
            properties("craw_egg").mapColor(MapColor.COLOR_YELLOW).sound(SoundType.METAL).strength(0.5f).noOcclusion(),
            CrawEggBlock::new
        );
        BURST_SPAWNER_ENTITY = blockEntity("burst_spawner", BurstSpawnerBlockEntity::new, BURST_SPAWNER);
        RELAY_SPAWNER_ENTITY = blockEntity("relay_spawner", RelaySpawnerBlockEntity::new, RELAY_SPAWNER);
        CRAW_EGG_ENTITY = blockEntity("craw_egg", CrawEggBlockEntity::new, CRAW_EGG);
    }

    private static void registerTar() {
        TAR = register(
            "tar",
            properties("tar").mapColor(MapColor.COLOR_GRAY).strength(100.0f).noCollision().noLootTable().liquid().pushReaction(PushReaction.POPPED).replaceable(),
            props -> new TarBlock(OrpimentFluids.TAR, props)
        );
        TAR_CAULDRON = register(
            "tar_cauldron",
            properties("tar_cauldron").strength(2.0f, 2.0f).requiresCorrectToolForDrops().noOcclusion(),
            TarCauldronBlock::new
        );
    }

    private static void registerTarredSandstone() {
        TARRED_SAND = register(
            "tarred_sand",
            properties("tarred_sand").mapColor(MapColor.COLOR_BROWN).sound(OrpimentSounds.TARRED_SAND).strength(0.5f).instrument(NoteBlockInstrument.SNARE),
            props -> new ColoredFallingBlock(new ColorRGBA(0xFF3B2F25), props)
        );
        TARRED_SANDSTONE = sandstone("tarred_sandstone", 0.8f, 0.8f, Block::new);
        TARRED_SANDSTONE_SLAB = sandstone("tarred_sandstone_slab", 2.0f, 6.0f, SlabBlock::new);
        TARRED_SANDSTONE_STAIRS = sandstone("tarred_sandstone_stairs", 0.8f, 0.8f, props -> new StairBlock(TARRED_SANDSTONE.defaultBlockState(), props));
        TARRED_SANDSTONE_WALL = sandstone("tarred_sandstone_wall", 0.8f, 0.8f, WallBlock::new);
        CUT_TARRED_SANDSTONE = sandstone("cut_tarred_sandstone", 0.8f, 0.8f, Block::new);
        CUT_TARRED_SANDSTONE_SLAB = sandstone("cut_tarred_sandstone_slab", 2.0f, 6.0f, SlabBlock::new);
        SMOOTH_TARRED_SANDSTONE = sandstone("smooth_tarred_sandstone", 2.0f, 6.0f, Block::new);
        SMOOTH_TARRED_SANDSTONE_SLAB = sandstone("smooth_tarred_sandstone_slab", 2.0f, 6.0f, SlabBlock::new);
        SMOOTH_TARRED_SANDSTONE_STAIRS = sandstone(
            "smooth_tarred_sandstone_stairs",
            2.0f,
            6.0f,
            props -> new StairBlock(SMOOTH_TARRED_SANDSTONE.defaultBlockState(), props)
        );
        CHISELED_TARRED_SANDSTONE = sandstone("chiseled_tarred_sandstone", 0.8f, 0.8f, Block::new);
    }

    private static void registerAmber() {
        AMBER = amber("orpiment_amber", true, AmberBlock::new);
        AMBER_BRICKS = amber("orpiment_amber_bricks", false, Block::new);
        AMBER_BRICK_SLAB = amber("orpiment_amber_brick_slab", false, SlabBlock::new);
        AMBER_BRICK_STAIRS = amber("orpiment_amber_brick_stairs", false, props -> new StairBlock(AMBER_BRICKS.defaultBlockState(), props));
        AMBER_BRICK_WALL = amber("orpiment_amber_brick_wall", false, WallBlock::new);
        AMBER_PILLAR = amber("orpiment_amber_pillar", false, RotatedPillarBlock::new);
        CHISELED_AMBER = amber("orpiment_chiseled_amber", false, Block::new);
        AMBER_SLAB = amber("orpiment_amber_slab", true, SlabBlock::new);
        AMBER_STAIRS = amber("orpiment_amber_stairs", true, props -> new StairBlock(AMBER.defaultBlockState(), props));
        AMBER_WALL = amber("orpiment_amber_wall", true, WallBlock::new);
        HOLLOWED_AMBER_BRICKS = amber("orpiment_hollowed_amber_bricks", false, Block::new);
        AMBER_LAMP = register(
            "orpiment_amber_lamp",
            amberProperties("orpiment_amber_lamp", false).lightLevel(AmberLampBlock::light),
            AmberLampBlock::new
        );
        AMBER_DRUSE = register(
            "orpiment_amber_druse",
            amberProperties("orpiment_amber_druse", true).lightLevel(state -> state.getValue(AmberDruseBlock.AMOUNT) * 3),
            AmberDruseBlock::new
        );
    }

    private static void registerStone() {
        LIGNITE = register(
            "orpiment_lignite",
            properties("orpiment_lignite").mapColor(MapColor.DIRT).sound(OrpimentSounds.LIGNITE).strength(1.0f).ignitedByLava(),
            Block::new
        );
        PETRIFIED_LOG = register(
            "orpiment_petrified_log",
            properties("orpiment_petrified_log").mapColor(MapColor.STONE).strength(2.5f, 6.0f).requiresCorrectToolForDrops(),
            RotatedPillarBlock::new
        );
        PETRIFIED_ROOTS = register(
            "orpiment_petrified_roots",
            properties("orpiment_petrified_roots")
                .mapColor(MapColor.PLANT)
                .sound(SoundType.HANGING_ROOTS)
                .instabreak()
                .noCollision()
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.POPPED),
            PetrifiedRootsBlock::new
        );
        WRAITH_ALTAR = register(
            "wraith_altar",
            properties("wraith_altar").mapColor(MapColor.COLOR_GRAY).sound(SoundType.DEEPSLATE_BRICKS).strength(20.0f, 5.0f).requiresCorrectToolForDrops().noOcclusion(),
            WraithAltarBlock::new
        );
        ECTOPLASM_SCAFFOLD = register(
            "ectoplasm_scaffold",
            properties("ectoplasm_scaffold").mapColor(MapColor.WOOL).sound(OrpimentSounds.ECTOPLASM_SCAFFOLD).instabreak().noOcclusion(),
            EctoplasmScaffoldBlock::new
        );
    }

    private static Block sandstone(String name, float hardness, float resistance, Function<BlockBehaviour.Properties, Block> factory) {
        return register(
            name,
            properties(name).mapColor(MapColor.COLOR_BROWN).sound(OrpimentSounds.TARRED_SANDSTONE).strength(hardness, resistance).requiresCorrectToolForDrops(),
            factory
        );
    }

    private static Block amber(String name, boolean clear, Function<BlockBehaviour.Properties, Block> factory) {
        return register(name, amberProperties(name, clear), factory);
    }

    private static BlockBehaviour.Properties amberProperties(String name, boolean clear) {
        BlockBehaviour.Properties properties = properties(name)
            .mapColor(MapColor.COLOR_ORANGE)
            .sound(OrpimentSounds.AMBER)
            .strength(3.0f, 4.0f)
            .requiresCorrectToolForDrops();
        return clear ? properties.noOcclusion().isRedstoneConductor((state, level, pos) -> false) : properties;
    }

    private static BlockBehaviour.Properties properties(String name) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), MmsDungeons.id(name)));
    }

    private static Block register(String name, BlockBehaviour.Properties properties, Function<BlockBehaviour.Properties, Block> factory) {
        return Registry.register(BuiltInRegistries.BLOCK, MmsDungeons.id(name), factory.apply(properties));
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> blockEntity(
        String name,
        BlockEntityType.BlockEntitySupplier<T> factory,
        Block block
    ) {
        Identifier id = MmsDungeons.id(name);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, new BlockEntityType<>(factory, Set.of(block)));
    }
}
