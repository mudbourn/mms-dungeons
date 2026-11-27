package info.mudbourn.mmsdungeons.mixin.combat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import info.mudbourn.mmsdungeons.client.mobs.swing.MobAnimations;
import info.mudbourn.mmsdungeons.client.mobs.swing.SwingBones;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
abstract class LivingRendererSwingMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void mmsDungeons$attachSwing(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        MobAnimations.attachIfSwinging(entity, state, partialTick);
    }

    @WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Ljava/lang/Object;)V"))
    private void mmsDungeons$poseSwing(EntityModel<?> model, Object state, Operation<Void> original) {
        original.call(model, state);
        SwingBones.apply(model, state);
    }
}
