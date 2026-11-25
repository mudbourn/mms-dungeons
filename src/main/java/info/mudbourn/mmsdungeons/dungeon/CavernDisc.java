package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.Codec;
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

// Signed distance in blocks to a cavern's disc: negative inside, zero at the rim.
public record CavernDisc(float radius, int bottom, int top, float rounding) implements DensityFunction {

    public static final MapCodec<CavernDisc> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.FLOAT.fieldOf("radius").forGetter(CavernDisc::radius),
        Codec.INT.fieldOf("bottom").forGetter(CavernDisc::bottom),
        Codec.INT.fieldOf("top").forGetter(CavernDisc::top),
        Codec.FLOAT.optionalFieldOf("rounding", 0.0F).forGetter(CavernDisc::rounding)
    ).apply(instance, CavernDisc::new));

    // Distance past which the exact value no longer matters to any user of the disc.
    private static final float FAR = 8.0F;

    public static void register() {
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, MmsDungeons.id("cavern_disc"), CODEC);
    }

    @Override
    public DensitySampler compileSampler(CompileContext context) {
        return new Sampler(this);
    }

    @Override
    public DensityFunction rewriteChildren(DfRewriteRule rule) {
        return this;
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

    private record Sampler(CavernDisc disc) implements DensitySampler {

        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer buffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, buffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int x, int y, int z) {
            float vertical = Math.max(disc.bottom - y, y - disc.top);
            if (vertical >= FAR) {
                return vertical;
            }
            float horizontal = (float) CavernGrid.distanceToCenter(x, z) - disc.radius;
            float r = disc.rounding;
            if (r <= 0.0F) {
                return Math.max(vertical, horizontal);
            }
            // Rounded edge where the wall meets the floor and roof.
            float h = horizontal + r;
            float v = vertical + r;
            float outside = (float) Math.sqrt(Math.max(h, 0.0F) * Math.max(h, 0.0F) + Math.max(v, 0.0F) * Math.max(v, 0.0F));
            return outside + Math.min(Math.max(h, v), 0.0F) - r;
        }
    }
}
