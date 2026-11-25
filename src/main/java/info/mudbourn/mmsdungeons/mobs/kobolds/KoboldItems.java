package info.mudbourn.mmsdungeons.mobs.kobolds;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.ToolMaterial;

public final class KoboldItems {

    public static final ToolMaterial KOBOLD_IRON = new ToolMaterial(
        BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
        1028,
        5.0f,
        2.0f,
        12,
        ItemTags.IRON_TOOL_MATERIALS
    );

    public static Item KOBOLD_SPAWN_EGG;
    public static Item KOBOLD_WARRIOR_SPAWN_EGG;
    public static Item KOBOLD_ENCHANTER_SPAWN_EGG;
    public static Item KOBOLD_RASCAL_SPAWN_EGG;
    public static Item KOBOLD_ENGINEER_SPAWN_EGG;
    public static Item KOBOLD_PIRATE_SPAWN_EGG;
    public static Item KOBOLD_CAPTAIN_SPAWN_EGG;
    public static Item KOBOLD_ZOMBIE_SPAWN_EGG;
    public static Item KOBOLD_SKELETON_SPAWN_EGG;
    public static Item WITHERBOLD_SPAWN_EGG;
    public static Item KOBOLD_SKULL;
    public static Item KOBOLD_WITHER_SKULL;
    public static Item KOBOLD_IRON_SWORD;
    public static Item KOBOLD_IRON_SHOVEL;
    public static Item KOBOLD_IRON_PICKAXE;
    public static Item KOBOLD_IRON_AXE;
    public static Item KOBOLD_IRON_HOE;
    public static Item BANNER_PATTERN_KOBOLD;
    public static Item MUSIC_DISC_KOBBLESTONE;
    public static Item KOBOLD_TEMPLATE;

    private static final List<Item> TAB_ITEMS = new ArrayList<>();

    private KoboldItems() {
    }

    public static void register() {
        KOBOLD_SPAWN_EGG = spawnEgg("kobold_spawn_egg", KoboldMobs.KOBOLD);
        KOBOLD_WARRIOR_SPAWN_EGG = spawnEgg("kobold_warrior_spawn_egg", KoboldMobs.KOBOLD_WARRIOR);
        KOBOLD_ENCHANTER_SPAWN_EGG = spawnEgg("kobold_enchanter_spawn_egg", KoboldMobs.KOBOLD_ENCHANTER);
        KOBOLD_RASCAL_SPAWN_EGG = spawnEgg("kobold_rascal_spawn_egg", KoboldMobs.KOBOLD_RASCAL);
        KOBOLD_ENGINEER_SPAWN_EGG = spawnEgg("kobold_engineer_spawn_egg", KoboldMobs.KOBOLD_ENGINEER);
        KOBOLD_PIRATE_SPAWN_EGG = spawnEgg("kobold_pirate_spawn_egg", KoboldMobs.KOBOLD_PIRATE);
        KOBOLD_CAPTAIN_SPAWN_EGG = spawnEgg("kobold_captain_spawn_egg", KoboldMobs.KOBOLD_CAPTAIN);
        KOBOLD_ZOMBIE_SPAWN_EGG = spawnEgg("kobold_zombie_spawn_egg", KoboldMobs.KOBOLD_ZOMBIE);
        KOBOLD_SKELETON_SPAWN_EGG = spawnEgg("kobold_skeleton_spawn_egg", KoboldMobs.KOBOLD_SKELETON);
        WITHERBOLD_SPAWN_EGG = spawnEgg("witherbold_spawn_egg", KoboldMobs.WITHERBOLD);
        KOBOLD_TEMPLATE = register(
            "kobold_template",
            properties("kobold_template").rarity(Rarity.UNCOMMON),
            SmithingTemplateItem::createArmorTrimTemplate
        );
        KOBOLD_IRON_SWORD = register("kobold_iron_sword", properties("kobold_iron_sword").sword(KOBOLD_IRON, 3.0f, -2.2f), Item::new);
        KOBOLD_IRON_SHOVEL = register("kobold_iron_shovel", properties("kobold_iron_shovel").shovel(KOBOLD_IRON, 1.5f, -3.0f), Item::new);
        KOBOLD_IRON_PICKAXE = register(
            "kobold_iron_pickaxe",
            properties("kobold_iron_pickaxe").pickaxe(KOBOLD_IRON, 1.0f, -2.8f),
            Item::new
        );
        KOBOLD_IRON_AXE = register("kobold_iron_axe", properties("kobold_iron_axe").axe(KOBOLD_IRON, 5.0f, -3.0f), Item::new);
        KOBOLD_IRON_HOE = register("kobold_iron_hoe", properties("kobold_iron_hoe").hoe(KOBOLD_IRON, -2.0f, -1.0f), Item::new);
        BANNER_PATTERN_KOBOLD = register(
            "banner_pattern_kobold",
            properties("banner_pattern_kobold")
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
                .delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(KoboldTags.BANNER_PATTERN)),
            Item::new
        );
        MUSIC_DISC_KOBBLESTONE = register(
            "music_disc_kobblestone",
            properties("music_disc_kobblestone")
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, MmsDungeons.id("kobblestone"))),
            Item::new
        );
        KOBOLD_SKULL = register(
            "kobold_skull",
            properties("kobold_skull").rarity(Rarity.UNCOMMON),
            props -> new StandingAndWallBlockItem(
                KoboldBlocks.KOBOLD_SKULL,
                KoboldBlocks.KOBOLD_SKULL_WALL,
                Direction.DOWN,
                props
            )
        );
        KOBOLD_WITHER_SKULL = register(
            "kobold_wither_skull",
            properties("kobold_wither_skull").rarity(Rarity.UNCOMMON),
            props -> new StandingAndWallBlockItem(
                KoboldBlocks.KOBOLD_WITHER_SKULL,
                KoboldBlocks.KOBOLD_WITHER_SKULL_WALL,
                Direction.DOWN,
                props
            )
        );
        registerTab();
    }

    private static Item spawnEgg(String name, EntityType<?> type) {
        return register(name, properties(name).spawnEgg(type), SpawnEggItem::new);
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
            MmsDungeons.id("kobolds"),
            FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mms_dungeons.kobolds"))
                .icon(() -> new ItemStack(KOBOLD_SKULL))
                .displayItems((parameters, output) -> TAB_ITEMS.forEach(output::accept))
                .build()
        );
    }
}
