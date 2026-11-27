package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.ThrownTomahawk;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

// Draws a thrown tomahawk turning end over end along its flight.
public class ThrownTomahawkRenderer
    extends EntityRenderer<ThrownTomahawk, ThrownTomahawkRenderer.State> {

    private static final float QUARTER_TURN = 90.0f;
    private static final float SPIN_DEGREES_PER_TICK = 40.0f;

    private final ItemModelResolver itemModelResolver;

    public ThrownTomahawkRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ThrownTomahawk entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot(partialTick);
        state.xRot = entity.getXRot(partialTick);
        state.stuck = entity.isStuck();
        this.itemModelResolver.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.GROUND, entity);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, state.yRot - QUARTER_TURN);
        poseStack.rotateDegrees(Axis.ZP, state.stuck ? state.xRot : state.ageInTicks * SPIN_DEGREES_PER_TICK);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {

        public final ItemStackRenderState item = new ItemStackRenderState();
        public float yRot;
        public float xRot;
        public boolean stuck;
    }
}
