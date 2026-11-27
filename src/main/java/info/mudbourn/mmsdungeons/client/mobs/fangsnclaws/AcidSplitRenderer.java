package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.AcidSplit;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

// Draws a lob of Purple Worm acid as a glowing green blob that always faces the camera.
public class AcidSplitRenderer extends EntityRenderer<AcidSplit, AcidSplitRenderer.SplitState> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("fangs_n_claws", "textures/entity/acid_split.png");
    private static final float BLOB_SCALE = 0.6f;
    private static final float U_MIN = 6.0f / 64.0f;
    private static final float U_MAX = 18.0f / 64.0f;
    private static final float V_MIN = 6.0f / 64.0f;
    private static final float V_MAX = 12.0f / 64.0f;

    public static class SplitState extends EntityRenderState {
        public float scale = 1.0f;
    }

    public AcidSplitRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SplitState createRenderState() {
        return new SplitState();
    }

    @Override
    public void extractRenderState(AcidSplit split, SplitState state, float partialTick) {
        super.extractRenderState(split, state, partialTick);
        state.scale = split.getScale();
    }

    @Override
    public void submit(SplitState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.25f, 0.0f);
        poseStack.scale(BLOB_SCALE * state.scale, BLOB_SCALE * state.scale, BLOB_SCALE * state.scale);
        poseStack.rotate(camera.orientation);
        int light = state.lightCoords;
        collector.submitCustomGeometry(
            poseStack,
            RenderTypes.entityCutout(TEXTURE),
            (pose, consumer) -> {
                vertex(consumer, pose, light, -0.5f, -0.5f, U_MIN, V_MAX);
                vertex(consumer, pose, light, 0.5f, -0.5f, U_MAX, V_MAX);
                vertex(consumer, pose, light, 0.5f, 0.5f, U_MAX, V_MIN);
                vertex(consumer, pose, light, -0.5f, 0.5f, U_MIN, V_MIN);
            }
        );
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void vertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        int light,
        float x,
        float y,
        float u,
        float v
    ) {
        consumer.addVertex(pose, x, y, 0.0f)
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
