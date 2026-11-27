package info.mudbourn.mmsdungeons.mixin.dungeon;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.dungeon.DungeonGeneration;
import info.mudbourn.mmsdungeons.dungeon.NetherPatches;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

// Breaks up large single-biome patches in the Periculum and caps Nether Infection patches in the Deep and Periculum Regions.
@Mixin(value = MultiNoiseBiomeSource.class, priority = 1600)
public abstract class PericulumSpreadMixin {

    // Quarts to each side that must share the biome before it is swapped.
    @Unique
    private static final int REACH = 16;

    // Quart offset of the replacement climate sample.
    @Unique
    private static final int FAR = 2500;

    @Unique
    private static final ResourceKey<Biome> DUNGEON_CAVES = ResourceKey.create(Registries.BIOME, MmsDungeons.id("dungeon_caves"));

    @Unique
    private static final TagKey<Biome> NETHER = TagKey.create(Registries.BIOME, MmsDungeons.id("nether_infestation"));

    // Erosion that stands in for a removed Nether Infection biome.
    @Unique
    private static final long STAND_IN_EROSION = Climate.quantizeCoord(0.1f);

    // Continentalness above which a cell's second Nether Infection biome is used.
    @Unique
    private static final long PAIR_SPLIT = Climate.quantizeCoord(0.2725f);

    @Unique
    private Boolean mmsDungeons$spreadDungeon;

    @Unique
    private List<Holder<Biome>> mmsDungeons$netherBiomes;

    @Shadow
    public abstract Holder<Biome> getNoiseBiome(Climate.TargetPoint target);

    @WrapMethod(method = "createResolver")
    private BiomeResolver mmsDungeons$spreadResolver(Climate.Sampler sampler, Operation<BiomeResolver> original) {
        return mmsDungeons$wrap(sampler, original.call(sampler));
    }

    @WrapMethod(method = "createResolverForChunk")
    private BiomeResolver mmsDungeons$spreadChunk(Climate.Sampler sampler, int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ, Operation<BiomeResolver> original) {
        return mmsDungeons$wrap(sampler, original.call(sampler, minX, minY, minZ, sizeX, sizeY, sizeZ));
    }

    @Unique
    private BiomeResolver mmsDungeons$wrap(Climate.Sampler sampler, BiomeResolver resolver) {
        if (!mmsDungeons$isDungeon()) {
            return resolver;
        }
        return (x, y, z) -> mmsDungeons$spread(resolver.getNoiseBiome(x, y, z), x, y, z, sampler);
    }

    @Unique
    private Holder<Biome> mmsDungeons$spread(Holder<Biome> biome, int x, int y, int z, Climate.Sampler sampler) {
        if (QuartPos.toBlock(y) > DungeonDimension.PERICULUM_TOP && QuartPos.toBlock(y) < DungeonDimension.SANCTUARY_BOTTOM && biome.is(NETHER)) {
            return mmsDungeons$capNether(biome, x, y, z, sampler);
        }
        if (QuartPos.toBlock(y) > DungeonDimension.PERICULUM_TOP
            || biome.is(DUNGEON_CAVES)
            || biome.unwrapKey().map(k -> k.identifier().getNamespace().equals("alexscaves")).orElse(true)) {
            return biome;
        }
        if (getNoiseBiome(sampler.sample(x + REACH, y, z)) != biome
            || getNoiseBiome(sampler.sample(x - REACH, y, z)) != biome
            || getNoiseBiome(sampler.sample(x, y, z + REACH)) != biome
            || getNoiseBiome(sampler.sample(x, y, z - REACH)) != biome) {
            return mmsDungeons$capNether(biome, x, y, z, sampler);
        }
        Holder<Biome> swap = getNoiseBiome(sampler.sample(x + mmsDungeons$far(), y, z + mmsDungeons$far()));
        return mmsDungeons$capNether(swap == biome || swap.is(DUNGEON_CAVES) ? biome : swap, x, y, z, sampler);
    }

    // The swap sample's offset, moved by every Deep Region rebuild.
    @Unique
    private static int mmsDungeons$far() {
        return FAR + DungeonGeneration.current() * 977;
    }

    @Unique
    private Holder<Biome> mmsDungeons$capNether(Holder<Biome> biome, int x, int y, int z, Climate.Sampler sampler) {
        if (!biome.is(NETHER)) {
            return biome;
        }
        Climate.TargetPoint target = sampler.sample(x, y, z);
        OptionalLong cell = NetherPatches.cell(x, z, mmsDungeons$vanillaCount());
        if (cell.isPresent()) {
            Holder<Biome> paired = mmsDungeons$pair(cell.getAsLong(), target.continentalness() >= PAIR_SPLIT);
            if (paired != null) {
                return paired;
            }
        }
        Holder<Biome> standIn = getNoiseBiome(new Climate.TargetPoint(
            target.temperature(),
            target.humidity(),
            target.continentalness(),
            STAND_IN_EROSION,
            target.depth(),
            target.weirdness()));
        return standIn.is(NETHER) || standIn.is(DUNGEON_CAVES) ? biome : standIn;
    }

    @Unique
    private Holder<Biome> mmsDungeons$pair(long cell, boolean second) {
        List<Holder<Biome>> vanilla = mmsDungeons$netherBiomes().stream().filter(h -> mmsDungeons$isVanilla(h)).toList();
        List<Holder<Biome>> descent = mmsDungeons$netherBiomes().stream().filter(h -> !mmsDungeons$isVanilla(h)).toList();
        if (vanilla.isEmpty()) {
            return null;
        }
        int first = NetherPatches.leader(cell, vanilla.size());
        if (!second) {
            return vanilla.get(first);
        }
        long other = cell >>> 24;
        if (!descent.isEmpty()) {
            return descent.get((int) Math.floorMod(other, (long) descent.size()));
        }
        if (vanilla.size() < 2) {
            return vanilla.get(first);
        }
        return vanilla.get((first + 1 + (int) Math.floorMod(other, (long) vanilla.size() - 1)) % vanilla.size());
    }

    @Unique
    private int mmsDungeons$vanillaCount() {
        return Math.max(1, (int) mmsDungeons$netherBiomes().stream().filter(h -> mmsDungeons$isVanilla(h)).count());
    }

    @Unique
    private static boolean mmsDungeons$isVanilla(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.identifier().getNamespace().equals("minecraft")).orElse(false);
    }

    @Unique
    private List<Holder<Biome>> mmsDungeons$netherBiomes() {
        if (mmsDungeons$netherBiomes == null) {
            mmsDungeons$netherBiomes = ((MultiNoiseBiomeSource) (Object) this).possibleBiomes().stream()
                .filter(h -> h.is(NETHER))
                .sorted(Comparator.comparing(h -> h.unwrapKey().map(k -> k.identifier().toString()).orElse("")))
                .toList();
        }
        return mmsDungeons$netherBiomes;
    }

    @Unique
    private boolean mmsDungeons$isDungeon() {
        if (mmsDungeons$spreadDungeon == null) {
            mmsDungeons$spreadDungeon = ((MultiNoiseBiomeSource) (Object) this).possibleBiomes().stream().anyMatch(h -> h.is(DUNGEON_CAVES));
        }
        return mmsDungeons$spreadDungeon;
    }
}
