package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

// One start per labyrinth cell, on a chunk near the cell's middle; caves cells get none.
public final class LabyrinthPlacement extends RandomSpreadStructurePlacement {

    public static final MapCodec<LabyrinthPlacement> CODEC = RecordCodecBuilder.mapCodec(
        instance -> placementCodec(instance).apply(instance, LabyrinthPlacement::new)
    );

    private static final int JITTER = 4;
    private static final int INSET = (LabyrinthCells.CELL_CHUNKS - JITTER) / 2;

    public LabyrinthPlacement(
        Vec3i locateOffset,
        AbstractSpreadingStructurePlacement.FrequencyReductionMethod frequencyReductionMethod,
        float frequency,
        int salt,
        Optional<AbstractSpreadingStructurePlacement.ExclusionZone> exclusionZone
    ) {
        super(
            locateOffset,
            frequencyReductionMethod,
            frequency,
            salt,
            exclusionZone,
            LabyrinthCells.CELL_CHUNKS,
            LabyrinthCells.CELL_CHUNKS - JITTER,
            RandomSpreadType.LINEAR
        );
    }

    public static void register() {
        Registry.register(BuiltInRegistries.STRUCTURE_PLACEMENT, MmsDungeons.id("labyrinth_cell"), CODEC);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<RandomSpreadStructurePlacement> codec() {
        return (MapCodec<RandomSpreadStructurePlacement>) (MapCodec<?>) CODEC;
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int chunkX, int chunkZ) {
        ChunkPos spread = super.getPotentialStructureChunk(seed, chunkX, chunkZ);
        return new ChunkPos(spread.x() + INSET, spread.z() + INSET);
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        return super.isPlacementChunk(state, chunkX, chunkZ)
            && LabyrinthCells.isLabyrinth(state.getLevelSeed(), LabyrinthCells.cellOfChunk(chunkX), LabyrinthCells.cellOfChunk(chunkZ));
    }
}
