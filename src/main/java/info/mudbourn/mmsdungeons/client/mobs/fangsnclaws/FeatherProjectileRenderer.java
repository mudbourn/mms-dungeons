package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.FeatherProjectile;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

// Draws the shrike's feather as an arrow in the source mod's feather texture.
public class FeatherProjectileRenderer extends ArrowRenderer<FeatherProjectile, ArrowRenderState> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("fangs_n_claws", "textures/entity/feather_projectile.png");

    public FeatherProjectileRenderer(EntityRendererProvider.Context context) {
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
