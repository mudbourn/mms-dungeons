package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.Wisp;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class WispRenderer extends MobRenderer<Wisp, LivingEntityRenderState, WispModel> {

    private static final Identifier TEXTURE = MmsDungeons.id("textures/entity/orpiment/wisp_v2.png");
    private static final Identifier CORE = MmsDungeons.id("textures/entity/orpiment/wisp_core.png");

    public WispRenderer(EntityRendererProvider.Context context) {
        super(context, new WispModel(context.bakeLayer(OrpimentClient.WISP_FLIPPED)), 0.5f);
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this,
                state -> CORE,
                (state, age) -> 1.0f,
                new WispModel(context.bakeLayer(OrpimentClient.WISP)),
                RenderTypes::eyes,
                false
            )
        );
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
