package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.DddPlacement;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Drops a Deadly Deadly Dungeon start that would sit within 250 blocks of an Alex's Caves biome.
@Mixin(Structure.class)
public abstract class DddStructureStartMixin {

    @Inject(method = "findValidGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void mmsDungeons$keepClearOfAlexsCaves(
        Structure.GenerationContext context,
        CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir
    ) {
        Optional<Structure.GenerationStub> stub = cir.getReturnValue();
        if (stub.isEmpty()) {
            return;
        }
        Identifier id = context.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey((Structure) (Object) this);
        if (id != null && DddPlacement.isPlacedDdd(id) && DddPlacement.nearAlexsCaves(context, stub.get().position())) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
