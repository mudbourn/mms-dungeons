package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.DddPlacement;
import info.mudbourn.mmsdungeons.dungeon.DddRuin;
import info.mudbourn.mmsdungeons.dungeon.LabyrinthCells;
import info.mudbourn.mmsdungeons.dungeon.LabyrinthPalette;
import net.minecraft.core.BlockPos;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Drops a labyrinth start outside a labyrinth cell or whose template theme is not its cell's, and any other Deadly Deadly Dungeon start near an Alex's Caves biome.
@Mixin(Structure.class)
public abstract class DddStructureStartMixin {

    @Inject(method = "findValidGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void mmsDungeons$checkStart(
        Structure.GenerationContext context,
        CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir
    ) {
        Optional<Structure.GenerationStub> stub = cir.getReturnValue();
        if (stub.isEmpty()) {
            return;
        }
        Identifier id = context.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey((Structure) (Object) this);
        if (id == null || !DddPlacement.isPlacedDdd(id)) {
            return;
        }
        BlockPos position = stub.get().position();
        DddRuin ruin = DddRuin.of(id);
        boolean drop = ruin == null
            ? DddPlacement.nearAlexsCaves(context, position)
            : !fitsCell(context, position, ruin);
        if (drop) {
            cir.setReturnValue(Optional.empty());
        }
    }

    private static boolean fitsCell(Structure.GenerationContext context, BlockPos position, DddRuin ruin) {
        int cellX = LabyrinthCells.cellOfBlock(position.getX());
        int cellZ = LabyrinthCells.cellOfBlock(position.getZ());
        return LabyrinthCells.isLabyrinth(context.seed(), cellX, cellZ)
            && LabyrinthPalette.theme(context.biomeResolver(), cellX, cellZ) == ruin.theme();
    }
}
