package info.mudbourn.mmsdungeons.client.mobs.swing;

import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.util.RenderUtil;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import info.mudbourn.mmsdungeons.mixin.combat.IllagerModelAccessor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.model.player.PlayerModel;

// Poses humanoid and illager models, armor included, from a mob's Better Combat animation, as Player Animation Library poses the player model.
public final class SwingBones {

    private static final PlayerAnimBone HEAD = new PlayerAnimBone("head");
    private static final PlayerAnimBone TORSO = new PlayerAnimBone("torso");
    private static final PlayerAnimBone RIGHT_ARM = new PlayerAnimBone("right_arm");
    private static final PlayerAnimBone LEFT_ARM = new PlayerAnimBone("left_arm");
    private static final PlayerAnimBone RIGHT_LEG = new PlayerAnimBone("right_leg");
    private static final PlayerAnimBone LEFT_LEG = new PlayerAnimBone("left_leg");

    private SwingBones() {
    }

    // Runs right after a model's own setupAnim; the player model itself is left to Player Animation Library.
    public static void apply(Model<?> model, Object state) {
        if (model instanceof PlayerModel
            || !(state instanceof IAvatarAnimationState animated)
            || !(animated.playerAnimLib$getAnimManager() instanceof MobAnimations animations)
            || !animations.isActive()) {
            return;
        }
        if (model instanceof HumanoidModel<?> humanoid) {
            pose(animations, humanoid.head, HEAD);
            pose(animations, humanoid.body, TORSO);
            pose(animations, humanoid.rightArm, RIGHT_ARM);
            pose(animations, humanoid.leftArm, LEFT_ARM);
            pose(animations, humanoid.rightLeg, RIGHT_LEG);
            pose(animations, humanoid.leftLeg, LEFT_LEG);
        } else if (model instanceof IllagerModel<?> illager) {
            IllagerModelAccessor parts = (IllagerModelAccessor) illager;
            parts.mmsDungeons$crossedArms().visible = false;
            parts.mmsDungeons$rightArm().visible = true;
            parts.mmsDungeons$leftArm().visible = true;
            pose(animations, parts.mmsDungeons$head(), HEAD);
            pose(animations, parts.mmsDungeons$rightArm(), RIGHT_ARM);
            pose(animations, parts.mmsDungeons$leftArm(), LEFT_ARM);
            pose(animations, parts.mmsDungeons$rightLeg(), RIGHT_LEG);
            pose(animations, parts.mmsDungeons$leftLeg(), LEFT_LEG);
        }
    }

    private static void pose(MobAnimations animations, ModelPart part, PlayerAnimBone bone) {
        RenderUtil.copyVanillaPart(part, bone);
        animations.updatePart(part, bone);
    }
}
