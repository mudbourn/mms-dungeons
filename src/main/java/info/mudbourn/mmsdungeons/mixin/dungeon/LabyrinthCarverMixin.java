package info.mudbourn.mmsdungeons.mixin.dungeon;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.dungeon.LabyrinthCells;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.CarverOutput;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Keeps carvers out of the Deep Region part of labyrinth chunks.
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class LabyrinthCarverMixin {

    @WrapOperation(
        method = "generateCarvers",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/carver/WorldCarver;carve(Lnet/minecraft/world/level/levelgen/WorldGenerationContext;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/CarverOutput;)Z"
        )
    )
    private boolean mmsDungeons$skipLabyrinth(
        WorldCarver carver,
        WorldGenerationContext context,
        RandomSource random,
        ChunkPos source,
        ChunkPos target,
        CarverOutput output,
        Operation<Boolean> original,
        @Local(argsOnly = true) ChunkAccess chunk,
        @Local(argsOnly = true) WorldGenRegion region
    ) {
        ChunkPos pos = chunk.getPos();
        boolean labyrinth = DungeonDimension.isDungeon(region.getLevel())
            && LabyrinthCells.isLabyrinthColumn(pos.getMinBlockX(), pos.getMinBlockZ());
        return original.call(carver, context, random, source, target, labyrinth ? new LabyrinthCells.AboveDeepRegion(output) : output);
    }
}
