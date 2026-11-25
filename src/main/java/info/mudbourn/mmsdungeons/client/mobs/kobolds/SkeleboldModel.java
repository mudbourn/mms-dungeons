package info.mudbourn.mmsdungeons.client.mobs.kobolds;

import net.minecraft.world.item.BowItem;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SkeleboldModel<S extends KoboldRenderState> extends KoboldModel<S> {

    public SkeleboldModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = KoboldModel.createMesh();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "right_arm",
            CubeListBuilder.create().texOffs(46, 16).addBox(-2.01f, -0.85f, -1.0f, 2.0f, 10.0f, 2.0f),
            PartPose.offset(-3.0f, 5.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "left_arm",
            CubeListBuilder.create().texOffs(33, 16).addBox(0.01f, -0.85f, -1.0f, 2.0f, 10.0f, 2.0f),
            PartPose.offset(3.0f, 5.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(13, 31).addBox(-1.0f, -0.01f, -1.0f, 2.0f, 10.0f, 2.0f),
            PartPose.offset(-1.5f, 14.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(0, 31).addBox(-1.0f, -0.01f, -1.0f, 2.0f, 10.0f, 2.0f),
            PartPose.offset(1.5f, 14.0f, 0.0f)
        );
        root.getChild("body").addOrReplaceChild(
            "tail",
            CubeListBuilder.create().texOffs(24, 15).addBox(-0.5f, 6.0f, -4.0f, 1.0f, 12.0f, 1.0f),
            PartPose.rotation(0.6109f, 0.0f, 0.0f)
        );
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(S state) {
        this.resetPose();
        this.walk(state, true);
        this.poseHands(state);
        if (state.swingAnimation > 0.0f) {
            this.mainArm(state).xRot -= this.lift(state.swingAnimation);
        }
    }

    private void poseHands(S state) {
        ItemStack held = state.getMainHandItemStack();
        if (held.isEmpty()) {
            return;
        }
        float side = state.isLeftHanded() ? -1.0f : 1.0f;
        if (state.isAggressive) {
            if (held.getItem() instanceof BowItem) {
                this.aim(this.mainArm(state), this.offArm(state), side, false);
            } else {
                this.mainArm(state).xRot = -2.0944f;
                this.mainArm(state).yRot = 0.1745f * side;
            }
        } else if (held.getItem() instanceof CrossbowItem && (state.isCharging || CrossbowItem.isCharged(held))) {
            this.aim(this.mainArm(state), this.offArm(state), side, state.isCharging);
        }
    }
}
