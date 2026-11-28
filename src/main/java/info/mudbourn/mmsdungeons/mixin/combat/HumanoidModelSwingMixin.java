package info.mudbourn.mmsdungeons.mixin.combat;

import info.mudbourn.mmsdungeons.client.mobs.swing.SwingBones;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Poses a humanoid model's swing inside its own setupAnim, ahead of GeckoLib copying the parts onto armor such as Fantasy Armor.
@Mixin(value = HumanoidModel.class, priority = 900)
abstract class HumanoidModelSwingMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void mmsDungeons$poseSwing(HumanoidRenderState state, CallbackInfo ci) {
        SwingBones.apply((HumanoidModel<?>) (Object) this, state);
    }
}
