package info.mudbourn.mmsdungeons.mixin.features;

import info.mudbourn.mmsdungeons.client.PartyOutlines;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Makes party teammates glow on the client so their outline shows through walls.
@Mixin(Entity.class)
public abstract class EntityOutlineMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void mmsDungeons$partyGlow(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!cir.getReturnValue() && self.level().isClientSide() && PartyOutlines.outlined(self)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void mmsDungeons$partyColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (cir.getReturnValue() == 0xFFFFFF && self.level().isClientSide() && PartyOutlines.outlined(self)) {
            cir.setReturnValue(PartyOutlines.COLOR);
        }
    }
}
