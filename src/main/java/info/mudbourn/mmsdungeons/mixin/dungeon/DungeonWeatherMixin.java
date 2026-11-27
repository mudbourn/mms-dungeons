package info.mudbourn.mmsdungeons.mixin.dungeon;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Keeps the dungeon clear of rain and thunder.
@Mixin(Level.class)
public abstract class DungeonWeatherMixin {

    @ModifyReturnValue(method = "getRainLevel", at = @At("RETURN"))
    private float mmsDungeons$noRain(float level) {
        return ((Level) (Object) this).dimension().equals(DungeonDimension.LEVEL) ? 0.0F : level;
    }

    @ModifyReturnValue(method = "getThunderLevel", at = @At("RETURN"))
    private float mmsDungeons$noThunder(float level) {
        return ((Level) (Object) this).dimension().equals(DungeonDimension.LEVEL) ? 0.0F : level;
    }
}
