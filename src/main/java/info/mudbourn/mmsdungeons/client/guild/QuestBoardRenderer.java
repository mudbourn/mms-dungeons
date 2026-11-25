package info.mudbourn.mmsdungeons.client.guild;

import com.mojang.blaze3d.vertex.PoseStack;
import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.guild.QuestBoardBlock;
import info.mudbourn.mmsdungeons.guild.QuestBoardBlockEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

// Draws the quest board mesh facing the way the block was placed.
public class QuestBoardRenderer implements BlockEntityRenderer<QuestBoardBlockEntity, QuestBoardRenderer.State> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MmsDungeons.id("quest_board"), "main");

    private static final Identifier TEXTURE = MmsDungeons.id("textures/entity/quest_board.png");

    private final Model<EntityRenderState> model;
    private final RenderType renderType;

    public QuestBoardRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new QuestBoardModel(context.bakeLayer(LAYER));
        this.renderType = RenderTypes.entityCutout(TEXTURE);
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(QuestBoardBlockEntity entity, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPos, crumblingOverlay);
        state.facing = entity.getBlockState().getValue(QuestBoardBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(new Matrix4f().rotationX((float) Math.toRadians(180.0)));
        float rotation = switch (state.facing) {
            case NORTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(rotation)));
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, this.renderType, (pose, buffer) -> {
            PoseStack local = new PoseStack();
            local.mulPose(pose.pose());
            this.model.renderToBuffer(local, buffer, light, OverlayTexture.NO_OVERLAY, -1);
        });
        poseStack.popPose();
    }
}
