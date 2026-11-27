package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Mimic;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityLayer;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

// Draws the Mimic as a chest with its toothy skin laid over it, turned to face out of its chest.
public class MimicRenderer extends FangsGeoRenderer<Mimic> {

    private static final Identifier SKIN =
        Identifier.fromNamespaceAndPath("fangs_n_claws", "textures/entity/mimic.png");

    public MimicRenderer(EntityRendererProvider.Context context, Identifier definition) {
        super(context, definition, mimic -> Look.plain("mimic", "mimic_chest"));
        this.addLayer(new SkinLayer(this));
    }

    @Override
    protected void setupRotations(GeoEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        poseStack.rotateDegrees(Axis.YP, 180.0f);
    }

    // Draws the skin over the chest texture.
    private static final class SkinLayer extends GeoEntityLayer {

        private SkinLayer(RenderLayerParent<GeoEntityRenderState, EntityModel<GeoEntityRenderState>> parent) {
            super(parent);
        }

        @Override
        protected void submitGeo(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            GeoEntityRenderState state
        ) {
            int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0f);
            collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(SKIN),
                (pose, consumer) -> state.model.draw(pose, consumer, light, overlay, -1, state.poses)
            );
        }
    }
}
