package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

// Draws nothing, for entities that are only a hit box or an effect.
public class NothingRenderer<T extends Entity> extends EntityRenderer<T, EntityRenderState> {

    public NothingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
