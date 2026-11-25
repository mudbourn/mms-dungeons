package info.mudbourn.mmsdungeons.client.mobs.kobolds;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class RascalModel<S extends KoboldRenderState> extends KoboldModel<S> {

    public RascalModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = KoboldModel.createMesh();
        mesh.getRoot().addOrReplaceChild(
            "bag",
            CubeListBuilder.create().texOffs(26, 30).addBox(-1.0f, 0.0f, 2.0f, 5.0f, 7.0f, 4.0f, new CubeDeformation(0.001f)),
            PartPose.offset(0.0f, 4.0f, 0.0f)
        );
        mesh.getRoot().addOrReplaceChild(
            "left_arm",
            CubeListBuilder.create().texOffs(33, 7).addBox(-5.0f, -0.85f, -1.5f, 8.0f, 5.0f, 3.0f),
            PartPose.offset(3.0f, 6.0f, 0.0f)
        );
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(S state) {
        this.resetPose();
        this.walk(state, false);
        this.rightArm.xRot = Mth.cos(state.walkAnimationPos * 0.6662f + (float) Math.PI) * state.walkAnimationSpeed;
        this.leftArm.xRot = -0.7854f;
        if (!state.getMainHandItemStack().isEmpty() && state.isAggressive) {
            this.rightArm.xRot = -2.0944f;
            this.rightArm.yRot = 0.1745f;
        }
        float time = state.swingAnimation;
        if (time > 0.0f) {
            if (state.isAggressive) {
                this.rightArm.xRot -= this.lift(time);
            } else {
                this.body.yRot = Mth.sin(Mth.sqrt(time) * (float) (Math.PI * 2)) * 0.2f;
                this.rightArm.yRot += this.body.yRot;
                this.leftArm.yRot += this.body.yRot;
                this.leftArm.xRot += this.body.yRot;
                this.rightArm.xRot -= this.toss(time);
                this.rightArm.yRot += this.body.yRot * 2.0f;
                this.rightArm.zRot += Mth.sin(time * (float) Math.PI) * -0.4f;
            }
        }
    }

    @Override
    public void translateToHand(HumanoidRenderState state, HumanoidArm arm, PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.rightArm.translateAndRotate(poseStack);
        poseStack.translate(-0.045, 0.096, 0.0);
        poseStack.scale(0.75f, 0.75f, 0.75f);
    }
}
