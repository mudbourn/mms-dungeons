package info.mudbourn.mmsdungeons.mobs.orpiment.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentBlocks;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.AmberDruseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;

// Scatters amber druse over a patch, a random number of crystals in each.
public record AmberDruseFeature(IntProvider count) implements Feature {

    public static final MapCodec<AmberDruseFeature> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(IntProviders.NON_NEGATIVE_CODEC.fieldOf("count").forGetter(AmberDruseFeature::count))
            .apply(instance, AmberDruseFeature::new)
    );

    @Override
    public MapCodec<AmberDruseFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int attempts = this.count.sample(random);
        int placed = 0;
        for (int i = 0; i < attempts; i++) {
            BlockPos pos = origin.offset(random.nextInt(8) - random.nextInt(8), 0, random.nextInt(8) - random.nextInt(8));
            BlockState existing = level.getBlockState(pos);
            if (!existing.isAir() && existing.getFluidState().getType() != Fluids.WATER && !existing.canBeReplaced()) {
                continue;
            }
            BlockState druse = OrpimentBlocks.AMBER_DRUSE.defaultBlockState().setValue(AmberDruseBlock.AMOUNT, random.nextInt(4) + 1);
            if (druse.canSurvive(level, pos)) {
                level.setBlock(pos, druse, 2);
                placed++;
            }
        }
        return placed > 0;
    }
}
