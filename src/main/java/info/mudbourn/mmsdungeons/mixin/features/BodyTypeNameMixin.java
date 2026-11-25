package info.mudbourn.mmsdungeons.mixin.features;

import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Mannequin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Names corpses and combat-logged bodies by their description instead of "Mannequin", for WTHIT and the like.
@Mixin(Entity.class)
abstract class BodyTypeNameMixin {

    private static final Set<String> BODY_KINDS = Set.of("Corpse", "Combat Logged Player");

    @Inject(method = "getTypeName", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$bodyTypeName(CallbackInfoReturnable<Component> cir) {
        if ((Object) this instanceof Mannequin mannequin) {
            Component description = ((MannequinAccessor) mannequin).mmsDungeons$getDescription();
            if (description != null && BODY_KINDS.contains(description.getString())) {
                cir.setReturnValue(description);
            }
        }
    }
}
