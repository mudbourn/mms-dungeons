package info.mudbourn.mmsdungeons.mixin.features;

import info.mudbourn.mmsdungeons.locator.LocatorItem;
import info.mudbourn.mmsdungeons.locator.Locators;
import info.mudbourn.mmsdungeons.net.CycleLocatorPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Turns a left click with a switched-on locator in the main hand into a focus cycle instead of an attack.
@Mixin(Minecraft.class)
public abstract class LocatorAttackMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$cycleLocator(CallbackInfoReturnable<Boolean> cir) {
        if (this.mmsDungeons$holdingOn()) {
            ClientPlayNetworking.send(new CycleLocatorPayload());
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$noMining(boolean leftClick, CallbackInfo ci) {
        if (this.mmsDungeons$holdingOn()) {
            ci.cancel();
        }
    }

    private boolean mmsDungeons$holdingOn() {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (minecraft.player == null || minecraft.gui.screen() != null) {
            return false;
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        return stack.is(Locators.LOCATOR) && LocatorItem.isOn(stack);
    }
}
