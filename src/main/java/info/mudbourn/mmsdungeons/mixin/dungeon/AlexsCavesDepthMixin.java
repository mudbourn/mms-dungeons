package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.dungeon.PericulumBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Tells the Alex's Caves lookup when the dungeon is sampling a Periculum height. ac_rareBiomeAt is added
// by Alex's Caves' own mixin, so this one runs at a later priority.
@Mixin(value = MultiNoiseBiomeSource.class, priority = 1500)
public abstract class AlexsCavesDepthMixin {

    @Unique
    private static final ResourceKey<Biome> DUNGEON_CAVES = ResourceKey.create(Registries.BIOME, MmsDungeons.id("dungeon_caves"));

    @Unique
    private Boolean mmsDungeons$dungeon;

    @Inject(method = "ac_rareBiomeAt", at = @At("HEAD"), remap = false)
    private void mmsDungeons$markDepth(int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        PericulumBiomes.sampling(mmsDungeons$isDungeon() && QuartPos.toBlock(y) <= DungeonDimension.PERICULUM_TOP);
    }

    @Inject(method = "ac_rareBiomeAt", at = @At("RETURN"), remap = false)
    private void mmsDungeons$clearDepth(int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        PericulumBiomes.sampling(false);
    }

    @Unique
    private boolean mmsDungeons$isDungeon() {
        if (mmsDungeons$dungeon == null) {
            mmsDungeons$dungeon = ((MultiNoiseBiomeSource) (Object) this).possibleBiomes().stream().anyMatch(h -> h.is(DUNGEON_CAVES));
        }
        return mmsDungeons$dungeon;
    }
}
