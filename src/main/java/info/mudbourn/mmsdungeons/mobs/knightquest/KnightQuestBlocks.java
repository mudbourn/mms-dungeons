package info.mudbourn.mmsdungeons.mobs.knightquest;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.knightquest.block.GreatChaliceBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class KnightQuestBlocks {

    public static Block GREAT_CHALICE;

    private KnightQuestBlocks() {
    }

    public static void register() {
        GREAT_CHALICE = Registry.register(
            BuiltInRegistries.BLOCK,
            MmsDungeons.id("great_chalice"),
            new GreatChaliceBlock(
                BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), MmsDungeons.id("great_chalice")))
                    .mapColor(MapColor.GOLD)
                    .sound(SoundType.METAL)
                    .strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
            )
        );
    }
}
