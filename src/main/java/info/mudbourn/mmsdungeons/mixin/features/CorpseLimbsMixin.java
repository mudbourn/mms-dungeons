package info.mudbourn.mmsdungeons.mixin.features;

import info.mudbourn.mmsdungeons.client.CorpsePose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Sprawls a corpse marker's limbs the way Absolute Revive poses a downed player.
@Mixin(PlayerModel.class)
public abstract class CorpseLimbsMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void mmsDungeons$corpseLimbs(AvatarRenderState state, CallbackInfo ci) {
        if (!CorpsePose.isCorpse(state)) {
            return;
        }
        PlayerModel model = (PlayerModel) (Object) this;
        model.resetPose();
        model.rightArm.zRot = (float) Math.toRadians(20.0);
        model.leftArm.zRot = (float) Math.toRadians(-40.0);
        model.rightLeg.zRot = (float) Math.toRadians(10.0);
        model.leftLeg.zRot = (float) Math.toRadians(-15.0);
    }
}
