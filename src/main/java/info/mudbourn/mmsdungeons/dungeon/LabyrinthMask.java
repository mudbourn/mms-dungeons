package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
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

// Solid density inside labyrinth cells of the Deep and Sanctuary regions, fading near caves cells, and a far negative value everywhere else.
public final class LabyrinthMask implements DensityFunction {

    public static final LabyrinthMask INSTANCE = new LabyrinthMask();
    public static final MapCodec<LabyrinthMask> CODEC = MapCodec.unit(INSTANCE);

    private static final float SOLID = 1.0F;
    private static final float OPEN = -1000.0F;

    private LabyrinthMask() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, MmsDungeons.id("labyrinth_mask"), CODEC);
    }

    @Override
    public DensitySampler compileSampler(CompileContext context) {
        return Sampler.INSTANCE;
    }

    @Override
    public DensityFunction rewriteChildren(DfRewriteRule rule) {
        return this;
    }

    @Override
    public Interval range() {
        return Interval.of(OPEN, SOLID);
    }

    @Override
    public int domainAxes() {
        return ALL_AXES;
    }

    @Override
    public MapCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    private enum Sampler implements DensitySampler {
        INSTANCE;

        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer buffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, buffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int x, int y, int z) {
            return LabyrinthCells.rockDensity(x, y, z);
        }
    }
}
