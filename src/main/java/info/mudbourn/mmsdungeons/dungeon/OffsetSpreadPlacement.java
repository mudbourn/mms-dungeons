package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

// Random spread whose grid is shifted by a fixed number of chunks on both axes.
public final class OffsetSpreadPlacement extends RandomSpreadStructurePlacement {

    public static final MapCodec<OffsetSpreadPlacement> CODEC = RecordCodecBuilder.mapCodec(
        instance -> placementCodec(instance)
            .and(
                instance.group(
                    Codec.intRange(0, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing),
                    Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation),
                    RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(RandomSpreadStructurePlacement::spreadType),
                    Codec.INT.fieldOf("grid_offset").forGetter(OffsetSpreadPlacement::gridOffset)
                )
            )
            .apply(instance, OffsetSpreadPlacement::new)
    );

    private final int gridOffset;

    public OffsetSpreadPlacement(
        Vec3i locateOffset,
        AbstractSpreadingStructurePlacement.FrequencyReductionMethod frequencyReductionMethod,
        float frequency,
        int salt,
        Optional<AbstractSpreadingStructurePlacement.ExclusionZone> exclusionZone,
        int spacing,
        int separation,
        RandomSpreadType spreadType,
        int gridOffset
    ) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone, spacing, separation, spreadType);
        this.gridOffset = gridOffset;
    }

    public static void register() {
        Registry.register(BuiltInRegistries.STRUCTURE_PLACEMENT, MmsDungeons.id("offset_spread"), CODEC);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<RandomSpreadStructurePlacement> codec() {
        return (MapCodec<RandomSpreadStructurePlacement>) (MapCodec<?>) CODEC;
    }

    public int gridOffset() {
        return gridOffset;
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int chunkX, int chunkZ) {
        ChunkPos shifted = super.getPotentialStructureChunk(seed, chunkX - gridOffset, chunkZ - gridOffset);
        return new ChunkPos(shifted.x() + gridOffset, shifted.z() + gridOffset);
    }
}
