package info.mudbourn.mmsdungeons.client.mobs.kobolds;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;

public class ZomboldModel<S extends KoboldRenderState> extends KoboldModel<S> {

    private static final float REACH = -1.5708f;

    public ZomboldModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(S state) {
        this.resetPose();
        float position = state.walkAnimationPos * 0.6662f;
        float speed = state.walkAnimationSpeed;
        this.rightLeg.xRot = Mth.cos(position) * 1.4f * speed;
        this.leftLeg.xRot = Mth.cos(position + (float) Math.PI) * 1.4f * speed;
        this.head.yRot = state.yRot * DEGREES;
        this.head.xRot = state.xRot * DEGREES;
        float sway = Mth.cos(state.ageInTicks * 0.08f) * 0.08f;
        this.rightArm.xRot = REACH + sway;
        this.leftArm.xRot = REACH - sway;
        this.rightArm.zRot = sway + 0.05f;
        this.leftArm.zRot = -(sway + 0.05f);
        this.sit(state);
        if (state.isAggressive) {
            this.raiseArms(state);
        }
        if (state.swingAnimation > 0.0f) {
            float lift = this.lift(state.swingAnimation);
            this.leftArm.xRot -= lift;
            this.rightArm.xRot -= lift;
        }
    }

    private void raiseArms(S state) {
        if (state.getMainHandItemStack().is(Items.TRIDENT)) {
            ModelPart throwing = this.mainArm(state);
            ModelPart braced = this.offArm(state);
            braced.xRot = REACH;
            throwing.xRot = 2.8798f;
            throwing.zRot = state.isLeftHanded() ? 0.1309f : -0.1309f;
        } else {
            this.rightArm.xRot = -2.0944f;
            this.leftArm.xRot = -2.0944f;
        }
    }
}
