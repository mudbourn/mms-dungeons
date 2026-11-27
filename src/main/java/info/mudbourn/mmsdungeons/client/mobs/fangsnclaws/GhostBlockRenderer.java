package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import com.mojang.blaze3d.vertex.PoseStack;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.block.GhostBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// Draws the block a Ghost Block has taken the look of.
public class GhostBlockRenderer implements BlockEntityRenderer<GhostBlockEntity, GhostBlockRenderer.State> {

    private final BlockModelResolver resolver;

    public GhostBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.resolver = context.blockModelResolver();
    }

    public static final class State extends BlockEntityRenderState {
        public final BlockModelRenderState model = new BlockModelRenderState();
        public boolean mimicking;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        GhostBlockEntity entity,
        State state,
        float partialTick,
        Vec3 cameraPos,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPos, crumblingOverlay);
        BlockState mimic = entity.getMimic();
        state.mimicking = mimic != null;
        if (mimic != null) {
            this.resolver.update(state.model, mimic, BlockDisplayContext.create());
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.mimicking) {
            state.model.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
    }
}
