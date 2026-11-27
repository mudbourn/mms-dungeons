package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import com.mojang.blaze3d.vertex.PoseStack;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Golem;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityLayer;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

// Draws a Golem with its grass or snow laid over the stone, and its eyes lit only while it is awake.
public class GolemRenderer extends FangsGeoRenderer<Golem> {

    public GolemRenderer(
        EntityRendererProvider.Context context,
        Identifier definition,
        String skin,
        String eyes,
        String overlay
    ) {
        super(context, definition, golem -> Look.skin(skin, golem.isDormant() ? null : eyes));
        Identifier cover = Identifier.fromNamespaceAndPath("fangs_n_claws", "textures/entity/" + overlay + ".png");
        this.addLayer(new CoverLayer(this, cover));
    }

    // Draws the cover texture over the stone texture.
    private static final class CoverLayer extends GeoEntityLayer {

        private final Identifier texture;

        private CoverLayer(
            RenderLayerParent<GeoEntityRenderState, EntityModel<GeoEntityRenderState>> parent,
            Identifier texture
        ) {
            super(parent);
            this.texture = texture;
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
                RenderTypes.entityCutout(this.texture),
                (pose, consumer) -> state.model.draw(pose, consumer, light, overlay, -1, state.poses)
            );
        }
    }
}
