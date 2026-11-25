package info.mudbourn.mmsdungeons.guild;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

// Registers the quest board block, its block entity and the item it is placed with.
public final class GuildBlocks {

    public static Block QUEST_BOARD;
    public static BlockEntityType<QuestBoardBlockEntity> QUEST_BOARD_ENTITY;

    private GuildBlocks() {
    }

    static void register() {
        Identifier id = MmsDungeons.id("quest_board");
        ResourceKey<Block> blockKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), id);
        QUEST_BOARD = Registry.register(
            BuiltInRegistries.BLOCK,
            id,
            new QuestBoardBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().setId(blockKey)));
        QUEST_BOARD_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id,
            new BlockEntityType<>(QuestBoardBlockEntity::new, Set.of(QUEST_BOARD)));

        ResourceKey<Item> itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        BlockItem item = Registry.register(
            BuiltInRegistries.ITEM,
            id,
            new BlockItem(QUEST_BOARD, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
            .register(output -> output.accept(item));
    }
}
