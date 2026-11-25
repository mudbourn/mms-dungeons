package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.AnimationChannel.Interpolations;
import net.minecraft.client.animation.AnimationChannel.Targets;
import net.minecraft.client.animation.AnimationDefinition.Builder;

public final class WraithAnimations {
    public static final AnimationDefinition summon = Builder.withLength(1.6667F)
        .addAnimation(
            "left_arm",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(-65.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.degreeVec(-95.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 15.0F), Interpolations.LINEAR),
                    new Keyframe(0.6667F, KeyframeAnimations.degreeVec(-85.0047F, -0.2178F, 17.4905F), Interpolations.LINEAR),
                    new Keyframe(0.8333F, KeyframeAnimations.degreeVec(-80.0234F, -0.6514F, 19.9527F), Interpolations.LINEAR),
                    new Keyframe(1.0F, KeyframeAnimations.degreeVec(-85.0047F, -0.2178F, 17.4905F), Interpolations.LINEAR),
                    new Keyframe(1.1667F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 15.0F), Interpolations.LINEAR),
                    new Keyframe(1.3333F, KeyframeAnimations.degreeVec(-95.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.5F, KeyframeAnimations.degreeVec(-65.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.6667F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "right_arm",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(-22.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(-67.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.degreeVec(-95.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.degreeVec(-97.5F, 0.0F, -10.0F), Interpolations.LINEAR),
                    new Keyframe(0.6667F, KeyframeAnimations.degreeVec(-104.9905F, -0.2178F, -14.9952F), Interpolations.LINEAR),
                    new Keyframe(0.8333F, KeyframeAnimations.degreeVec(-107.4718F, -0.6512F, -17.4574F), Interpolations.LINEAR),
                    new Keyframe(1.0F, KeyframeAnimations.degreeVec(-104.9905F, -0.2178F, -14.9952F), Interpolations.LINEAR),
                    new Keyframe(1.1667F, KeyframeAnimations.degreeVec(-97.5F, 0.0F, -10.0F), Interpolations.LINEAR),
                    new Keyframe(1.3333F, KeyframeAnimations.degreeVec(-95.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.5F, KeyframeAnimations.degreeVec(-67.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.6667F, KeyframeAnimations.degreeVec(-22.5F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .build();
    public static final AnimationDefinition throw_attack = Builder.withLength(1.3393F)
        .looping()
        .addAnimation(
            "body",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.2976F, KeyframeAnimations.degreeVec(7.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.744F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.0417F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.3393F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "left_arm",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(-92.5592F, -0.5409F, 12.4884F), Interpolations.LINEAR),
                    new Keyframe(0.744F, KeyframeAnimations.degreeVec(-112.5592F, -0.5409F, 12.4884F), Interpolations.LINEAR),
                    new Keyframe(0.8929F, KeyframeAnimations.degreeVec(-92.5592F, -0.5409F, 12.4884F), Interpolations.LINEAR),
                    new Keyframe(1.0417F, KeyframeAnimations.degreeVec(-50.0592F, -0.5409F, 12.4884F), Interpolations.LINEAR),
                    new Keyframe(1.1905F, KeyframeAnimations.degreeVec(-90.0592F, -0.5409F, 12.4884F), Interpolations.LINEAR),
                    new Keyframe(1.3393F, KeyframeAnimations.degreeVec(-92.5592F, -0.5409F, 12.4884F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "right_arm",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(-95.0F, 0.0F, -7.5F), Interpolations.LINEAR),
                    new Keyframe(0.1488F, KeyframeAnimations.degreeVec(-112.5F, 0.0F, -7.5F), Interpolations.LINEAR),
                    new Keyframe(0.2976F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, -7.5F), Interpolations.LINEAR),
                    new Keyframe(0.4464F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, -7.5F), Interpolations.LINEAR),
                    new Keyframe(0.5952F, KeyframeAnimations.degreeVec(-95.0F, 0.0F, -7.5F), Interpolations.LINEAR),
                    new Keyframe(1.3393F, KeyframeAnimations.degreeVec(-95.0F, 0.0F, -7.5F), Interpolations.LINEAR)
                }
            )
        )
        .build();
    public static final AnimationDefinition hover = Builder.withLength(1.282F)
        .looping()
        .addAnimation(
            "left_arm",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3205F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.641F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.9615F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.282F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "right_arm",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3205F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.641F, KeyframeAnimations.degreeVec(-7.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.9615F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.282F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "whole",
            new AnimationChannel(
                Targets.POSITION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3205F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.641F, KeyframeAnimations.posVec(0.0F, 2.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.9615F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.282F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .build();
    public static final AnimationDefinition movement = Builder.withLength(0.6667F)
        .looping()
        .addAnimation(
            "head",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.degreeVec(7.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.6667F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "body",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.degreeVec(12.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.6667F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "left_arm",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(7.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.degreeVec(17.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.degreeVec(7.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.6667F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "right_arm",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.degreeVec(12.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.6667F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .build();
    public static final AnimationDefinition death = Builder.withLength(1.1667F)
        .addAnimation(
            "head",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(12.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.degreeVec(85.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.6667F, KeyframeAnimations.degreeVec(105.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.8333F, KeyframeAnimations.degreeVec(127.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(1.0F, KeyframeAnimations.degreeVec(175.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "head",
            new AnimationChannel(
                Targets.POSITION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F, -1.0F, -3.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, -5.0F, -7.0F), Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -7.0F, -5.0F), Interpolations.LINEAR),
                    new Keyframe(0.6667F, KeyframeAnimations.posVec(0.0F, -4.0F, -5.0F), Interpolations.LINEAR),
                    new Keyframe(0.8333F, KeyframeAnimations.posVec(0.0F, -8.0F, -8.0F), Interpolations.LINEAR),
                    new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, -10.0F, -11.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "body",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.degreeVec(47.5F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.degreeVec(87.5F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "body",
            new AnimationChannel(
                Targets.POSITION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F, -1.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, -5.0F, -7.0F), Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -15.0F, -7.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "left_arm",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "left_arm",
            new AnimationChannel(
                Targets.POSITION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -1.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F, -7.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, -15.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "right_arm",
            new AnimationChannel(
                Targets.ROTATION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .addAnimation(
            "right_arm",
            new AnimationChannel(
                Targets.POSITION,
                new Keyframe[]{
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F, -6.0F, 0.0F), Interpolations.LINEAR),
                    new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, -15.0F, 0.0F), Interpolations.LINEAR)
                }
            )
        )
        .build();
}
