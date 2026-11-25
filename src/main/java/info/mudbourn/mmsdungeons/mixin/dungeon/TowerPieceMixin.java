package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.TowerPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Saves a top height on tower pieces and places nothing above it.
@Mixin(PoolElementStructurePiece.class)
public abstract class TowerPieceMixin implements TowerPiece {

    @Unique
    private int mmsDungeons$towerTop = TowerPiece.UNSET;

    @Override
    public int mmsDungeons$towerTop() {
        return mmsDungeons$towerTop;
    }

    @Override
    public void mmsDungeons$setTowerTop(int top) {
        mmsDungeons$towerTop = top;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/levelgen/structure/pieces/StructurePieceSerializationContext;Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void mmsDungeons$loadTop(StructurePieceSerializationContext context, CompoundTag tag, CallbackInfo ci) {
        mmsDungeons$towerTop = tag.getIntOr("MmsTowerTop", TowerPiece.UNSET);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void mmsDungeons$saveTop(StructurePieceSerializationContext context, CompoundTag tag, CallbackInfo ci) {
        if (mmsDungeons$towerTop != TowerPiece.UNSET) {
            tag.putInt("MmsTowerTop", mmsDungeons$towerTop);
        }
    }

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$skipAboveTop(
        WorldGenLevel level,
        StructureManager structureManager,
        ChunkGenerator generator,
        RandomSource random,
        BoundingBox chunkBB,
        BlockPos referencePos,
        boolean keepJigsaws,
        CallbackInfo ci
    ) {
        if (mmsDungeons$towerTop != TowerPiece.UNSET && chunkBB.minY() > mmsDungeons$towerTop) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "place", at = @At("HEAD"), argsOnly = true)
    private BoundingBox mmsDungeons$clampToTop(BoundingBox chunkBB) {
        if (mmsDungeons$towerTop == TowerPiece.UNSET || chunkBB.maxY() <= mmsDungeons$towerTop) {
            return chunkBB;
        }
        return new BoundingBox(chunkBB.minX(), chunkBB.minY(), chunkBB.minZ(), chunkBB.maxX(), mmsDungeons$towerTop, chunkBB.maxZ());
    }
}
