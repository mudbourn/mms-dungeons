package info.mudbourn.mmsdungeons.mobs.kobolds;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.kobolds.block.KoboldSkullBlock;
import info.mudbourn.mmsdungeons.mobs.kobolds.block.KoboldSkullType;
import info.mudbourn.mmsdungeons.mobs.kobolds.block.KoboldWallSkullBlock;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class KoboldBlocks {

    public static Block KOBOLD_SKULL;
    public static Block KOBOLD_SKULL_WALL;
    public static Block KOBOLD_WITHER_SKULL;
    public static Block KOBOLD_WITHER_SKULL_WALL;

    private KoboldBlocks() {
    }

    public static void register() {
        KOBOLD_SKULL = register(
            "kobold_skull",
            properties("kobold_skull").instrument(NoteBlockInstrument.SKELETON),
            props -> new KoboldSkullBlock(KoboldSkullType.SKELEBOLD, props, () -> KoboldMobs.KOBOLD_SKELETON)
        );
        KOBOLD_SKULL_WALL = register(
            "kobold_skull_wall",
            properties("kobold_skull_wall"),
            props -> new KoboldWallSkullBlock(KoboldSkullType.SKELEBOLD, props, () -> KoboldMobs.KOBOLD_SKELETON)
        );
        KOBOLD_WITHER_SKULL = register(
            "kobold_wither_skull",
            properties("kobold_wither_skull").instrument(NoteBlockInstrument.WITHER_SKELETON),
            props -> new KoboldSkullBlock(KoboldSkullType.WITHERBOLD, props, () -> KoboldMobs.WITHERBOLD)
        );
        KOBOLD_WITHER_SKULL_WALL = register(
            "kobold_wither_skull_wall",
            properties("kobold_wither_skull_wall"),
            props -> new KoboldWallSkullBlock(KoboldSkullType.WITHERBOLD, props, () -> KoboldMobs.WITHERBOLD)
        );
        BlockEntityTypes.SKULL.addValidBlock(KOBOLD_SKULL);
        BlockEntityTypes.SKULL.addValidBlock(KOBOLD_SKULL_WALL);
        BlockEntityTypes.SKULL.addValidBlock(KOBOLD_WITHER_SKULL);
        BlockEntityTypes.SKULL.addValidBlock(KOBOLD_WITHER_SKULL_WALL);
    }

    private static BlockBehaviour.Properties properties(String name) {
        return BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), MmsDungeons.id(name)))
            .mapColor(MapColor.NONE)
            .sound(SoundType.BONE_BLOCK)
            .strength(1.0f)
            .pushReaction(PushReaction.POPPED)
            .noOcclusion()
            .randomTicks();
    }

    private static Block register(String name, BlockBehaviour.Properties properties, Function<BlockBehaviour.Properties, Block> factory) {
        Identifier id = MmsDungeons.id(name);
        return Registry.register(BuiltInRegistries.BLOCK, id, factory.apply(properties));
    }
}
