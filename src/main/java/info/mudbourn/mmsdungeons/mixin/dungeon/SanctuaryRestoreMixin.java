package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.dungeon.SanctuaryStash;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Puts a rebuilt dungeon chunk's stashed Sanctuary back before its light is worked out.
@Mixin(ChunkStatusTasks.class)
public abstract class SanctuaryRestoreMixin {

    @Inject(method = "initializeLight", at = @At("HEAD"))
    private static void mmsDungeons$restoreSanctuary(
        WorldGenContext context,
        ChunkStep step,
        StaticCache2D<GenerationChunkHolder> cache,
        ChunkAccess chunk,
        CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        if (DungeonDimension.isDungeon(context.level())) {
            SanctuaryStash.restore(context.level(), chunk);
        }
    }
}
