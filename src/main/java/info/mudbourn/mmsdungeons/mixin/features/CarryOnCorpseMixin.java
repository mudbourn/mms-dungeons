package info.mudbourn.mmsdungeons.mixin.features;

import info.mudbourn.mmsdungeons.corpse.Corpses;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Stops Carry On from picking up a corpse for anyone but its owner.
@Pseudo
@Mixin(targets = "tschipp.carryon.common.carry.PickupHandler", remap = false)
public abstract class CarryOnCorpseMixin {

    @Inject(method = "tryPickupEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private static void mmsDungeons$ownerOnly(ServerPlayer player, Entity entity, Function<Entity, Boolean> check, CallbackInfoReturnable<Boolean> cir) {
        if (!Corpses.mayHandle(player, entity)) {
            cir.setReturnValue(false);
        }
    }
}
