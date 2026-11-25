package info.mudbourn.mmsdungeons.mixin.dungeon;

import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition;
import com.github.alexmodguy.alexscaves.server.misc.VoronoiGenerator.VoronoiInfo;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import info.mudbourn.mmsdungeons.dungeon.AlexsCavesRotation;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.dungeon.PericulumBiomes;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Keeps Alex's Caves biomes in the Deep Region and places them by the rotating distance from spawn alone.
@Mixin(BiomeGenerationNoiseCondition.class)
public abstract class AlexsCavesConditionMixin {

    private static final String TEST = "test(IIIFLnet/minecraft/world/level/biome/Climate$Sampler;Lnet/minecraft/resources/ResourceKey;Lcom/github/alexmodguy/alexscaves/server/misc/VoronoiGenerator$VoronoiInfo;)Z";

    @Shadow(remap = false)
    private boolean disabledCompletely;

    @Shadow(remap = false)
    public abstract int getRarityOffset();

    // In the dungeon only the region and spawn distance decide; its climate noise rarely fits Alex's Caves ranges.
    @Inject(method = TEST, at = @At("HEAD"), cancellable = true, remap = false)
    private void mmsDungeons$deepOnly(int x, int y, int z, float depth, Climate.Sampler sampler, ResourceKey<Level> dimension, VoronoiInfo info, CallbackInfoReturnable<Boolean> cir) {
        if (dimension != DungeonDimension.LEVEL) {
            return;
        }
        if (disabledCompletely || QuartPos.toBlock(y + 1) > DungeonDimension.SANCTUARY_BOTTOM) {
            cir.setReturnValue(false);
            return;
        }
        Vec3 center = ACBiomeRarity.getRareBiomeCenter(info);
        double distance = AlexsCavesRotation.distanceFor(getRarityOffset());
        if (QuartPos.toBlock(y) <= DungeonDimension.PERICULUM_TOP) {
            distance *= PericulumBiomes.LENIENCY;
        }
        cir.setReturnValue(center != null
            && QuartPos.toBlock((int) center.x) * (double) QuartPos.toBlock((int) center.x)
            + QuartPos.toBlock((int) center.z) * (double) QuartPos.toBlock((int) center.z) >= distance * distance);
    }
}
