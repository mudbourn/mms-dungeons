package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsGear;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.ThrownPitchfork;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;

// Draws a thrown pitchfork flying point first along its motion, like a thrown trident.
public class ThrownPitchforkRenderer
    extends EntityRenderer<ThrownPitchfork, ThrownPitchforkRenderer.State> {

    private static final float QUARTER_TURN = 90.0f;
    private static final double CULLING_MARGIN = 1.5;

    private final PitchforkModel model;

    public ThrownPitchforkRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new PitchforkModel(context.getModelSet());
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ThrownPitchfork entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot(partialTick);
        state.xRot = entity.getXRot(partialTick);
        state.hellfire = entity.getItem().is(FangsNClawsGear.HELLFIRE_PITCHFORK);
        state.foil = entity.getItem().hasFoil();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, state.yRot - QUARTER_TURN);
        poseStack.rotateDegrees(Axis.ZP, state.xRot + QUARTER_TURN);
        this.model.submit(
            poseStack,
            collector,
            state.hellfire,
            state.foil,
            (long) state.ageInTicks,
            state.lightCoords,
            state.outlineColor
        );
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    protected AABB getBoundingBoxForCulling(ThrownPitchfork entity, float partialTick) {
        return super.getBoundingBoxForCulling(entity, partialTick).inflate(CULLING_MARGIN);
    }

    public static class State extends EntityRenderState {

        public float yRot;
        public float xRot;
        public boolean hellfire;
        public boolean foil;
    }
}
