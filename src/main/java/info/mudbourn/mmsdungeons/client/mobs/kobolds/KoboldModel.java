package info.mudbourn.mmsdungeons.client.mobs.kobolds;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class KoboldModel<S extends KoboldRenderState> extends HumanoidModel<S> {

    protected static final float DEGREES = (float) (Math.PI / 180.0);

    public KoboldModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(createMesh(), 64, 64);
    }

    public static MeshDefinition createMesh() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-3.5f, -7.0f, -3.5f, 7.0f, 7.0f, 7.0f)
                .texOffs(22, 0)
                .addBox(-2.5f, -3.0f, -6.5f, 5.0f, 3.0f, 3.0f)
                .texOffs(1, 3)
                .addBox(-0.5f, -3.85f, -5.5f, 1.0f, 1.0f, 2.0f),
            PartPose.offset(0.0f, 4.0f, -0.5f)
        );
        head.addOrReplaceChild(
            "hat",
            CubeListBuilder.create()
                .texOffs(0, 50)
                .addBox(-3.5f, -12.0f, -3.0f, 7.0f, 7.0f, 7.0f, new CubeDeformation(0.5f))
                .texOffs(17, 52)
                .addBox(-6.0f, -10.2f, -5.5f, 12.0f, 0.0f, 12.0f),
            PartPose.offset(0.0f, 4.0f, -0.5f)
        );
        head.addOrReplaceChild(
            "left_horn",
            CubeListBuilder.create().texOffs(36, 0).addBox(-0.5f, -4.0f, -1.0f, 2.0f, 5.0f, 2.0f),
            PartPose.offsetAndRotation(1.5f, -7.0f, 2.0f, -0.6109f, 0.3054f, 0.1745f)
        );
        head.addOrReplaceChild(
            "right_horn",
            CubeListBuilder.create().texOffs(45, 0).addBox(-1.5f, -4.0f, -1.0f, 2.0f, 5.0f, 2.0f),
            PartPose.offsetAndRotation(-1.5f, -7.0f, 2.0f, -0.6109f, -0.3054f, -0.1745f)
        );
        root.addOrReplaceChild(
            "right_arm",
            CubeListBuilder.create().texOffs(46, 16).addBox(-3.0f, -0.85f, -1.5f, 3.0f, 10.0f, 3.0f),
            PartPose.offset(-3.0f, 5.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "left_arm",
            CubeListBuilder.create().texOffs(33, 16).addBox(0.0f, -0.85f, -1.5f, 3.0f, 10.0f, 3.0f),
            PartPose.offset(3.0f, 5.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(13, 31).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 10.0f, 3.0f),
            PartPose.offset(-1.5f, 14.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(0, 31).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 10.0f, 3.0f),
            PartPose.offset(1.5f, 14.0f, 0.0f)
        );
        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(3, 15).addBox(-3.0f, 0.0f, -2.0f, 6.0f, 10.0f, 4.0f),
            PartPose.offset(0.0f, 4.0f, 0.0f)
        );
        body.addOrReplaceChild(
            "tail",
            CubeListBuilder.create().texOffs(24, 15).addBox(-1.0f, 6.0f, -4.0f, 2.0f, 12.0f, 2.0f),
            PartPose.rotation(0.6109f, 0.0f, 0.0f)
        );
        return mesh;
    }

    @Override
    public void setupAnim(S state) {
        this.resetPose();
        this.walk(state, true);
        this.poseMainHand(state);
        this.poseOffHand(state);
        this.swing(state);
    }

    // Walking legs, head turn, idle arm sway and the seated pose.
    protected void walk(S state, boolean swingArms) {
        float position = state.walkAnimationPos * 0.6662f;
        float speed = state.walkAnimationSpeed;
        if (swingArms) {
            this.rightArm.xRot = Mth.cos(position + (float) Math.PI) * speed;
            this.leftArm.xRot = Mth.cos(position) * speed;
        }
        this.rightLeg.xRot = Mth.cos(position) * 1.4f * speed;
        this.leftLeg.xRot = Mth.cos(position + (float) Math.PI) * 1.4f * speed;
        this.head.yRot = state.yRot * DEGREES;
        this.head.xRot = state.xRot * DEGREES;
        float sway = Mth.cos(state.ageInTicks * 0.04f) * 0.04f + 0.04f;
        this.rightArm.zRot += sway;
        this.leftArm.zRot -= sway;
        this.sit(state);
    }

    protected void sit(S state) {
        if (state.isPassenger) {
            this.rightLeg.xRot = -1.5708f;
            this.leftLeg.xRot = -1.5708f;
            this.rightLeg.yRot = 0.2618f;
            this.leftLeg.yRot = -0.2618f;
        }
    }

    private void poseMainHand(S state) {
        ItemStack held = state.getMainHandItemStack();
        if (held.isEmpty()) {
            return;
        }
        ModelPart main = this.mainArm(state);
        ModelPart off = this.offArm(state);
        float side = state.isLeftHanded() ? -1.0f : 1.0f;
        if (held.getItem() instanceof CrossbowItem || held.is(Items.BOW)) {
            if (state.isAggressive) {
                this.aim(main, off, side, state.isCharging);
            }
        } else if (held.is(Items.EMERALD) || held.is(Items.POTION)) {
            main.xRot = -0.8727f;
            main.yRot = 0.0873f;
            this.head.xRot = 0.1745f;
        } else if (state.isAggressive) {
            main.xRot = -2.0944f;
            main.yRot = 0.1745f * side;
        }
    }

    protected void aim(ModelPart main, ModelPart off, float side, boolean charging) {
        if (charging) {
            main.xRot = -0.6981f;
            main.yRot = -0.3491f * side;
            off.xRot = -1.1345f;
            off.yRot = 0.5672f * side;
        } else {
            main.xRot = -1.4399f;
            main.yRot = -0.2618f * side;
            off.xRot = -1.3963f;
            off.yRot = 0.3054f * side;
        }
    }

    private void poseOffHand(S state) {
        ItemStack held = state.getOffHandItemStack();
        if (held.isEmpty()) {
            return;
        }
        ModelPart main = this.mainArm(state);
        ModelPart off = this.offArm(state);
        float side = state.isLeftHanded() ? -1.0f : 1.0f;
        if (held.has(DataComponents.BLOCKS_ATTACKS)) {
            if (state.isBlocking) {
                off.xRot = -0.6981f;
                off.yRot = 0.2618f * side;
            }
        } else if (held.is(Items.TRIDENT)) {
            if (state.isAggressive) {
                off.xRot = 2.8798f;
                main.xRot = 0.0f;
            }
        } else if (held.is(Items.EMERALD) || held.is(Items.POTION)) {
            off.xRot = -0.8727f;
            off.yRot = 0.0873f;
            this.head.xRot = 0.1745f;
        }
    }

    private void swing(S state) {
        float time = state.swingAnimation;
        if (time <= 0.0f) {
            return;
        }
        if (state.isAggressive) {
            ModelPart main = this.mainArm(state);
            main.xRot -= this.lift(time);
        } else if (state.getMainHandItemStack().is(Items.EMERALD)) {
            this.body.yRot = Mth.sin(Mth.sqrt(time) * (float) (-Math.PI * 2)) * -0.2f;
            this.leftArm.yRot += this.body.yRot;
            this.rightArm.yRot += this.body.yRot;
            this.rightArm.xRot += this.body.yRot;
            this.leftArm.xRot = this.rightArm.xRot - this.toss(time);
            this.leftArm.yRot += this.body.yRot * 2.0f;
            this.leftArm.zRot += Mth.sin(time * (float) Math.PI) * -0.4f;
        } else if (state.getOffHandItemStack().is(Items.EMERALD)) {
            this.body.yRot = Mth.sin(Mth.sqrt(time) * (float) (Math.PI * 2)) * 0.2f;
            this.rightArm.yRot += this.body.yRot;
            this.leftArm.yRot += this.body.yRot;
            this.leftArm.xRot += this.body.yRot;
            this.rightArm.xRot -= this.toss(time);
            this.rightArm.yRot += this.body.yRot * 2.0f;
            this.rightArm.zRot += Mth.sin(time * (float) Math.PI) * -0.4f;
        }
    }

    // How far a striking arm is raised at a point of the swing.
    protected float lift(float time) {
        return this.curve(time) / 1.2f - 1.0f;
    }

    protected float toss(float time) {
        float reach = Mth.sin(time * (float) Math.PI) * -(this.head.xRot - 0.7f) * 0.75f;
        return this.curve(time) * 1.2f + reach;
    }

    private float curve(float time) {
        float eased = 1.0f - time;
        eased *= eased;
        eased *= eased;
        return Mth.sin((1.0f - eased) * (float) Math.PI);
    }

    protected ModelPart mainArm(S state) {
        return state.isLeftHanded() ? this.leftArm : this.rightArm;
    }

    protected ModelPart offArm(S state) {
        return state.isLeftHanded() ? this.rightArm : this.leftArm;
    }

    @Override
    public void translateToHand(HumanoidRenderState state, HumanoidArm arm, PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.getArm(arm).translateAndRotate(poseStack);
        poseStack.translate(arm == HumanoidArm.LEFT ? 0.045 : -0.045, 0.096, 0.0);
        poseStack.scale(0.75f, 0.75f, 0.75f);
    }
}
