package info.mudbourn.mmsdungeons.dungeon;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

// Bountiful Fish crates reeled in with ordinary catches anywhere in the dungeon.
public final class DungeonFishing {

    private static final String BOUNTIFUL_FISH = "bountiful-fish";
    private static final float CRATE_CHANCE = 0.05f;
    private static final int WOODEN_WEIGHT = 8;
    private static final int METAL_WEIGHT = 5;
    private static final int DIAMOND_WEIGHT = 2;

    private DungeonFishing() {
    }

    public static void register() {
        if (!FabricLoader.getInstance().isModLoaded(BOUNTIFUL_FISH)) {
            return;
        }
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            if (key.equals(BuiltInLootTables.FISHING_FISH)) {
                table.withPool(cratePool());
            }
        });
    }

    private static LootPool.Builder cratePool() {
        return LootPool.lootPool()
            .when(LocationCheck.checkLocation(LocationPredicate.Builder.inDimension(DungeonDimension.LEVEL)))
            .when(LootItemRandomChanceCondition.randomChance(CRATE_CHANCE))
            .add(LootItem.lootTableItem(crate("wooden_crate")).setWeight(WOODEN_WEIGHT))
            .add(LootItem.lootTableItem(crate("metal_crate")).setWeight(METAL_WEIGHT))
            .add(LootItem.lootTableItem(crate("diamond_crate")).setWeight(DIAMOND_WEIGHT));
    }

    private static Item crate(String name) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(BOUNTIFUL_FISH, name));
    }
}
