package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public class CrawModel extends EntityModel<CrawRenderState> {

    private static final float WING_SWING = (float) Math.PI * 0.15f;
    private static final float MIN_FLAP_SPEED = 0.15f;

    private final ModelPart head;
    private final ModelPart headTuft;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final KeyframeAnimation walking;

    public CrawModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.headTuft = this.head.getChild("head_tuft");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.walking = CrawAnimations.walking.bake(root);
    }

    @Override
    public void setupAnim(CrawRenderState state) {
        super.setupAnim(state);
        this.walking.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 4.0f, 4.0f);
        this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
        this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
        float angle = Mth.cos(state.flap) * WING_SWING * Math.max(state.flapSpeed, MIN_FLAP_SPEED);
        this.rightWing.zRot = angle;
        this.leftWing.zRot = -angle;
        this.headTuft.visible = !state.isBaby;
    }
}
