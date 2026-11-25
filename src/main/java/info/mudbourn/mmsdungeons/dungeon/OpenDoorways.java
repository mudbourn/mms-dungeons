package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

// Turns doorway jigsaws that would seal into stone bricks into air.
public final class OpenDoorways implements StructureProcessor {

    public static final OpenDoorways INSTANCE = new OpenDoorways();
    private static final MapCodec<OpenDoorways> CODEC = MapCodec.unit(INSTANCE);
    private static final String SEAL = "minecraft:stone_bricks";

    private OpenDoorways() {
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
        if (info.state().is(Blocks.JIGSAW) && info.nbt() != null && SEAL.equals(info.nbt().getStringOr("final_state", ""))) {
            return new StructureTemplate.StructureBlockInfo(info.pos(), Blocks.AIR.defaultBlockState(), null);
        }
        return info;
    }

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        return CODEC;
    }
}
