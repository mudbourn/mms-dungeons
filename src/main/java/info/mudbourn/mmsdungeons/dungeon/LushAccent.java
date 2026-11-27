package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

// Mixes moss into a plain stone brick labyrinth in clumps: mossy variants, moss blocks and moss carpets on floors.
public final class LushAccent implements StructureProcessor {

    public static final LushAccent INSTANCE = new LushAccent();

    // Size in blocks of one clump cell of the moss noise.
    private static final int CLUMP = 6;
    // Noise above this is a mossy patch.
    private static final float PATCH = 0.62F;
    // Noise above this is the heart of a patch, where moss blocks and carpets go.
    private static final float HEART = 0.78F;
    private static final float MOSSY_SHARE = 0.65F;
    private static final float MOSS_BLOCK_SHARE = 0.35F;
    private static final float CARPET_SHARE = 0.45F;
    private static final float CRACKED_SHARE = 0.12F;

    private static final Map<Block, Block> MOSSY = Map.of(
        Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS,
        Blocks.STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_STAIRS,
        Blocks.STONE_BRICK_SLAB, Blocks.MOSSY_STONE_BRICK_SLAB,
        Blocks.STONE_BRICK_WALL, Blocks.MOSSY_STONE_BRICK_WALL,
        Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE,
        Blocks.COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_STAIRS,
        Blocks.COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB,
        Blocks.COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE_WALL
    );

    private LushAccent() {
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
        Block block = info.state().getBlock();
        Block mossy = MOSSY.get(block);
        if (mossy == null) {
            return info;
        }
        BlockPos pos = info.pos();
        float clump = clump(pos);
        float roll = roll(pos, 0L);
        boolean full = block == Blocks.STONE_BRICKS || block == Blocks.COBBLESTONE;
        BlockState out = info.state();
        if (full && clump > HEART && roll < MOSS_BLOCK_SHARE) {
            out = Blocks.MOSS_BLOCK.defaultBlockState();
        } else if (clump > PATCH && roll < MOSSY_SHARE) {
            out = DddMaterial.copyShape(info.state(), mossy);
        } else if (block == Blocks.STONE_BRICKS && roll(pos, 1L) < CRACKED_SHARE) {
            out = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        }
        return out == info.state() ? info : new StructureTemplate.StructureBlockInfo(pos, out, info.nbt());
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
        ServerLevelAccessor level,
        BlockPos offset,
        BlockPos pivot,
        List<StructureTemplate.StructureBlockInfo> originals,
        List<StructureTemplate.StructureBlockInfo> processed,
        StructurePlaceSettings settings
    ) {
        Long2ObjectOpenHashMap<BlockState> states = new Long2ObjectOpenHashMap<>();
        for (StructureTemplate.StructureBlockInfo info : processed) {
            states.put(info.pos().asLong(), info.state());
        }
        List<StructureTemplate.StructureBlockInfo> out = new ArrayList<>(processed.size());
        for (StructureTemplate.StructureBlockInfo info : processed) {
            out.add(carpeted(info, states) ? new StructureTemplate.StructureBlockInfo(info.pos(), Blocks.MOSS_CARPET.defaultBlockState(), null) : info);
        }
        return out;
    }

    // An air block over a full stone floor, with air above it too so doorway lintels stay clear, in the heart of a patch.
    private static boolean carpeted(StructureTemplate.StructureBlockInfo info, Long2ObjectOpenHashMap<BlockState> states) {
        if (!info.state().isAir()) {
            return false;
        }
        BlockPos pos = info.pos();
        BlockState below = states.get(pos.below().asLong());
        BlockState above = states.get(pos.above().asLong());
        if (below == null || above == null || !above.isAir() || !floor(below.getBlock())) {
            return false;
        }
        return clump(pos) > HEART && roll(pos, 2L) < CARPET_SHARE;
    }

    private static boolean floor(Block block) {
        return block == Blocks.STONE_BRICKS
            || block == Blocks.MOSSY_STONE_BRICKS
            || block == Blocks.CRACKED_STONE_BRICKS
            || block == Blocks.COBBLESTONE
            || block == Blocks.MOSSY_COBBLESTONE
            || block == Blocks.MOSS_BLOCK
            || block == Blocks.SMOOTH_STONE;
    }

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        return MapCodec.unit(this);
    }

    // Smooth value noise in 0 to 1 over a coarse lattice, so moss comes in clumps.
    private static float clump(BlockPos pos) {
        int cx = Math.floorDiv(pos.getX(), CLUMP);
        int cy = Math.floorDiv(pos.getY(), CLUMP);
        int cz = Math.floorDiv(pos.getZ(), CLUMP);
        float fx = smooth(Math.floorMod(pos.getX(), CLUMP) / (float) CLUMP);
        float fy = smooth(Math.floorMod(pos.getY(), CLUMP) / (float) CLUMP);
        float fz = smooth(Math.floorMod(pos.getZ(), CLUMP) / (float) CLUMP);
        float x00 = lerp(fx, lattice(cx, cy, cz), lattice(cx + 1, cy, cz));
        float x10 = lerp(fx, lattice(cx, cy + 1, cz), lattice(cx + 1, cy + 1, cz));
        float x01 = lerp(fx, lattice(cx, cy, cz + 1), lattice(cx + 1, cy, cz + 1));
        float x11 = lerp(fx, lattice(cx, cy + 1, cz + 1), lattice(cx + 1, cy + 1, cz + 1));
        return lerp(fz, lerp(fy, x00, x10), lerp(fy, x01, x11));
    }

    private static float lattice(int x, int y, int z) {
        return unit(LabyrinthCells.mix(x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L));
    }

    private static float roll(BlockPos pos, long salt) {
        return unit(LabyrinthCells.mix(pos.asLong() * 0x9E3779B97F4A7C15L + salt));
    }

    private static float unit(long hash) {
        return (hash >>> 40) / (float) (1L << 24);
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    private static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }
}
