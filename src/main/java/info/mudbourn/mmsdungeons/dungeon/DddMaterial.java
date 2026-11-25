package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

// Turns every block of a ruin into one family, all stone or all deepslate, keeping each block's shape.
public final class DddMaterial implements StructureProcessor {

    public static final DddMaterial STONE = new DddMaterial(toStone());
    public static final DddMaterial DEEPSLATE = new DddMaterial(toDeepslate());

    private final Map<Block, Block> mapping;

    private DddMaterial(Map<Block, Block> mapping) {
        this.mapping = mapping;
    }

    public static DddMaterial of(DddRuin ruin) {
        return ruin == DddRuin.DEEPSLATE ? DEEPSLATE : STONE;
    }

    @Override
    public StructureTemplate.@Nullable StructureBlockInfo processBlock(
        LevelReader level,
        BlockPos targetPosition,
        BlockPos referencePos,
        BlockPos templateRelativePos,
        StructureTemplate.StructureBlockInfo info,
        StructurePlaceSettings settings
    ) {
        Block target = mapping.get(info.state().getBlock());
        if (target == null) {
            return info;
        }
        return new StructureTemplate.StructureBlockInfo(info.pos(), copyShape(info.state(), target), info.nbt());
    }

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        return MapCodec.unit(this);
    }

    private static BlockState copyShape(BlockState from, Block to) {
        BlockState out = to.defaultBlockState();
        for (Property<?> property : from.getProperties()) {
            out = copyProperty(out, from, property);
        }
        return out;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState out, BlockState from, Property<T> property) {
        return out.hasProperty(property) ? out.setValue(property, from.getValue(property)) : out;
    }

    private static Map<Block, Block> toDeepslate() {
        Map<Block, Block> map = new HashMap<>();
        map.put(Blocks.STONE, Blocks.DEEPSLATE);
        map.put(Blocks.SMOOTH_STONE, Blocks.POLISHED_DEEPSLATE);
        map.put(Blocks.SMOOTH_STONE_SLAB, Blocks.POLISHED_DEEPSLATE_SLAB);
        map.put(Blocks.STONE_SLAB, Blocks.COBBLED_DEEPSLATE_SLAB);
        map.put(Blocks.STONE_STAIRS, Blocks.COBBLED_DEEPSLATE_STAIRS);
        map.put(Blocks.COBBLESTONE, Blocks.COBBLED_DEEPSLATE);
        map.put(Blocks.COBBLESTONE_SLAB, Blocks.COBBLED_DEEPSLATE_SLAB);
        map.put(Blocks.COBBLESTONE_STAIRS, Blocks.COBBLED_DEEPSLATE_STAIRS);
        map.put(Blocks.COBBLESTONE_WALL, Blocks.COBBLED_DEEPSLATE_WALL);
        map.put(Blocks.MOSSY_COBBLESTONE, Blocks.COBBLED_DEEPSLATE);
        map.put(Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.COBBLED_DEEPSLATE_SLAB);
        map.put(Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.COBBLED_DEEPSLATE_STAIRS);
        map.put(Blocks.MOSSY_COBBLESTONE_WALL, Blocks.COBBLED_DEEPSLATE_WALL);
        map.put(Blocks.STONE_BRICKS, Blocks.DEEPSLATE_BRICKS);
        map.put(Blocks.CRACKED_STONE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS);
        map.put(Blocks.MOSSY_STONE_BRICKS, Blocks.DEEPSLATE_BRICKS);
        map.put(Blocks.CHISELED_STONE_BRICKS, Blocks.CHISELED_DEEPSLATE);
        map.put(Blocks.STONE_BRICK_STAIRS, Blocks.DEEPSLATE_BRICK_STAIRS);
        map.put(Blocks.STONE_BRICK_SLAB, Blocks.DEEPSLATE_BRICK_SLAB);
        map.put(Blocks.STONE_BRICK_WALL, Blocks.DEEPSLATE_BRICK_WALL);
        map.put(Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.DEEPSLATE_BRICK_STAIRS);
        map.put(Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.DEEPSLATE_BRICK_SLAB);
        map.put(Blocks.MOSSY_STONE_BRICK_WALL, Blocks.DEEPSLATE_BRICK_WALL);
        map.put(Blocks.INFESTED_STONE, Blocks.INFESTED_DEEPSLATE);
        map.put(Blocks.INFESTED_STONE_BRICKS, Blocks.INFESTED_DEEPSLATE);
        map.put(Blocks.INFESTED_CRACKED_STONE_BRICKS, Blocks.INFESTED_DEEPSLATE);
        map.put(Blocks.INFESTED_MOSSY_STONE_BRICKS, Blocks.INFESTED_DEEPSLATE);
        map.put(Blocks.INFESTED_CHISELED_STONE_BRICKS, Blocks.INFESTED_DEEPSLATE);
        map.put(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        map.put(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        map.put(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        map.put(Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE);
        map.put(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        map.put(Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        map.put(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        map.put(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        return map;
    }

    private static Map<Block, Block> toStone() {
        Map<Block, Block> map = new HashMap<>();
        map.put(Blocks.DEEPSLATE, Blocks.STONE);
        map.put(Blocks.COBBLED_DEEPSLATE, Blocks.COBBLESTONE);
        map.put(Blocks.COBBLED_DEEPSLATE_SLAB, Blocks.COBBLESTONE_SLAB);
        map.put(Blocks.COBBLED_DEEPSLATE_STAIRS, Blocks.COBBLESTONE_STAIRS);
        map.put(Blocks.COBBLED_DEEPSLATE_WALL, Blocks.COBBLESTONE_WALL);
        map.put(Blocks.POLISHED_DEEPSLATE, Blocks.SMOOTH_STONE);
        map.put(Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.SMOOTH_STONE_SLAB);
        map.put(Blocks.POLISHED_DEEPSLATE_STAIRS, Blocks.STONE_STAIRS);
        map.put(Blocks.POLISHED_DEEPSLATE_WALL, Blocks.STONE_BRICK_WALL);
        map.put(Blocks.DEEPSLATE_BRICKS, Blocks.STONE_BRICKS);
        map.put(Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.CRACKED_STONE_BRICKS);
        map.put(Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.STONE_BRICK_STAIRS);
        map.put(Blocks.DEEPSLATE_BRICK_SLAB, Blocks.STONE_BRICK_SLAB);
        map.put(Blocks.DEEPSLATE_BRICK_WALL, Blocks.STONE_BRICK_WALL);
        map.put(Blocks.DEEPSLATE_TILES, Blocks.STONE_BRICKS);
        map.put(Blocks.CRACKED_DEEPSLATE_TILES, Blocks.CRACKED_STONE_BRICKS);
        map.put(Blocks.DEEPSLATE_TILE_STAIRS, Blocks.STONE_BRICK_STAIRS);
        map.put(Blocks.DEEPSLATE_TILE_SLAB, Blocks.STONE_BRICK_SLAB);
        map.put(Blocks.DEEPSLATE_TILE_WALL, Blocks.STONE_BRICK_WALL);
        map.put(Blocks.CHISELED_DEEPSLATE, Blocks.CHISELED_STONE_BRICKS);
        map.put(Blocks.REINFORCED_DEEPSLATE, Blocks.STONE_BRICKS);
        map.put(Blocks.INFESTED_DEEPSLATE, Blocks.INFESTED_STONE);
        map.put(Blocks.DEEPSLATE_COAL_ORE, Blocks.COAL_ORE);
        map.put(Blocks.DEEPSLATE_IRON_ORE, Blocks.IRON_ORE);
        map.put(Blocks.DEEPSLATE_COPPER_ORE, Blocks.COPPER_ORE);
        map.put(Blocks.DEEPSLATE_GOLD_ORE, Blocks.GOLD_ORE);
        map.put(Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.REDSTONE_ORE);
        map.put(Blocks.DEEPSLATE_LAPIS_ORE, Blocks.LAPIS_ORE);
        map.put(Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DIAMOND_ORE);
        map.put(Blocks.DEEPSLATE_EMERALD_ORE, Blocks.EMERALD_ORE);
        return map;
    }
}
