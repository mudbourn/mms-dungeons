package info.mudbourn.mmsdungeons.mixin.dungeon;

import com.github.alexmodguy.alexscaves.server.misc.VoronoiGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Reads the jitter table Alex's Caves uses to place cave biome cells.
@Mixin(VoronoiGenerator.class)
public interface VoronoiGeneratorAccessor {

    @Accessor("RandVecs2D")
    static double[] mmsDungeons$randVecs2D() {
        throw new AssertionError();
    }
}
