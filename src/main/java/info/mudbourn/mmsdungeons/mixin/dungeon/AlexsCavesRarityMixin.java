package info.mudbourn.mmsdungeons.mixin.dungeon;

import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import com.github.alexmodguy.alexscaves.server.misc.VoronoiGenerator.VoronoiInfo;
import info.mudbourn.mmsdungeons.dungeon.AlexsCavesSpacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Empties an Alex's Caves cell that sits too close to a copy of the same biome.
@Mixin(ACBiomeRarity.class)
public abstract class AlexsCavesRarityMixin {

    @Inject(method = "getRareBiomeInfoForQuad", at = @At("RETURN"), cancellable = true, remap = false)
    private static void mmsDungeons$spaceCopies(long worldSeed, int x, int z, CallbackInfoReturnable<VoronoiInfo> cir) {
        VoronoiInfo info = cir.getReturnValue();
        if (info != null && AlexsCavesSpacing.yields(worldSeed, info)) {
            cir.setReturnValue(null);
        }
    }
}
