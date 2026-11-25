package info.mudbourn.mmsdungeons.mobs.orpiment;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.EctoplasmScaffoldItem;
import info.mudbourn.mmsdungeons.mobs.orpiment.item.AmberCageItem;
import info.mudbourn.mmsdungeons.mobs.orpiment.item.BucketOfSapItem;
import info.mudbourn.mmsdungeons.mobs.orpiment.item.TarBucketItem;
import info.mudbourn.mmsdungeons.mobs.orpiment.item.TarClumpItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

public final class OrpimentItems {

    public static Item TAR_CLUMP;
    public static Item TAR_BUCKET;
    public static Item SAP_BALL;
    public static Item AMBER_CHUNK;
    public static Item STUCK_POTTERY_SHERD;
    public static Item MUSIC_DISC_FOSSIL;
    public static Item WISP_SKULL;
    public static Item SOUL_SEAL;
    public static Item WRAITH_ECTOPLASM;
    public static Item AMBER_CAGE;
    public static Item BUCKET_OF_SAP;
    public static Item SHINE_SMITHING_TEMPLATE;
    public static Item BIRD_FEED;

    private static final List<Item> TAB_ITEMS = new ArrayList<>();

    private OrpimentItems() {
    }

    public static void register() {
        registerSpawnEggs();
        TAR_CLUMP = register("tar_clump", properties("tar_clump").stacksTo(16), TarClumpItem::new);
        TAR_BUCKET = register(
            "tar_bucket",
            properties("tar_bucket").craftRemainder(Items.BUCKET).stacksTo(1),
            props -> new TarBucketItem(OrpimentFluids.TAR, props)
        );
        SAP_BALL = register("sap_ball", properties("sap_ball"), Item::new);
        AMBER_CHUNK = register("orpiment_amber_chunk", properties("orpiment_amber_chunk"), Item::new);
        STUCK_POTTERY_SHERD = register(
            "stuck_pottery_sherd",
            properties("stuck_pottery_sherd").potPattern(ResourceKey.create(Registries.DECORATED_POT_PATTERN, MmsDungeons.id("stuck"))),
            Item::new
        );
        MUSIC_DISC_FOSSIL = register(
            "music_disc_fossil",
            properties("music_disc_fossil")
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, MmsDungeons.id("fossil"))),
            Item::new
        );
        WISP_SKULL = register("wisp_skull", properties("wisp_skull").rarity(Rarity.UNCOMMON), Item::new);
        SOUL_SEAL = register("soul_seal", properties("soul_seal").rarity(Rarity.UNCOMMON), Item::new);
        WRAITH_ECTOPLASM = register("wraith_ectoplasm", properties("wraith_ectoplasm").rarity(Rarity.UNCOMMON), Item::new);
        AMBER_CAGE = register("amber_cage", properties("amber_cage").rarity(Rarity.UNCOMMON).durability(35), AmberCageItem::new);
        BUCKET_OF_SAP = register("bucket_of_sap", properties("bucket_of_sap").stacksTo(1), BucketOfSapItem::new);
        SHINE_SMITHING_TEMPLATE = register(
            "shine_smithing_template",
            properties("shine_smithing_template").rarity(Rarity.UNCOMMON),
            SmithingTemplateItem::createArmorTrimTemplate
        );
        BIRD_FEED = register("bird_feed", properties("bird_feed").stacksTo(16), Item::new);
        registerBlockItems();
        registerTab();
    }

    private static void registerSpawnEggs() {
        spawnEgg("tar_cube_spawn_egg", OrpimentMobs.TAR_CUBE);
        spawnEgg("sap_spawn_egg", OrpimentMobs.SAP);
        spawnEgg("wraith_spawn_egg", OrpimentMobs.WRAITH);
        spawnEgg("wisp_spawn_egg", OrpimentMobs.WISP);
        spawnEgg("craw_spawn_egg", OrpimentMobs.CRAW);
    }

    private static void registerBlockItems() {
        for (Block block : new Block[] {
            OrpimentBlocks.TARRED_SAND,
            OrpimentBlocks.TARRED_SANDSTONE,
            OrpimentBlocks.TARRED_SANDSTONE_SLAB,
            OrpimentBlocks.TARRED_SANDSTONE_STAIRS,
            OrpimentBlocks.TARRED_SANDSTONE_WALL,
            OrpimentBlocks.CUT_TARRED_SANDSTONE,
            OrpimentBlocks.CUT_TARRED_SANDSTONE_SLAB,
            OrpimentBlocks.SMOOTH_TARRED_SANDSTONE,
            OrpimentBlocks.SMOOTH_TARRED_SANDSTONE_SLAB,
            OrpimentBlocks.SMOOTH_TARRED_SANDSTONE_STAIRS,
            OrpimentBlocks.CHISELED_TARRED_SANDSTONE,
            OrpimentBlocks.LIGNITE,
            OrpimentBlocks.PETRIFIED_LOG,
            OrpimentBlocks.PETRIFIED_ROOTS,
            OrpimentBlocks.AMBER,
            OrpimentBlocks.AMBER_BRICKS,
            OrpimentBlocks.AMBER_BRICK_SLAB,
            OrpimentBlocks.AMBER_BRICK_STAIRS,
            OrpimentBlocks.AMBER_BRICK_WALL,
            OrpimentBlocks.AMBER_PILLAR,
            OrpimentBlocks.CHISELED_AMBER,
            OrpimentBlocks.AMBER_SLAB,
            OrpimentBlocks.AMBER_STAIRS,
            OrpimentBlocks.AMBER_WALL,
            OrpimentBlocks.AMBER_LAMP,
            OrpimentBlocks.AMBER_DRUSE,
            OrpimentBlocks.HOLLOWED_AMBER_BRICKS,
            OrpimentBlocks.WRAITH_ALTAR,
            OrpimentBlocks.BURST_SPAWNER,
            OrpimentBlocks.RELAY_SPAWNER,
            OrpimentBlocks.CRAW_EGG
        }) {
            blockItem(block, BlockItem::new);
        }
        blockItem(OrpimentBlocks.ECTOPLASM_SCAFFOLD, EctoplasmScaffoldItem::new);
    }

    private static void blockItem(Block block, java.util.function.BiFunction<Block, Item.Properties, Item> factory) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        register(name, properties(name).useBlockDescriptionPrefix(), props -> factory.apply(block, props));
    }

    private static void spawnEgg(String name, EntityType<?> type) {
        register(name, properties(name).spawnEgg(type), SpawnEggItem::new);
    }

    private static Item.Properties properties(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MmsDungeons.id(name)));
    }

    private static Item register(String name, Item.Properties properties, Function<Item.Properties, Item> factory) {
        Identifier id = MmsDungeons.id(name);
        Item item = Registry.register(BuiltInRegistries.ITEM, id, factory.apply(properties));
        TAB_ITEMS.add(item);
        return item;
    }

    private static void registerTab() {
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            MmsDungeons.id("orpiment"),
            FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mms_dungeons.orpiment"))
                .icon(() -> new ItemStack(AMBER_CHUNK))
                .displayItems((parameters, output) -> TAB_ITEMS.forEach(output::accept))
                .build()
        );
    }
}
