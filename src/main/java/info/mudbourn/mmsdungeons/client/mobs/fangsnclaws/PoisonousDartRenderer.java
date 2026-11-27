package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.PoisonousDart;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

// Draws the poisonous dart as an arrow in the source mod's dart texture.
public class PoisonousDartRenderer extends ArrowRenderer<PoisonousDart, ArrowRenderState> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("fangs_n_claws", "textures/entity/poisonous_dart.png");

    public PoisonousDartRenderer(EntityRendererProvider.Context context) {
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
