package info.mudbourn.mmsdungeons.mixin.combat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import info.mudbourn.mmsdungeons.client.mobs.swing.SwingBones;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelFeatureRenderer.class)
abstract class ModelFeatureSwingMixin {

    @WrapOperation(method = "prepareModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V"))
    private void mmsDungeons$poseSwing(Model<Object> model, Object state, Operation<Void> original) {
        original.call(model, state);
        SwingBones.apply(model, state);
    }
}
