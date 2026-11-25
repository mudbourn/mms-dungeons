package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

public class WraithModel extends EntityModel<WraithRenderState> {

    private static final float HOVER_SPEED = 0.8f;
    private static final float HOVER_HEIGHT = 2.0f;

    private final ModelPart whole;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final KeyframeAnimation summon;
    private final KeyframeAnimation idle;
    private final KeyframeAnimation fire;
    private final KeyframeAnimation move;

    public WraithModel(ModelPart root) {
        super(root);
        this.whole = root.getChild("whole");
        this.head = this.whole.getChild("head");
        this.body = this.whole.getChild("body");
        this.leftArm = this.whole.getChild("left_arm");
        this.rightArm = this.whole.getChild("right_arm");
        this.summon = WraithAnimations.summon.bake(this.whole);
        this.idle = WraithAnimations.hover.bake(this.whole);
        this.fire = WraithAnimations.throw_attack.bake(this.whole);
        this.move = WraithAnimations.movement.bake(this.whole);
    }

    @Override
    public void setupAnim(WraithRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
        this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
        boolean animated = state.summon.isStarted() || state.idle.isStarted() || state.fire.isStarted() || state.move.isStarted();
        if (animated) {
            this.leftArm.xRot = -1.4835f;
            this.rightArm.xRot = -1.4835f;
            this.body.xRot = 0.0f;
        }
        this.move.apply(state.move, state.ageInTicks);
        this.idle.apply(state.idle, state.ageInTicks);
        this.fire.apply(state.fire, state.ageInTicks);
        this.summon.apply(state.summon, state.ageInTicks);
        this.whole.y += this.hoverOffset(state);
    }

    private float hoverOffset(WraithRenderState state) {
        if (!state.idle.isStarted() || state.move.isStarted()) {
            return 0.0f;
        }
        float length = WraithAnimations.hover.lengthInSeconds();
        float progress = state.idle.getTimeInMillis(state.ageInTicks) / 1000.0f * HOVER_SPEED;
        float t = (progress > length ? progress % length : progress) / length;
        return (float) Math.sin(t * Math.PI * 2.0) * HOVER_HEIGHT;
    }
}
