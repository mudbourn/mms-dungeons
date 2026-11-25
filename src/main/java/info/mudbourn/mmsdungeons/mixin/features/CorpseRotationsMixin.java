package info.mudbourn.mmsdungeons.mixin.features;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsdungeons.client.CorpsePose;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Lays a corpse marker flat on the ground in the Absolute Revive downed pose.
@Mixin(AvatarRenderer.class)
public abstract class CorpseRotationsMixin {

    // Half the torso's depth in blocks at the avatar model scale, so the back rests on the floor.
    private static final double TORSO_HALF_DEPTH = 2.0 / 16.0 * 0.9375;

    @Inject(method = "setupRotations", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$corpseRotations(AvatarRenderState state, PoseStack pose, float bodyRot, float scale, CallbackInfo ci) {
        if (CorpsePose.isCorpse(state)) {
            pose.rotateDegrees(Axis.YP, 90.0F - CorpsePose.facing(state));
            pose.rotateDegrees(Axis.ZP, 90.0F);
            pose.rotateDegrees(Axis.YP, 270.0F);
            pose.translate(0.0, -0.9, -TORSO_HALF_DEPTH);
            ci.cancel();
        }
    }
}
