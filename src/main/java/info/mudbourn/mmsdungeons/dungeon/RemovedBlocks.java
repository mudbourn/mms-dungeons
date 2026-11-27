package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

// Settles the void air that earlier processors leave where they removed a block, such as a mob spawner.
// Where something stood on the removed block, such as a chest or a pot, a solid block takes its place so it
// does not float. Every other removed block becomes plain air.
public final class RemovedBlocks implements StructureProcessor {

    private final @Nullable DddMaterial material;

    // material turns the filler into the ruin's own family; null keeps DDD's own blocks.
    public RemovedBlocks(@Nullable DddMaterial material) {
        this.material = material;
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
        return info;
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
            if (!info.state().is(Blocks.VOID_AIR)) {
                out.add(info);
                continue;
            }
            BlockPos pos = info.pos();
            BlockState above = states.get(pos.above().asLong());
            boolean holdsSomething = above != null && !above.isAir();
            BlockState settled = holdsSomething ? filler(level, pos, states) : Blocks.AIR.defaultBlockState();
            out.add(new StructureTemplate.StructureBlockInfo(pos, settled, null));
        }
        return out;
    }

    // The block under the hole when it is a full block, so the floor stays one material, otherwise stone bricks.
    private BlockState filler(ServerLevelAccessor level, BlockPos pos, Long2ObjectOpenHashMap<BlockState> states) {
        BlockState below = states.get(pos.below().asLong());
        if (below != null && below.getFluidState().isEmpty() && below.isCollisionShapeFullBlock(level, pos.below())) {
            return below;
        }
        BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
        return material == null ? bricks : material.apply(bricks);
    }

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        return MapCodec.unit(this);
    }
}
