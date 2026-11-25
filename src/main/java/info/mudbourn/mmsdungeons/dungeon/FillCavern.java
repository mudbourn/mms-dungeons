package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

// Signed distance in blocks to the fill cavern around a position: negative inside, a huge value where there is none.
public record FillCavern(DensityFunction base) implements DensityFunction {

    public static final MapCodec<FillCavern> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.CODEC.fieldOf("base").forGetter(FillCavern::base)
    ).apply(instance, FillCavern::new));

    private static final String SEED_STREAM = "fill_cavern";

    public static void register() {
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, MmsDungeons.id("fill_cavern"), CODEC);
    }

    @Override
    public DensitySampler compileSampler(CompileContext context) {
        long seed = context.createRandom(MmsDungeons.id(SEED_STREAM)).nextLong();
        FillCaverns.bind(base.compileSampler(context), seed);
        return new Sampler();
    }

    @Override
    public DensityFunction rewriteChildren(DfRewriteRule rule) {
        DensityFunction rewritten = rule.rewrite(base);
        return rewritten == base ? this : new FillCavern(rewritten);
    }

    @Override
    public Interval range() {
        return Interval.INFINITE;
    }

    @Override
    public int domainAxes() {
        return ALL_AXES;
    }

    @Override
    public MapCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    private record Sampler() implements DensitySampler {

        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer buffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, buffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int x, int y, int z) {
            return FillCaverns.distance(x, y, z);
        }
    }
}
