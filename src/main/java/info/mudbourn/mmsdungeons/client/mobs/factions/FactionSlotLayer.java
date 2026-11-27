package info.mudbourn.mmsdungeons.client.mobs.factions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

// Draws the items in a faction member's Combat Amenities belt and back slots: the belt item at the right hip, the back item across the back.
public class FactionSlotLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final float BELT_SCALE = 0.4f;
    private static final float BACK_SCALE = 0.7f;

    public FactionSlotLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        FactionRenderState faction = (FactionRenderState) state;
        if (!faction.beltItem.isEmpty()) {
            poseStack.pushPose();
            this.getParentModel().body.translateAndRotate(poseStack);
            poseStack.translate(-0.3f, 0.7f, 0.0f);
            poseStack.scale(BELT_SCALE, -BELT_SCALE, -BELT_SCALE);
            faction.beltItem.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
        if (!faction.backItem.isEmpty()) {
            poseStack.pushPose();
            this.getParentModel().body.translateAndRotate(poseStack);
            poseStack.translate(0.0f, 0.35f, 0.2f);
            poseStack.rotateDegrees(Axis.ZP, 45.0f);
            poseStack.scale(BACK_SCALE, -BACK_SCALE, BACK_SCALE);
            faction.backItem.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
