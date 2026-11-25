package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Pins local difficulty at its maximum everywhere in the dungeon.
@Mixin(ServerLevel.class)
public abstract class DungeonDifficultyMixin {

    // Past the point where world age and chunk time stop raising local difficulty.
    private static final long MAX_WORLD_TICKS = 72000L + 1440000L * 2;
    private static final long MAX_CHUNK_TICKS = 3600000L;

    @Inject(method = "getCurrentDifficultyAt", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$maxDifficulty(BlockPos pos, CallbackInfoReturnable<DifficultyInstance> cir) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (DungeonDimension.isDungeon(level)) {
            cir.setReturnValue(new DifficultyInstance(level.getDifficulty(), MAX_WORLD_TICKS, MAX_CHUNK_TICKS, 1.0f));
        }
    }
}
