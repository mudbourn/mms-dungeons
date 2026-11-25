package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.dungeon.FillCaverns;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Gives fill caverns their biome over whatever the dungeon's biome source picks.
@Mixin(value = MultiNoiseBiomeSource.class, priority = 2000)
public abstract class PericulumFillBiomeMixin {

    @Unique
    private static final ResourceKey<Biome> DUNGEON_CAVES = ResourceKey.create(
        Registries.BIOME,
        MmsDungeons.id("dungeon_caves")
    );

    @Unique
    private Boolean mmsDungeons$dungeon;

    @Inject(method = "createResolver", at = @At("RETURN"), cancellable = true)
    private void mmsDungeons$fillResolver(Climate.Sampler sampler, CallbackInfoReturnable<BiomeResolver> cir) {
        cir.setReturnValue(mmsDungeons$wrap(cir.getReturnValue()));
    }

    @Inject(method = "createResolverForChunk", at = @At("RETURN"), cancellable = true)
    private void mmsDungeons$fillChunkResolver(
        Climate.Sampler sampler,
        int minX,
        int minY,
        int minZ,
        int sizeX,
        int sizeY,
        int sizeZ,
        CallbackInfoReturnable<BiomeResolver> cir
    ) {
        cir.setReturnValue(mmsDungeons$wrap(cir.getReturnValue()));
    }

    @Unique
    private BiomeResolver mmsDungeons$wrap(BiomeResolver resolver) {
        if (!mmsDungeons$isDungeon()) {
            return resolver;
        }
        return (x, y, z) -> {
            Holder<Biome> fill = FillCaverns.biomeAt(
                QuartPos.toBlock(x) + 2,
                QuartPos.toBlock(y) + 2,
                QuartPos.toBlock(z) + 2
            );
            return fill != null ? fill : resolver.getNoiseBiome(x, y, z);
        };
    }

    @Unique
    private boolean mmsDungeons$isDungeon() {
        if (mmsDungeons$dungeon == null) {
            mmsDungeons$dungeon = ((MultiNoiseBiomeSource) (Object) this).possibleBiomes()
                .stream()
                .anyMatch(holder -> holder.is(DUNGEON_CAVES));
        }
        return mmsDungeons$dungeon;
    }
}
