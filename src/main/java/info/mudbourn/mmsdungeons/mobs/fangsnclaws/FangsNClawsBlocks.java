package info.mudbourn.mmsdungeons.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.block.BearTrapBlock;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.block.GhostBlock;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.block.GhostBlockEntity;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.block.VileLanternBlock;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

// The Fangs 'n Claws blocks.
public final class FangsNClawsBlocks {

    public static Block GHOST_BLOCK;
    public static Block BEAR_TRAP;
    public static Block VILE_LANTERN;
    public static BlockEntityType<GhostBlockEntity> GHOST_BLOCK_ENTITY;

    private FangsNClawsBlocks() {
    }

    public static void register() {
        GHOST_BLOCK = register(
            "ghost_block",
            properties("ghost_block")
                .strength(0.3f)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false)
                .isViewBlocking((state, level, pos, area) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false),
            GhostBlock::new
        );
        BEAR_TRAP = register(
            "bear_trap",
            properties("bear_trap").mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.METAL).noOcclusion(),
            BearTrapBlock::new
        );
        VILE_LANTERN = register(
            "vile_lantern",
            properties("vile_lantern")
                .mapColor(MapColor.METAL)
                .forceSolidOn()
                .requiresCorrectToolForDrops()
                .strength(3.5f)
                .sound(SoundType.LANTERN)
                .lightLevel(state -> 15)
                .noOcclusion()
                .pushReaction(PushReaction.POPPED),
            VileLanternBlock::new
        );
        GHOST_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            MmsDungeons.id("ghost_block"),
            new BlockEntityType<>(GhostBlockEntity::new, Set.of(GHOST_BLOCK))
        );
    }

    private static BlockBehaviour.Properties properties(String name) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), MmsDungeons.id(name)));
    }

    private static Block register(String name, BlockBehaviour.Properties properties, Function<BlockBehaviour.Properties, Block> factory) {
        return Registry.register(BuiltInRegistries.BLOCK, MmsDungeons.id(name), factory.apply(properties));
    }
}
