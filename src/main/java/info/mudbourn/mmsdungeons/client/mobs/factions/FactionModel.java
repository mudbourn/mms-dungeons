package info.mudbourn.mmsdungeons.client.mobs.factions;

import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionMember;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

// The player model with casting and skill wind-up poses, hanging legs in flight, and plain swings and a roll pose for when Better Combat or Combat Roll has no animation to play.
public class FactionModel extends PlayerModel {

    public FactionModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        // One model draws every member of a faction, so parts an origin's fur hid on the last member are shown again first.
        this.root().getAllParts().forEach(part -> part.visible = true);
        super.setupAnim(state);
        if (!(state instanceof FactionRenderState faction)) {
            return;
        }
        if (faction.flying) {
            float drift = Mth.sin(state.ageInTicks * 0.1f) * 0.08f;
            this.rightLeg.xRot = 0.15f + drift;
            this.leftLeg.xRot = 0.05f - drift;
        }
        boolean offHand = (faction.action & FactionMember.OFF_HAND_FLAG) != 0;
        byte action = (byte) (faction.action & ~FactionMember.OFF_HAND_FLAG);
        ModelPart arm = offHand ? this.leftArm : this.rightArm;
        float mirror = offHand ? -1.0f : 1.0f;
        float swing = state.swingAnimation;
        float ease = Mth.sin(swing * Mth.PI);
        switch (action) {
            case FactionMember.ACTION_CAST -> {
                float sway = Mth.sin(state.ageInTicks * 0.3f) * 0.1f;
                this.rightArm.xRot = -1.9f + sway;
                this.leftArm.xRot = -1.9f - sway;
                this.rightArm.zRot = -0.3f;
                this.leftArm.zRot = 0.3f;
            }
            case FactionMember.ACTION_CHARGE -> {
                this.rightArm.xRot = -2.6f;
                this.rightArm.zRot = -0.2f;
                this.body.yRot = 0.3f;
            }
            case FactionMember.ACTION_SLASH, FactionMember.ACTION_SLASH_LEFT -> {
                if (swing > 0.0f) {
                    float direction = action == FactionMember.ACTION_SLASH ? 1.0f : -1.0f;
                    arm.xRot = -1.45f;
                    arm.yRot = Mth.lerp(swing, 1.1f, -1.1f) * direction * mirror;
                    this.body.yRot = Mth.lerp(swing, 0.4f, -0.4f) * direction * mirror;
                }
            }
            case FactionMember.ACTION_SLAM -> {
                if (swing > 0.0f) {
                    float arc = -2.9f + swing * 3.5f;
                    this.rightArm.xRot = arc;
                    this.leftArm.xRot = arc;
                    this.rightArm.yRot = -0.3f;
                    this.leftArm.yRot = 0.3f;
                    this.body.xRot = ease * 0.4f;
                }
            }
            case FactionMember.ACTION_ROLL -> {
                if (faction.rollAnimated) {
                    return;
                }
                this.rightArm.xRot = -2.2f;
                this.leftArm.xRot = -2.2f;
                this.rightLeg.xRot = -1.3f;
                this.leftLeg.xRot = -1.3f;
            }
            default -> {
            }
        }
    }
}
