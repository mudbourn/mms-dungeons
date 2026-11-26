package info.mudbourn.mmsdungeons.mobs.knightquest;

import info.mudbourn.mmsdungeons.MmsDungeons;
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
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

public final class KnightQuestItems {

    public static Item SMALL_ESSENCE;
    public static Item GREAT_ESSENCE;
    public static Item RADIANT_ESSENCE;
    public static Item CHAOTIC_ESSENCE;
    public static Item EMPTY_GOBLET;
    public static Item FILLED_GOBLET;
    public static Item MUSIC_DISC_THE_ARCHITECT_OF_CHAOS;
    public static Item LIZZY_SCALE;
    public static Item RATMAN_EYE;

    private static final List<Item> TAB_ITEMS = new ArrayList<>();

    private KnightQuestItems() {
    }

    public static void register() {
        SMALL_ESSENCE = register("small_essence", properties("small_essence"), props -> new LoreItem(props, "small_essence"));
        GREAT_ESSENCE = register("great_essence", properties("great_essence"), props -> new LoreItem(props, "great_essence"));
        RADIANT_ESSENCE = register(
            "radiant_essence",
            properties("radiant_essence").rarity(Rarity.RARE),
            props -> new LoreItem(props, "radiant_essence")
        );
        CHAOTIC_ESSENCE = register(
            "chaotic_essence",
            properties("chaotic_essence").rarity(Rarity.EPIC),
            props -> new LoreItem(props, "chaotic_essence")
        );
        EMPTY_GOBLET = register("empty_goblet", properties("empty_goblet").stacksTo(16), props -> new LoreItem(props, "empty_goblet"));
        FILLED_GOBLET = register("filled_goblet", properties("filled_goblet").stacksTo(16), props -> new LoreItem(props, "filled_goblet"));
        MUSIC_DISC_THE_ARCHITECT_OF_CHAOS = register(
            "music_disc_the_architect_of_chaos",
            properties("music_disc_the_architect_of_chaos")
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, MmsDungeons.id("the_architect_of_chaos"))),
            Item::new
        );
        LIZZY_SCALE = register("lizzy_scale", properties("lizzy_scale"), props -> new LoreItem(props, "lizzy_scale"));
        RATMAN_EYE = register("ratman_eye", properties("ratman_eye"), props -> new LoreItem(props, "ratman_eye"));
        blockItem(KnightQuestBlocks.GREAT_CHALICE);
        registerSpawnEggs();
        registerTab();
    }

    private static void registerSpawnEggs() {
        spawnEgg("gremlin_spawn_egg", KnightQuestMobs.GREMLIN);
        spawnEgg("bad_patch_spawn_egg", KnightQuestMobs.BAD_PATCH);
        spawnEgg("eld_knight_spawn_egg", KnightQuestMobs.ELD_KNIGHT);
        spawnEgg("eld_bomb_spawn_egg", KnightQuestMobs.ELD_BOMB);
        spawnEgg("lizzy_spawn_egg", KnightQuestMobs.LIZZY);
        spawnEgg("momma_lizzy_spawn_egg", KnightQuestMobs.MOMMA_LIZZY);
        spawnEgg("samhain_spawn_egg", KnightQuestMobs.SAMHAIN);
        spawnEgg("swampman_spawn_egg", KnightQuestMobs.SWAMPMAN);
        spawnEgg("ratman_spawn_egg", KnightQuestMobs.RATMAN);
        spawnEgg("ghosty_spawn_egg", KnightQuestMobs.GHOSTY);
        spawnEgg("fallen_knight_spawn_egg", KnightQuestMobs.FALLEN_KNIGHT);
        spawnEgg("netherman_spawn_egg", KnightQuestMobs.NETHERMAN);
    }

    private static void blockItem(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        register(name, properties(name).useBlockDescriptionPrefix(), props -> new BlockItem(block, props));
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
            MmsDungeons.id("knightquest"),
            FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mms_dungeons.knightquest"))
                .icon(() -> new ItemStack(SMALL_ESSENCE))
                .displayItems((parameters, output) -> TAB_ITEMS.forEach(output::accept))
                .build()
        );
    }
}
