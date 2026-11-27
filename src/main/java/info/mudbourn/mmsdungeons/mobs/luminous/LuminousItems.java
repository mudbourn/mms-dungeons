package info.mudbourn.mmsdungeons.mobs.luminous;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

public final class LuminousItems {

    private static final List<Item> EGGS = new ArrayList<>();

    private LuminousItems() {
    }

    public static void register() {
        spawnEgg("miner_zombie_spawn_egg", LuminousMobs.MINER_ZOMBIE);
        spawnEgg("icy_zombie_spawn_egg", LuminousMobs.ICY_ZOMBIE);
        spawnEgg("swamp_zombie_spawn_egg", LuminousMobs.SWAMP_ZOMBIE);
        spawnEgg("dark_oak_zombie_spawn_egg", LuminousMobs.DARK_OAK_ZOMBIE);
        spawnEgg("cherry_zombie_spawn_egg", LuminousMobs.CHERRY_ZOMBIE);
        spawnEgg("savanna_zombie_spawn_egg", LuminousMobs.SAVANNA_ZOMBIE);
        spawnEgg("crimson_walker_spawn_egg", LuminousMobs.CRIMSON_WALKER);
        spawnEgg("miner_skeleton_spawn_egg", LuminousMobs.MINER_SKELETON);
        spawnEgg("icy_skeleton_spawn_egg", LuminousMobs.ICY_SKELETON);
        spawnEgg("swamp_skeleton_spawn_egg", LuminousMobs.SWAMP_SKELETON);
        spawnEgg("dark_oak_skeleton_spawn_egg", LuminousMobs.DARK_OAK_SKELETON);
        spawnEgg("hollow_skeleton_spawn_egg", LuminousMobs.HOLLOW_SKELETON);
        spawnEgg("cherry_skeleton_spawn_egg", LuminousMobs.CHERRY_SKELETON);
        spawnEgg("savanna_skeleton_spawn_egg", LuminousMobs.SAVANNA_SKELETON);
        spawnEgg("sunken_skeleton_spawn_egg", LuminousMobs.SUNKEN_SKELETON);
        spawnEgg("revenant_skeleton_spawn_egg", LuminousMobs.REVENANT_SKELETON);
        registerTab();
    }

    private static void spawnEgg(String name, EntityType<?> type) {
        Identifier id = MmsDungeons.id(name);
        Item.Properties properties = new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, id))
            .spawnEgg(type);
        EGGS.add(Registry.register(BuiltInRegistries.ITEM, id, new SpawnEggItem(properties)));
    }

    private static void registerTab() {
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            MmsDungeons.id("luminous"),
            FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mms_dungeons.luminous"))
                .icon(() -> new ItemStack(EGGS.getFirst()))
                .displayItems((parameters, output) -> EGGS.forEach(output::accept))
                .build()
        );
    }
}
