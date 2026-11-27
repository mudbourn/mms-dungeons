package info.mudbourn.mmsdungeons.mobs.fangsnclaws;

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
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

// The Fangs 'n Claws items and their creative tab.
public final class FangsNClawsItems {

    public static Item POISONOUS_DART;
    public static Item VILE_FAT;
    public static Item BLACK_HORN;
    public static Item SPECTRAL_ESSENCE;
    public static Item EVIL_EYE;
    public static Item FUR;
    public static Item HEAVY_CLAW;
    public static Item LONG_FANG;
    public static Item CHITIN;
    public static Item GIANT_FEATHER;
    public static Item SNOW_DUVET;
    public static Item OWLBEAR_MEAT;
    public static Item COOKED_OWLBEAR_MEAT;
    public static Item SCORPION_STING;
    public static Item VELOCITY_ARROW;
    public static Item TOTEM_OF_FROST;
    public static Item HORSE_BLANKET;
    public static Item STURDY_SADDLE;
    public static Item SHRIKE_UPGRADE_SMITHING_TEMPLATE;

    private static final List<Item> TAB_ITEMS = new ArrayList<>();

    private FangsNClawsItems() {
    }

    public static void register() {
        POISONOUS_DART = plain("poisonous_dart");
        VILE_FAT = plain("vile_fat");
        BLACK_HORN = plain("black_horn");
        SPECTRAL_ESSENCE = plain("spectral_essence");
        EVIL_EYE = plain("evil_eye");
        FUR = plain("fur");
        HEAVY_CLAW = plain("heavy_claw");
        LONG_FANG = plain("long_fang");
        CHITIN = plain("chitin");
        GIANT_FEATHER = plain("giant_feather");
        SNOW_DUVET = plain("snow_duvet");
        OWLBEAR_MEAT = food("owlbear_meat", 4, 0.4f);
        COOKED_OWLBEAR_MEAT = food("cooked_owlbear_meat", 10, 1.0f);
        SCORPION_STING = plain("scorpion_sting");
        VELOCITY_ARROW = register("velocity_arrow", properties("velocity_arrow"), VelocityArrowItem::new);
        TOTEM_OF_FROST = single("totem_of_frost");
        HORSE_BLANKET = single("horse_blanket");
        STURDY_SADDLE = single("sturdy_saddle");
        SHRIKE_UPGRADE_SMITHING_TEMPLATE = plain("shrike_upgrade_smithing_template");
        FangsNClawsGear.register();
        blockItem(FangsNClawsBlocks.GHOST_BLOCK);
        blockItem(FangsNClawsBlocks.BEAR_TRAP);
        blockItem(FangsNClawsBlocks.VILE_LANTERN);
        registerSpawnEggs();
        registerTab();
    }

    private static void registerSpawnEggs() {
        spawnEgg("goblin_spawn_egg", FangsNClawsMobs.GOBLIN);
        spawnEgg("dart_goblin_spawn_egg", FangsNClawsMobs.DART_GOBLIN);
        spawnEgg("ogre_spawn_egg", FangsNClawsMobs.OGRE);
        spawnEgg("cave_ogre_spawn_egg", FangsNClawsMobs.CAVE_OGRE);
        spawnEgg("hell_ogre_spawn_egg", FangsNClawsMobs.HELL_OGRE);
        spawnEgg("imp_spawn_egg", FangsNClawsMobs.IMP);
        spawnEgg("ghost_spawn_egg", FangsNClawsMobs.GHOST);
        spawnEgg("fire_ghost_spawn_egg", FangsNClawsMobs.FIRE_GHOST);
        spawnEgg("evil_bat_spawn_egg", FangsNClawsMobs.EVIL_BAT);
        spawnEgg("mimic_spawn_egg", FangsNClawsMobs.MIMIC);
        spawnEgg("silver_skeleton_spawn_egg", FangsNClawsMobs.SILVER_SKELETON);
        spawnEgg("hyena_spawn_egg", FangsNClawsMobs.HYENA);
        spawnEgg("baby_hyena_spawn_egg", FangsNClawsMobs.BABY_HYENA);
        spawnEgg("wild_wolf_spawn_egg", FangsNClawsMobs.WILD_WOLF);
        spawnEgg("baby_wild_wolf_spawn_egg", FangsNClawsMobs.BABY_WILD_WOLF);
        spawnEgg("owlbear_spawn_egg", FangsNClawsMobs.OWLBEAR);
        spawnEgg("baby_owlbear_spawn_egg", FangsNClawsMobs.BABY_OWLBEAR);
        spawnEgg("shrike_spawn_egg", FangsNClawsMobs.SHRIKE);
        spawnEgg("baby_shrike_spawn_egg", FangsNClawsMobs.BABY_SHRIKE);
        spawnEgg("scorpion_spawn_egg", FangsNClawsMobs.SCORPION);
        spawnEgg("desert_scorpion_spawn_egg", FangsNClawsMobs.DESERT_SCORPION);
        spawnEgg("frost_scorpion_spawn_egg", FangsNClawsMobs.FROST_SCORPION);
        spawnEgg("nether_scorpion_spawn_egg", FangsNClawsMobs.NETHER_SCORPION);
        spawnEgg("baby_scorpion_spawn_egg", FangsNClawsMobs.BABY_SCORPION);
        spawnEgg("werewolf_spawn_egg", FangsNClawsMobs.WEREWOLF);
        spawnEgg("zombie_horse_spawn_egg", FangsNClawsMobs.ZOMBIE_HORSE);
        spawnEgg("skeleton_horse_spawn_egg", FangsNClawsMobs.SKELETON_HORSE);
        spawnEgg("nightmare_horse_spawn_egg", FangsNClawsMobs.NIGHTMARE_HORSE);
        spawnEgg("horse_bat_spawn_egg", FangsNClawsMobs.HORSE_BAT);
        spawnEgg("golem_spawn_egg", FangsNClawsMobs.GOLEM);
        spawnEgg("ice_golem_spawn_egg", FangsNClawsMobs.ICE_GOLEM);
        spawnEgg("purple_worm_spawn_egg", FangsNClawsMobs.PURPLE_WORM);
    }

    private static Item plain(String name) {
        return register(name, properties(name), Item::new);
    }

    private static void blockItem(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        register(name, properties(name).useBlockDescriptionPrefix(), props -> new BlockItem(block, props));
    }

    private static Item single(String name) {
        return register(name, properties(name).stacksTo(1), Item::new);
    }

    private static Item food(String name, int nutrition, float saturation) {
        FoodProperties food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
        return register(name, properties(name).food(food), Item::new);
    }

    private static void spawnEgg(String name, EntityType<?> type) {
        register(name, properties(name).spawnEgg(type), SpawnEggItem::new);
    }

    static Item.Properties properties(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MmsDungeons.id(name)));
    }

    static Item register(String name, Item.Properties properties, Function<Item.Properties, Item> factory) {
        Identifier id = MmsDungeons.id(name);
        Item item = Registry.register(BuiltInRegistries.ITEM, id, factory.apply(properties));
        TAB_ITEMS.add(item);
        return item;
    }

    private static void registerTab() {
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            MmsDungeons.id("fangsnclaws"),
            FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mms_dungeons.fangsnclaws"))
                .icon(() -> new ItemStack(SPECTRAL_ESSENCE))
                .displayItems((parameters, output) -> TAB_ITEMS.forEach(output::accept))
                .build()
        );
    }
}
