package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.VelocityArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

// Draws the velocity arrow as an arrow in the source mod's texture.
public class VelocityArrowRenderer extends ArrowRenderer<VelocityArrow, ArrowRenderState> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("fangs_n_claws", "textures/entity/velocity_arrow.png");

    public VelocityArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) {
        return TEXTURE;
    }
}
