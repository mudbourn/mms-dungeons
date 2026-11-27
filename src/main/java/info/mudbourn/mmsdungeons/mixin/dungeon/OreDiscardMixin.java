package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.OreExposure;
import net.minecraft.world.level.levelgen.feature.AbstractOreFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// Scales the air-exposure discard chance of ore veins in the dungeon.
@Mixin(AbstractOreFeature.class)
public abstract class OreDiscardMixin {

    @ModifyArg(
        method = "canPlaceOre",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/AbstractOreFeature;shouldSkipAirCheck(Lnet/minecraft/util/RandomSource;F)Z"),
        index = 1
    )
    private float mmsDungeons$scaleDiscard(float chance) {
        return OreExposure.discardChance(chance);
    }
}
