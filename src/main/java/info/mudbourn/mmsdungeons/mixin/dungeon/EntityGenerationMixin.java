package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.DeepEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Marks dungeon entities saved before generations were tracked as belonging to the first generation.
@Mixin(Entity.class)
public abstract class EntityGenerationMixin {

    @Inject(method = "load", at = @At("TAIL"))
    private void mmsDungeons$stampLegacy(ValueInput input, CallbackInfo ci) {
        DeepEntities.stampLegacy((Entity) (Object) this);
    }
}
