package info.mudbourn.mmsdungeons.mixin.dungeon;

import com.mojang.datafixers.util.Pair;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Searches dungeon biomes at a vertical step fine enough to hit the thin Aerial Hell cavern band.
@Mixin(ServerLevel.class)
public abstract class LocateBiomeStepMixin {

    @Unique
    private static final int STEP = 16;

    @Inject(method = "findClosestBiome3d", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$fineStep(Predicate<Holder<Biome>> biome, BlockPos origin, int radius, int horizontalStep, int verticalStep, CallbackInfoReturnable<Pair<BlockPos, Holder<Biome>>> cir) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (verticalStep > STEP && DungeonDimension.isDungeon(level)) {
            cir.setReturnValue(level.findClosestBiome3d(biome, origin, radius, horizontalStep, STEP));
        }
    }
}
