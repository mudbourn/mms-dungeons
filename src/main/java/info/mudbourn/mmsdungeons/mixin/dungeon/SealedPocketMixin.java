package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.dungeon.SealedPockets;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Fills sealed air pockets in dungeon chunks once carvers have run.
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class SealedPocketMixin {

    @Inject(method = "generateCarvers", at = @At("TAIL"))
    private void mmsDungeons$fillPockets(
        ChunkAccess chunk,
        Blender blender,
        NoiseChunk noiseChunk,
        RandomState randomState,
        BiomeManager biomeManager,
        WorldGenRegion region,
        MaterialRule rule,
        CallbackInfo ci
    ) {
        if (DungeonDimension.isDungeon(region.getLevel())) {
            SealedPockets.fill(chunk);
        }
    }
}
