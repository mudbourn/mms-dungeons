package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

// The translucent outer shell of a cube mob.
public class CubeOuterLayer<S extends SlimeRenderState> extends RenderLayer<S, CubeModel<S>> {

    private static final float ALPHA = 0.8f;

    private final CubeModel<S> model;
    private final Identifier texture;

    public CubeOuterLayer(RenderLayerParent<S, CubeModel<S>> parent, EntityModelSet models, ModelLayerLocation layer, Identifier texture) {
        super(parent);
        this.model = new CubeModel<>(models.bakeLayer(layer));
        this.texture = texture;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
        boolean glowing = state.appearsGlowing() && state.isInvisible;
        if (state.isInvisible && !glowing) {
            return;
        }
        collector.order(1).submitModel(
            this.model,
            state,
            poseStack,
            glowing ? RenderTypes.outline(this.texture) : RenderTypes.entityTranslucent(this.texture),
            light,
            LivingEntityRenderer.getOverlayCoords(state, 0.0f),
            ARGB.white(ALPHA),
            null,
            state.outlineColor
        );
    }
}
