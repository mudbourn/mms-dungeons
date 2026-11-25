package info.mudbourn.mmsdungeons.mixin.dungeon;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import info.mudbourn.mmsdungeons.dungeon.DddRuin;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// Tells piece placement which Deadly Deadly Dungeon ruin its start belongs to.
@Mixin(StructureStart.class)
public abstract class DddStructurePlaceMixin {

    @Shadow
    @Final
    private Structure structure;

    @WrapMethod(method = "placeInChunk")
    private void mmsDungeons$markRuin(
        WorldGenLevel level,
        StructureManager structures,
        ChunkGenerator generator,
        RandomSource random,
        BoundingBox box,
        ChunkPos chunkPos,
        Operation<Void> original
    ) {
        DddRuin.setPlacing(DddRuin.of(level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(structure)));
        try {
            original.call(level, structures, generator, random, box, chunkPos);
        } finally {
            DddRuin.setPlacing(null);
        }
    }
}
