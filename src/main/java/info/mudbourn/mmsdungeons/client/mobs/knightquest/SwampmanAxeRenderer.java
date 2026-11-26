package info.mudbourn.mmsdungeons.client.mobs.knightquest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.SwampmanAxe;
import info.mudbourn.mmsrendercommon.client.geo.GeoAssets;
import info.mudbourn.mmsrendercommon.client.geo.GeoModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

// Draws a thrown Swampman axe as its geo model.
public class SwampmanAxeRenderer extends EntityRenderer<SwampmanAxe, ArrowRenderState> {

    private static final Identifier GEO = Identifier.fromNamespaceAndPath("knightquest", "geo/swampman_axe.geo.json");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("knightquest", "textures/entity/swampman.png");

    public SwampmanAxeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }

    @Override
    public void extractRenderState(SwampmanAxe entity, ArrowRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.xRot = entity.getXRot(partialTick);
        state.yRot = entity.getYRot(partialTick);
        state.shake = entity.shakeTime - partialTick;
    }

    @Override
    public void submit(ArrowRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        GeoModel model = GeoAssets.model(GEO);
        if (model != null) {
            poseStack.pushPose();
            poseStack.rotateDegrees(Axis.YP, state.yRot + 90.0F);
            poseStack.rotateDegrees(Axis.ZP, state.xRot);
            if (state.shake > 0.0F) {
                poseStack.rotateDegrees(Axis.ZP, -Mth.sin(state.shake * 3.0F) * state.shake);
            }
            int light = state.lightCoords;
            collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(TEXTURE),
                (pose, consumer) -> model.draw(pose, consumer, light, OverlayTexture.NO_OVERLAY, -1)
            );
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }
}
