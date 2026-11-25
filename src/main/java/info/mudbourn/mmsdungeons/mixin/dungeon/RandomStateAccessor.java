package info.mudbourn.mmsdungeons.mixin.dungeon;

import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Reads the noise router a random state was built with.
@Mixin(RandomState.class)
public interface RandomStateAccessor {

    @Accessor("router")
    NoiseRouter mmsDungeons$router();
}
