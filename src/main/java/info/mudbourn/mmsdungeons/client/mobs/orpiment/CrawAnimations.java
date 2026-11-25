package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.AnimationChannel.Interpolations;
import net.minecraft.client.animation.AnimationChannel.Targets;
import net.minecraft.client.animation.AnimationDefinition.Builder;

public final class CrawAnimations {
    public static final AnimationDefinition perk = Builder.withLength(2.5938F)
        .addAnimation(
            "head",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.2591F, KeyframeAnimations.degreeVec(0.0F, 27.5F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.0363F, KeyframeAnimations.degreeVec(0.0F, -42.5F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.8135F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(2.5907F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "head",
            new AnimationChannel(
                Targets.POSITION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(2.3306F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(2.3316F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(2.5897F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(2.5907F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "bottom_beak",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.5181F, KeyframeAnimations.degreeVec(12.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.7772F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.2953F, KeyframeAnimations.degreeVec(17.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.5544F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(2.5907F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "neck",
            new AnimationChannel(
                Targets.POSITION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(2.3306F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(2.3316F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(2.5897F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(2.5907F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .build();
    public static final AnimationDefinition walking = Builder.withLength(1.5F)
        .looping()
        .addAnimation(
            "head",
            new AnimationChannel(
                Targets.POSITION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, 0.0F, 2.0F), Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.0F), Interpolations.LINEAR),
                    new Keyframe(0.6667F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.8333F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.0F), Interpolations.LINEAR),
                    new Keyframe(1.1667F, KeyframeAnimations.posVec(0.0F, 0.0F, 2.0F), Interpolations.LINEAR),
                    new Keyframe(1.3333F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.0F), Interpolations.LINEAR),
                    new Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "left_leg",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.6667F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "left_leg",
            new AnimationChannel(
                Targets.POSITION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, 2.0F, 1.0F), Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.6667F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "right_leg",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.8333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "right_leg",
            new AnimationChannel(
                Targets.POSITION,
                new Keyframe[]{
                    new Keyframe(0.8333F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.1667F, KeyframeAnimations.posVec(0.0F, 2.0F, 1.0F), Interpolations.LINEAR),
                    new Keyframe(1.3333F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "neck",
            new AnimationChannel(
                Targets.POSITION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, 0.0F, 2.0F), Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.0F), Interpolations.LINEAR),
                    new Keyframe(0.6667F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.8333F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.0F), Interpolations.LINEAR),
                    new Keyframe(1.1667F, KeyframeAnimations.posVec(0.0F, 0.0F, 2.0F), Interpolations.LINEAR),
                    new Keyframe(1.3333F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.0F), Interpolations.LINEAR),
                    new Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .build();
}
