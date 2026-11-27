package info.mudbourn.mmsdungeons.mixin.features;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Makes spike blocks impassable to ground pathfinding.
@Mixin(WalkNodeEvaluator.class)
public abstract class SpikePathingMixin {

    private static final TagKey<Block> MMS_DUNGEONS$PATH_SPIKES =
        TagKey.create(Registries.BLOCK, MmsDungeons.id("path_spikes"));

    @Inject(method = "getPathTypeFromState", at = @At("RETURN"), cancellable = true)
    private static void mmsDungeons$avoidSpikes(
        BlockGetter level,
        BlockPos pos,
        CallbackInfoReturnable<PathType> cir
    ) {
        PathType type = cir.getReturnValue();
        if (type == PathType.DAMAGING || type == PathType.BLOCKED) {
            return;
        }
        if (level.getBlockState(pos).is(MMS_DUNGEONS$PATH_SPIKES)) {
            cir.setReturnValue(PathType.DAMAGING);
        }
    }
}
