package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.AbstractCubeMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;

// Draws a cube mob as a core inside a translucent shell, with glowing eyes.
public abstract class CubeRenderer<T extends AbstractCubeMob, S extends SlimeRenderState> extends AbstractCubeMobRenderer<T, S, CubeModel<S>> {

    private final Identifier texture;

    protected CubeRenderer(
        EntityRendererProvider.Context context,
        ModelLayerLocation inner,
        ModelLayerLocation outer,
        Identifier texture,
        Identifier eyes
    ) {
        super(context, new CubeModel<>(context.bakeLayer(inner)));
        this.texture = texture;
        this.addLayer(new CubeOuterLayer<>(this, context.getModelSet(), outer, texture));
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this,
                state -> eyes,
                (state, age) -> 1.0f,
                new CubeModel<>(context.bakeLayer(outer)),
                RenderTypes::eyes,
                true
            )
        );
    }

    @Override
    protected void scale(S state, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        this.downscaleSlightly(poseStack);
        super.scale(state, poseStack);
    }

    @Override
    public Identifier getTextureLocation(S state) {
        return this.texture;
    }
}
