package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.DddPlacement;
import info.mudbourn.mmsdungeons.dungeon.DddPruning;
import info.mudbourn.mmsdungeons.dungeon.DddRuin;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.dungeon.DungeonOverlaps;
import info.mudbourn.mmsdungeons.dungeon.LabyrinthLinks;
import info.mudbourn.mmsdungeons.dungeon.ReachableStructures;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Prunes dead ends from Deadly Deadly Dungeon starts in the dungeon and drops any other structure there with no way in or that would overlap a labyrinth.
@Mixin(Structure.class)
public abstract class DddPruneMixin {

    @Inject(method = "generate", at = @At("RETURN"), cancellable = true)
    private void mmsDungeons$pruneDeadEnds(
        Holder<Structure> structure,
        ResourceKey<Level> dimension,
        RegistryAccess registryAccess,
        ChunkGenerator generator,
        BiomeSource biomeSource,
        Climate.Sampler sampler,
        RandomState randomState,
        StructureTemplateManager templates,
        long seed,
        ChunkPos chunkPos,
        int references,
        LevelHeightAccessor heights,
        Predicate<Holder<Biome>> validBiome,
        CallbackInfoReturnable<StructureStart> cir
    ) {
        StructureStart start = cir.getReturnValue();
        if (!dimension.equals(DungeonDimension.LEVEL) || !start.isValid()) {
            return;
        }
        Identifier id = structure.unwrapKey().map(ResourceKey::identifier).orElse(null);
        if (id == null) {
            return;
        }
        if (!ReachableStructures.exempt(id)) {
            boolean refused = DungeonOverlaps.clashes(id, start.getBoundingBox())
                || !ReachableStructures.touchesCaveAir(start.getBoundingBox(), generator, randomState, heights);
            if (refused) {
                cir.setReturnValue(StructureStart.INVALID_START);
            }
            return;
        }
        if (!DddPlacement.isPlacedDdd(id)) {
            return;
        }
        DddRuin ruin = DddRuin.of(id);
        if (ruin == null) {
            return;
        }
        List<StructurePiece> kept = DddPruning.prune(start.getPieces(), ruin, generator, randomState, templates, heights);
        if (kept.isEmpty()) {
            cir.setReturnValue(StructureStart.INVALID_START);
            return;
        }
        kept.addAll(LabyrinthLinks.corridors(kept, ruin, seed, templates));
        cir.setReturnValue(new StructureStart(start.getStructure(), chunkPos, references, new PiecesContainer(kept)));
    }
}
