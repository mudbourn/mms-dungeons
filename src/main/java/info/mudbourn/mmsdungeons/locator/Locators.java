package info.mudbourn.mmsdungeons.locator;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

// Registers the locator item and lists what it can find.
public final class Locators {

    public static final List<LocatorTarget> TARGETS = List.of(
        LocatorTarget.presetStructure("labyrinth"),
        LocatorTarget.presetStructure("kobold_den"),
        LocatorTarget.presetStructure("orpiment_catacombs"),
        LocatorTarget.presetStructure("orpiment_arid_sanctum"),
        LocatorTarget.presetStructure("abandoned_portal"),
        LocatorTarget.presetBiome("magnetic_caves"),
        LocatorTarget.presetBiome("primordial_caves"),
        LocatorTarget.presetBiome("toxic_caves"),
        LocatorTarget.presetBiome("abyssal_chasm"),
        LocatorTarget.presetBiome("forlorn_hollows"),
        LocatorTarget.presetBiome("candy_cavity"));

    public static Item LOCATOR;

    public static LocatorTarget preset(Identifier id) {
        for (LocatorTarget target : TARGETS) {
            if (target.id().equals(id)) {
                return target;
            }
        }
        return null;
    }

    private Locators() {
    }

    public static void register() {
        Identifier id = MmsDungeons.id("locator");
        ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        LOCATOR = Registry.register(
            BuiltInRegistries.ITEM,
            id,
            new LocatorItem(
                new Item.Properties()
                    .stacksTo(1)
                    .durability(64)
                    .repairable(Items.COPPER_INGOT)
                    .setId(key)));
        LocatorSounds.register();
        LocatorSessions.register();
        LocatorRegions.register();
        LocatorNetworking.register();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(output -> output.accept(LOCATOR));
    }
}
