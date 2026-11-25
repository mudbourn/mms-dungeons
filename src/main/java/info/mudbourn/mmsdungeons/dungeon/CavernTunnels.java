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

// Signed distance in blocks to the winding tunnels that branch out of each Aerial Hell cavern.
public record CavernTunnels(int count, float start, float length, float radius, int baseY, float rise) implements DensityFunction {

    public static final MapCodec<CavernTunnels> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.fieldOf("count").forGetter(CavernTunnels::count),
        Codec.FLOAT.fieldOf("start").forGetter(CavernTunnels::start),
        Codec.FLOAT.fieldOf("length").forGetter(CavernTunnels::length),
        Codec.FLOAT.fieldOf("radius").forGetter(CavernTunnels::radius),
        Codec.INT.fieldOf("base_y").forGetter(CavernTunnels::baseY),
        Codec.FLOAT.fieldOf("rise").forGetter(CavernTunnels::rise)
    ).apply(instance, CavernTunnels::new));

    private static final float FAR = 64.0F;
    private static final double TAU = Math.PI * 2.0;

    public static void register() {
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, MmsDungeons.id("cavern_tunnels"), CODEC);
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

    private record Sampler(CavernTunnels tunnels) implements DensitySampler {

        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer buffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, buffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int x, int y, int z) {
            CavernTunnels t = tunnels;
            if (Math.abs(y - t.baseY) > t.rise + t.radius * 2.0F + 4.0F) {
                return FAR;
            }
            long center = CavernGrid.nearestCenter(x, z);
            if (center == CavernGrid.NONE) {
                return FAR;
            }
            double dx = x - CavernGrid.centerX(center);
            double dz = z - CavernGrid.centerZ(center);
            double r = Math.sqrt(dx * dx + dz * dz);
            if (r < t.start || r > t.length + t.radius * 2.0F) {
                return FAR;
            }
            long hash = CavernGrid.hash(center);
            double angle = Math.atan2(dz, dx);
            double best = FAR;
            for (int i = 0; i < t.count; i++) {
                long h = hash * (i * 2L + 1L);
                double heading = TAU * i / t.count + phase(h, 0) * 0.6;
                double wander = 0.35 * Math.sin(r / (18.0 + 10.0 * phase(h, 1)) + phase(h, 2) * TAU);
                double off = wrap(angle - heading - wander);
                double side = r * off;
                double centerY = t.baseY + t.rise * Math.sin(r / (22.0 + 8.0 * phase(h, 3)) + phase(h, 4) * TAU);
                double up = y - centerY;
                double widen = t.radius * (1.0 + 0.35 * Math.sin(r / 9.0 + phase(h, 5) * TAU));
                if (r > t.length) {
                    widen *= Math.max(0.0, 1.0 - (r - t.length) / (t.radius * 2.0));
                }
                double distance = Math.sqrt(side * side + up * up * 1.6) - widen;
                best = Math.min(best, distance);
            }
            return (float) best;
        }

        // A number from 0 to 1 taken from one slice of the hash.
        private static double phase(long hash, int slot) {
            return ((hash >>> (slot * 10)) & 1023L) / 1023.0;
        }

        private static double wrap(double angle) {
            return angle - TAU * Math.floor((angle + Math.PI) / TAU);
        }
    }
}
