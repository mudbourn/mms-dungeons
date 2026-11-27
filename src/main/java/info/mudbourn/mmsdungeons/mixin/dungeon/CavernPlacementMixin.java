package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.CavernGrid;
import info.mudbourn.mmsdungeons.dungeon.DungeonGeneration;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Moves the Aerial Hell caverns with every Deep Region rebuild.
@Mixin(RandomSpreadStructurePlacement.class)
public abstract class CavernPlacementMixin {

    @ModifyVariable(method = "getPotentialStructureChunk", at = @At("HEAD"), argsOnly = true)
    private long mmsDungeons$rebuildSeed(long seed) {
        return CavernGrid.isCavernPlacement((RandomSpreadStructurePlacement) (Object) this) ? DungeonGeneration.layoutSeed(seed) : seed;
    }
}
