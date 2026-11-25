package info.mudbourn.mmsdungeons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import info.mudbourn.mmsdungeons.dungeon.DungeonPortalBlock;
import info.mudbourn.mmsdungeons.dungeon.DungeonPortalBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

// Draws the dungeon portal as a thin sheet of the end portal's starfield, as thick as a nether portal.
public class DungeonPortalRenderer extends AbstractEndPortalRenderer<DungeonPortalBlockEntity, DungeonPortalRenderer.State> {

    private static final float THICKNESS = 0.25f;

    public static class State extends EndPortalRenderState {
        public Direction.Axis axis = Direction.Axis.Z;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DungeonPortalBlockEntity portal, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        super.extractRenderState(portal, state, partialTick, camera, overlay);
        state.axis = portal.getBlockState().getOptionalValue(DungeonPortalBlock.AXIS).orElse(Direction.Axis.Z);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.scale(state.axis == Direction.Axis.X ? THICKNESS : 1.0f, state.axis == Direction.Axis.Y ? THICKNESS : 1.0f, state.axis == Direction.Axis.Z ? THICKNESS : 1.0f);
        pose.translate(-0.5f, -0.5f, -0.5f);
        submitCube(state.facesToShow, RenderTypes.endPortal(), pose, collector);
        pose.popPose();
    }
}
