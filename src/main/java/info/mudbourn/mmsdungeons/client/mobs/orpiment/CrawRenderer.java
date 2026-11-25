package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import com.mojang.blaze3d.vertex.PoseStack;
import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.Craw;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class CrawRenderer extends MobRenderer<Craw, CrawRenderState, CrawModel> {

    private static final Identifier[] TEXTURES = {
        texture("craw"),
        texture("crimson_craw"),
        texture("icy_craw"),
        texture("creamy_craw"),
        texture("dark_craw"),
        texture("flora_craw"),
        texture("albino_craw")
    };

    public CrawRenderer(EntityRendererProvider.Context context) {
        super(context, new CrawModel(context.bakeLayer(OrpimentClient.CRAW)), 0.5f);
        this.addLayer(new SaddleLayer(this, new CrawModel(context.bakeLayer(OrpimentClient.CRAW_SADDLE))));
    }

    private static Identifier texture(String name) {
        return MmsDungeons.id("textures/entity/orpiment/" + name + ".png");
    }

    @Override
    public CrawRenderState createRenderState() {
        return new CrawRenderState();
    }

    @Override
    public void extractRenderState(Craw craw, CrawRenderState state, float partialTick) {
        super.extractRenderState(craw, state, partialTick);
        state.variant = craw.getVariant();
        state.flap = Mth.lerp(partialTick, craw.oFlap, craw.flap);
        state.flapSpeed = Mth.lerp(partialTick, craw.oFlapSpeed, craw.flapSpeed);
        state.saddled = craw.isSaddled();
    }

    @Override
    public Identifier getTextureLocation(CrawRenderState state) {
        return TEXTURES[state.variant];
    }

    // Draws the saddle over the body of a saddled Craw.
    private static class SaddleLayer extends RenderLayer<CrawRenderState, CrawModel> {

        private static final Identifier SADDLE = texture("craw_saddle");

        private final CrawModel model;

        SaddleLayer(RenderLayerParent<CrawRenderState, CrawModel> parent, CrawModel model) {
            super(parent);
            this.model = model;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, CrawRenderState state, float yRot, float xRot) {
            if (state.saddled) {
                renderColoredCutoutModel(this.model, SADDLE, poseStack, collector, light, state, -1, 0);
            }
        }
    }
}
