package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.ClearableStructureCheck;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.dungeon.DungeonGeneration;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Keeps structure lookups from trusting the starts stored in stale dungeon chunks.
@Mixin(StructureCheck.class)
public abstract class DeepStructureCheckMixin implements ClearableStructureCheck {

    @Shadow
    @Final
    private ResourceKey<Level> dimension;

    @Shadow
    @Final
    private Long2ObjectMap<Object2IntMap<Structure>> loadedChunks;

    @Shadow
    @Final
    private Map<Structure, Long2BooleanMap> featureChecks;

    @Shadow
    @Final
    private LongSet chunksWithoutStartsInStorage;

    @Inject(method = "tryLoadFromStorage", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$skipStale(ChunkPos pos, Structure structure, boolean requireUnreferenced, CallbackInfoReturnable<StructureCheckResult> cir) {
        if (dimension == DungeonDimension.LEVEL && DungeonGeneration.isStale(pos)) {
            cir.setReturnValue(StructureCheckResult.CHUNK_LOAD_NEEDED);
        }
    }

    @Override
    public void mmsDungeons$clear() {
        synchronized (this) {
            loadedChunks.clear();
            featureChecks.clear();
            chunksWithoutStartsInStorage.clear();
        }
    }
}
