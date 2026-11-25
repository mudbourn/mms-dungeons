package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

// Registers the dungeon portal block and the item admins place it with.
public final class DungeonBlocks {

    public static Block DUNGEON_PORTAL;
    public static BlockEntityType<DungeonPortalBlockEntity> DUNGEON_PORTAL_ENTITY;

    private DungeonBlocks() {
    }

    public static void register() {
        Identifier id = MmsDungeons.id("dungeon_portal");
        ResourceKey<Block> blockKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), id);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
            .setId(blockKey)
            .noCollision()
            .noOcclusion()
            .strength(-1.0f, 3600000.0f)
            .lightLevel(state -> 11)
            .sound(SoundType.GLASS)
            .noLootTable()
            .pushReaction(PushReaction.IMMOVEABLE);
        DUNGEON_PORTAL = Registry.register(BuiltInRegistries.BLOCK, id, new DungeonPortalBlock(properties));
        DUNGEON_PORTAL_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id,
            new BlockEntityType<>(DungeonPortalBlockEntity::new, Set.of(DUNGEON_PORTAL)));

        ResourceKey<Item> itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        Item.Properties itemProperties = new Item.Properties()
            .setId(itemKey)
            .useBlockDescriptionPrefix();
        BlockItem item = Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(DUNGEON_PORTAL, itemProperties));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(output -> output.accept(item));
    }
}
