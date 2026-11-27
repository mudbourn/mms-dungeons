package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.TridentModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

// Draws a pitchfork on the trident model with the source mod's skin and animated flame glow.
public final class PitchforkModel {

    private static final String NAMESPACE = "fangs_n_claws";
    private static final int FLAME_FRAMES = 4;
    private static final int FRAME_TICKS = 10;
    private static final Identifier FIRE_SKIN = skin("fire_pitchfork");
    private static final Identifier HELLFIRE_SKIN = skin("hellfire_pitchfork");
    private static final Identifier[] FLAMES = flames();

    private final TridentModel model;

    public PitchforkModel(EntityModelSet models) {
        this.model = new TridentModel(models.bakeLayer(ModelLayers.TRIDENT));
    }

    public TridentModel model() {
        return this.model;
    }

    public void submit(
        PoseStack poseStack,
        SubmitNodeCollector collector,
        boolean hellfire,
        boolean foil,
        long ticks,
        int light,
        int outline
    ) {
        Identifier skin = hellfire ? HELLFIRE_SKIN : FIRE_SKIN;
        if (foil) {
            collector.submitModel(
                this.model,
                Unit.INSTANCE,
                poseStack,
                RenderTypes.entitySolidGlint(skin),
                light,
                OverlayTexture.NO_OVERLAY,
                outline
            );
        } else {
            collector.submitModel(this.model, Unit.INSTANCE, poseStack, skin, light, OverlayTexture.NO_OVERLAY, outline);
        }
        collector.submitModel(
            this.model,
            Unit.INSTANCE,
            poseStack,
            RenderTypes.entityTranslucentEmissive(FLAMES[(int) (ticks / FRAME_TICKS % FLAME_FRAMES)]),
            light,
            OverlayTexture.NO_OVERLAY,
            EntityRenderState.NO_OUTLINE
        );
    }

    private static Identifier skin(String name) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, "textures/entity/fire_pitchfork/" + name + ".png");
    }

    private static Identifier[] flames() {
        Identifier[] flames = new Identifier[FLAME_FRAMES];
        for (int frame = 0; frame < FLAME_FRAMES; frame++) {
            flames[frame] = skin("fire_pitchfork_fire_" + frame);
        }
        return flames;
    }
}
