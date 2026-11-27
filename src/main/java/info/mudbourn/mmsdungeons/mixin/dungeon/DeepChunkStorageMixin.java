package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.dungeon.DungeonGeneration;
import info.mudbourn.mmsdungeons.dungeon.SanctuaryStash;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Rebuilds stale dungeon chunks as they load and records which ones are saved with current content.
@Mixin(SerializableChunkData.class)
public abstract class DeepChunkStorageMixin {

    @Inject(method = "read", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$rebuildStale(ServerLevel level, PoiManager poi, RegionStorageInfo info, ChunkPos pos, CallbackInfoReturnable<ProtoChunk> cir) {
        if (DungeonDimension.isDungeon(level) && DungeonGeneration.isStale(pos)) {
            cir.setReturnValue(SanctuaryStash.rebuild((SerializableChunkData) (Object) this, level, poi, pos));
        }
    }

    @Inject(method = "copyOf", at = @At("HEAD"))
    private static void mmsDungeons$markSaved(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<SerializableChunkData> cir) {
        if (DungeonDimension.isDungeon(level)) {
            DungeonGeneration.markSaved(chunk.getPos());
        }
    }
}
