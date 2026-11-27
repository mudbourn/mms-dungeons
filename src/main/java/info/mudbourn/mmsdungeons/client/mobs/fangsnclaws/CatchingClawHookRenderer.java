package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.CatchingClawHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// Draws a Catching Claw hook as a flat sprite with a line back to the thrower.
public class CatchingClawHookRenderer extends EntityRenderer<CatchingClawHook, CatchingClawHookRenderer.HookState> {

    private static final Identifier HOOK =
        Identifier.fromNamespaceAndPath("fangs_n_claws", "textures/entity/catching_claw_hook.png");
    private static final Identifier NETHERITE_HOOK =
        Identifier.fromNamespaceAndPath("fangs_n_claws", "textures/entity/catching_claw_hook_netherite.png");
    private static final float SPRITE_SCALE = 0.3f;
    private static final int LINE_COLOR = 0xFF000000;
    private static final double HAND_DROP = 0.4;

    public static class HookState extends EntityRenderState {
        public boolean netherite;
        public Vec3 toOwner = Vec3.ZERO;
        public boolean hasOwner;
    }

    public CatchingClawHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public HookState createRenderState() {
        return new HookState();
    }

    @Override
    public void extractRenderState(CatchingClawHook hook, HookState state, float partialTick) {
        super.extractRenderState(hook, state, partialTick);
        state.netherite = hook.isNetherite();
        Entity owner = hook.getOwner();
        state.hasOwner = owner != null;
        if (owner != null) {
            Vec3 hand = owner.getEyePosition(partialTick).subtract(0.0, HAND_DROP, 0.0);
            state.toOwner = hand.subtract(hook.getPosition(partialTick));
        }
    }

    @Override
    public void submit(HookState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(SPRITE_SCALE, SPRITE_SCALE, SPRITE_SCALE);
        poseStack.rotate(camera.orientation);
        int light = state.lightCoords;
        collector.submitCustomGeometry(
            poseStack,
            RenderTypes.entityCutout(state.netherite ? NETHERITE_HOOK : HOOK),
            (pose, consumer) -> {
                vertex(consumer, pose, light, -0.5f, -0.5f, 0.0f, 1.0f);
                vertex(consumer, pose, light, 0.5f, -0.5f, 1.0f, 1.0f);
                vertex(consumer, pose, light, 0.5f, 0.5f, 1.0f, 0.0f);
                vertex(consumer, pose, light, -0.5f, 0.5f, 0.0f, 0.0f);
            }
        );
        poseStack.popPose();
        if (state.hasOwner) {
            float x = (float) state.toOwner.x;
            float y = (float) state.toOwner.y;
            float z = (float) state.toOwner.z;
            float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
            Vec3 direction = state.toOwner.normalize();
            poseStack.pushPose();
            collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, consumer) -> {
                consumer.addVertex(pose, 0.0f, 0.0f, 0.0f)
                    .setColor(LINE_COLOR)
                    .setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z)
                    .setLineWidth(width);
                consumer.addVertex(pose, x, y, z)
                    .setColor(LINE_COLOR)
                    .setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z)
                    .setLineWidth(width);
            });
            poseStack.popPose();
        }
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
