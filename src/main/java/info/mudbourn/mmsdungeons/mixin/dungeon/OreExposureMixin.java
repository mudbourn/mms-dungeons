package info.mudbourn.mmsdungeons.mixin.dungeon;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.dungeon.OreExposure;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import org.spongepowered.asm.mixin.Mixin;

// Marks ore veins that generate in the dungeon.
@Mixin(OreFeature.class)
public abstract class OreExposureMixin {

    @WrapMethod(method = "place")
    private boolean mmsDungeons$markDungeon(
        WorldGenLevel level,
        ChunkGenerator generator,
        RandomSource random,
        BlockPos origin,
        Operation<Boolean> original
    ) {
        OreExposure.inDungeon(level.getLevel().dimension().equals(DungeonDimension.LEVEL));
        try {
            return original.call(level, generator, random, origin);
        } finally {
            OreExposure.inDungeon(false);
        }
    }
}
