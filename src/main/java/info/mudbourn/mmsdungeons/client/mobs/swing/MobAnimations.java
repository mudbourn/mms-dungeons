package info.mudbourn.mmsdungeons.client.mobs.swing;

import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.HumanoidAnimationController;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.MirrorModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.SpeedModifier;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.math.Vec3f;
import com.zigythebird.playeranimcore.molang.MolangLoader;
import info.mudbourn.mmsdungeons.mobs.factions.FactionWeapons;
import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionMember;
import info.mudbourn.mmsdungeons.net.MobSwingPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.bettercombat.BetterCombatMod;
import net.bettercombat.client.BetterCombatClientMod;
import net.bettercombat.client.animation.TransmissionSpeedModifier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.fx.PlayerAnimation;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.utils.AnimationHelper;
import org.jspecify.annotations.Nullable;

// Plays Better Combat attacks on a mob through Player Animation Library, layered as on a player, plus weapon poses, Spell Engine cast and release animations and the Combat Roll roll for faction members.
public final class MobAnimations extends AvatarAnimManager {

    private static final Identifier ROLL = Identifier.fromNamespaceAndPath("combat_roll", "roll");
    private static final Map<LivingEntity, MobAnimations> BY_MOB = new WeakHashMap<>();
    private static final double WALKING_SPEED = 0.03;
    private static final int SPELL_FADE_TICKS = 3;

    private final LivingEntity mob;
    private final @Nullable FactionMember member;
    private final HumanoidAnimationController attack = controller();
    private final TransmissionSpeedModifier attackSpeed = new TransmissionSpeedModifier(1.0f);
    private final MirrorModifier attackMirror = new MirrorModifier();
    private final HumanoidAnimationController roll = controller();
    private final SpeedModifier rollSpeed = new SpeedModifier(1.0f);
    private final PoseLayer mainItemPose = new PoseLayer(false);
    private final PoseLayer mainBodyPose = new PoseLayer(false);
    private final PoseLayer offItemPose = new PoseLayer(true);
    private final PoseLayer offBodyPose = new PoseLayer(true);
    private final SpellLayer casting = new SpellLayer();
    private final SpellLayer release = new SpellLayer();
    private int lastSwing;
    private int lastRoll;
    private String lastSpell = "";
    private int lastRelease;

    private MobAnimations(LivingEntity mob) {
        super(null);
        this.mob = mob;
        this.member = mob instanceof FactionMember faction ? faction : null;
        this.lastSwing = this.member == null ? 0 : this.member.swingSequence();
        this.lastRoll = this.member == null ? 0 : this.member.rollTicks();
        this.lastRelease = this.member == null ? 0 : this.member.releaseSequence();
        this.attackMirror.enabled = false;
        this.attack.addModifier(this.attackMirror, 0);
        this.attack.addModifier(this.attackSpeed, 0);
        this.attack.addModifierLast(this.pitchAdjustment());
        this.attack.setPostAnimationSetupConsumer(bones -> {
            bones.apply("torso").setEnabled(true);
            bones.apply("head").rotXEnabled = false;
            if (this.legsAnimate()) {
                return;
            }
            bones.apply("right_leg").setEnabled(false);
            bones.apply("left_leg").setEnabled(false);
        });
        this.roll.addModifier(this.rollSpeed, 0);
        this.addAnimLayer(1, this.offItemPose.controller);
        this.addAnimLayer(2, this.offBodyPose.controller);
        this.addAnimLayer(3, this.mainItemPose.controller);
        this.addAnimLayer(4, this.mainBodyPose.controller);
        this.addAnimLayer(1000, this.roll);
        this.addAnimLayer(1500, this.casting.controller);
        this.addAnimLayer(1600, this.release.controller);
        this.addAnimLayer(2000, this.attack);
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MobSwingPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            if (context.client().level != null && context.client().level.getEntity(payload.entityId()) instanceof LivingEntity mob) {
                of(mob).playAttack(payload.animation(), payload.ticks(), payload.upswing(), payload.hand());
            }
        }));
        ClientTickEvents.END_LEVEL_TICK.register(level -> {
            for (Entity entity : level.entitiesForRendering()) {
                if (entity instanceof FactionMember member) {
                    of(member);
                }
            }
            List<MobAnimations> live = new ArrayList<>(BY_MOB.values());
            BY_MOB.keySet().removeIf(mob -> mob.isRemoved() || mob.level() != level);
            for (MobAnimations animations : live) {
                if (!animations.mob.isRemoved()) {
                    animations.tickMob();
                }
            }
        });
    }

    // Hands a faction member's animation stack to Player Animation Library's model, item and body hooks for this frame.
    public static void attach(FactionMember mob, AvatarRenderState state, float partialTick) {
        of(mob).attachTo(state, partialTick);
    }

    // Does the same for any other armed mob that has swung a Better Combat weapon, and clears it for one that has not; players are left to Player Animation Library.
    public static void attachIfSwinging(LivingEntity mob, LivingEntityRenderState state, float partialTick) {
        if (mob instanceof FactionMember || !(state instanceof ArmedEntityRenderState) || state instanceof AvatarRenderState) {
            return;
        }
        MobAnimations animations = BY_MOB.get(mob);
        if (animations == null) {
            ((IAvatarAnimationState) state).playerAnimLib$setAnimManager(null);
        } else {
            animations.attachTo(state, partialTick);
        }
    }

    private void attachTo(LivingEntityRenderState state, float partialTick) {
        this.setTickDelta(partialTick);
        ((IAvatarAnimationState) state).playerAnimLib$setAnimManager(this);
    }

    public static boolean hasRoll() {
        return PlayerAnimResources.hasAnimation(ROLL);
    }

    private static MobAnimations of(LivingEntity mob) {
        return BY_MOB.computeIfAbsent(mob, MobAnimations::new);
    }

    private static HumanoidAnimationController controller() {
        return new HumanoidAnimationController((controller, data, setter) -> PlayState.STOP, MolangLoader::createNewEngine);
    }

    private void tickMob() {
        if (this.member != null) {
            this.tickMember(this.member);
        }
        this.handleAnimations(0.0f, true, false);
    }

    // Faction members sync their swings and rolls through entity data, and hold Better Combat's resting weapon poses.
    private void tickMember(FactionMember member) {
        int swing = member.swingSequence();
        if (swing != this.lastSwing) {
            this.lastSwing = swing;
            this.playAttack(member.swingAnimation(), member.swingTicks(), member.swingUpswing(), member.swingHand());
        }
        int rollTicks = member.rollTicks();
        if (rollTicks > 0 && this.lastRoll == 0) {
            this.playRoll();
        }
        this.lastRoll = rollTicks;
        this.updateSpells(member);
        this.updatePoses(member);
    }

    // Spell Engine's casting animation for as long as the member casts, and its release animation each time a spell goes off.
    private void updateSpells(FactionMember member) {
        String spell = member.castingSpell();
        if (!spell.equals(this.lastSpell)) {
            this.lastSpell = spell;
            Spell value = this.spell(spell);
            Spell.Active.Cast cast = value == null || value.active == null ? null : value.active.cast;
            this.casting.play(cast == null ? null : cast.animation, cast != null && cast.animation_pitch);
        }
        int released = member.releaseSequence();
        if (released != this.lastRelease) {
            this.lastRelease = released;
            Spell value = this.spell(member.releasedSpell());
            this.release.play(value == null || value.release == null ? null : value.release.animation, true);
        }
    }

    private @Nullable Spell spell(String id) {
        Identifier parsed = id.isEmpty() ? null : Identifier.tryParse(id);
        return parsed == null ? null : SpellRegistry.from(this.mob.level()).getValue(parsed);
    }

    @Override
    public void handleAnimations(float partialTick, boolean fullTick, boolean firstPersonPass) {
        Vec3 velocity = this.mob.getDeltaMovement();
        AnimationData data = new AnimationData((float) ((Math.abs(velocity.x) + Math.abs(velocity.z)) / 2.0), partialTick, firstPersonPass);
        if (fullTick) {
            this.tick(data.copy());
        }
        float frame = this.mob.tickCount + partialTick;
        if (this.isFirstTick() || frame != this.getLastUpdateTime()) {
            if (!Minecraft.getInstance().isPaused()) {
                this.updatedAt(frame);
            }
            this.tickAnimation(this, data);
        }
    }

    // Better Combat's attack timing: the upswing plays at its own speed, then the rest of the swing winds down, mirrored for the off hand.
    private void playAttack(String name, int ticks, float swingUpswing, byte hand) {
        Identifier id = Identifier.tryParse(name);
        Optional<Animation> found = id == null ? Optional.empty() : PlayerAnimResources.getAnimationOptional(id);
        if (found.isEmpty()) {
            return;
        }
        Animation animation = found.get();
        float length = Math.max(1, ticks);
        float upswing = Mth.clamp(swingUpswing, 0.05f, 0.95f);
        float endTick = animation.data().<Float>get("endTick").orElse(animation.length());
        float speed = endTick / length;
        float multiplier = BetterCombatMod.config.getUpswingMultiplier();
        float upswingSpeed = speed / (upswing / multiplier);
        float downwindSpeed = (float) (speed * Mth.lerp(Math.max(multiplier - 0.5, 0.0) / 0.5, 1.0f - upswing, upswing / (1.0f - upswing)));
        boolean mirror = hand == FactionMember.SWING_OFF_HAND;
        this.attackMirror.enabled = mirror != this.leftHanded();
        this.attackSpeed.set(upswingSpeed, List.of(
            new TransmissionSpeedModifier.Gear(length * upswing, downwindSpeed),
            new TransmissionSpeedModifier.Gear(length, speed)
        ));
        this.attack.triggerAnimation(animation);
    }

    private void playRoll() {
        Optional<Animation> found = PlayerAnimResources.getAnimationOptional(ROLL);
        if (found.isEmpty()) {
            return;
        }
        this.rollSpeed.speed = found.get().length() / FactionMember.ROLL_TICKS;
        this.roll.triggerAnimation(found.get());
    }

    // Better Combat's resting weapon poses, dropped while busy and kept on the body only for two-handed weapons once it walks.
    private void updatePoses(FactionMember member) {
        boolean busy = member.isUsingItem()
            || member.action() != FactionMember.ACTION_NONE
            || member.rollTicks() > 0
            || member.crawling()
            || member.onClimbable()
            || member.isDeadOrDying();
        String main = busy ? "" : FactionWeapons.pose(member.getMainHandItem(), false);
        String off = busy ? "" : FactionWeapons.pose(member.getOffhandItem(), true);
        boolean left = this.leftHanded();
        this.mainItemPose.set(main, left);
        this.offItemPose.set(off, left);
        boolean bodyPose = FactionWeapons.heavy(this.mob.getMainHandItem()) || !this.walking();
        this.mainBodyPose.set(bodyPose ? main : "", left);
        this.offBodyPose.set(bodyPose ? off : "", left);
    }

    // Better Combat's look adjustment: the attack bends with the mob's pitch, mostly through the body.
    private AdjustmentModifier pitchAdjustment() {
        return new AdjustmentModifier((part, data) -> {
            float pitch = (float) Math.toRadians(this.mob.getXRot());
            float rotation = switch (part) {
                case "body" -> pitch * 0.75f;
                case "right_arm", "left_arm" -> pitch * 0.25f;
                case "right_leg", "left_leg" -> -pitch * 0.75f;
                default -> Float.NaN;
            };
            if (Float.isNaN(rotation)) {
                return Optional.empty();
            }
            return Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(rotation, 0.0f, 0.0f), new Vec3f(0.0f, 0.0f, 0.0f)));
        });
    }

    // Whether the attack may move the legs: never while crawling, or while running faster than Better Combat's leg threshold.
    private boolean legsAnimate() {
        if (this.mob.getPose() == Pose.SWIMMING || this.mob.getVehicle() != null) {
            return false;
        }
        float threshold = BetterCombatClientMod.config.legAnimationThreshold;
        return threshold <= 0.0f || !this.walking() || this.mob.getDeltaMovement().horizontalDistanceSqr() <= threshold * threshold;
    }

    private boolean walking() {
        return this.mob.getDeltaMovement().horizontalDistance() > WALKING_SPEED;
    }

    private boolean leftHanded() {
        return this.mob.getMainArm() == HumanoidArm.LEFT;
    }

    // One Spell Engine animation channel as a player has it: faded in, mirrored for left-handed mobs, and bent with the mob's pitch when the spell asks.
    private final class SpellLayer {

        private final HumanoidAnimationController controller = controller();
        private final SpeedModifier speed = new SpeedModifier(1.0f);
        private final MirrorModifier mirror = new MirrorModifier();
        private boolean pitching;

        private SpellLayer() {
            this.mirror.enabled = false;
            this.controller.addModifier(this.mirror, 0);
            this.controller.addModifier(this.speed, 0);
            this.controller.addModifierLast(new AdjustmentModifier((part, data) -> {
                if (!this.pitching) {
                    return Optional.empty();
                }
                float pitch = (float) Math.toRadians(MobAnimations.this.mob.getXRot() / 2.0f);
                float rotation = switch (part) {
                    case "body", "right_arm", "left_arm" -> pitch;
                    case "right_leg", "left_leg" -> -pitch;
                    default -> Float.NaN;
                };
                if (Float.isNaN(rotation)) {
                    return Optional.empty();
                }
                return Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(rotation, 0.0f, 0.0f), new Vec3f(0.0f, 0.0f, 0.0f)));
            }));
            this.controller.setPostAnimationSetupConsumer(bones -> {
                bones.apply("torso").setEnabled(true);
                bones.apply("head").rotXEnabled = false;
                if (MobAnimations.this.mob.getVehicle() != null) {
                    bones.apply("right_leg").setEnabled(false);
                    bones.apply("left_leg").setEnabled(false);
                }
            });
        }

        private void play(@Nullable PlayerAnimation animation, boolean pitching) {
            String id = animation == null ? "" : AnimationHelper.getAnimationId(MobAnimations.this.mob, animation);
            Identifier parsed = id == null || id.isEmpty() ? null : Identifier.tryParse(id);
            Optional<Animation> found = parsed == null ? Optional.empty() : PlayerAnimResources.getAnimationOptional(parsed);
            if (found.isEmpty()) {
                if (this.controller.isActive()) {
                    this.controller.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(SPELL_FADE_TICKS, EasingType.EASE_IN_OUT_SINE), RawAnimation.begin().thenWait(SPELL_FADE_TICKS));
                }
                return;
            }
            this.pitching = pitching;
            this.mirror.enabled = MobAnimations.this.leftHanded();
            this.speed.speed = animation.speed > 0.0f ? animation.speed : 1.0f;
            this.controller.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(SPELL_FADE_TICKS, EasingType.EASE_IN_OUT_SINE), found.get());
        }
    }

    // One Better Combat pose channel, mirrored for the off hand and for left-handed mobs.
    private static final class PoseLayer {

        private final HumanoidAnimationController controller = controller();
        private final MirrorModifier mirror = new MirrorModifier();
        private final boolean offHand;
        private @Nullable String pose;
        private boolean mirrored;

        private PoseLayer(boolean offHand) {
            this.offHand = offHand;
            this.mirror.enabled = false;
            this.controller.addModifier(this.mirror, 0);
        }

        private void set(String pose, boolean leftHanded) {
            boolean mirrored = this.offHand != leftHanded;
            if (pose.equals(this.pose) && mirrored == this.mirrored) {
                return;
            }
            this.pose = pose;
            this.mirrored = mirrored;
            Identifier id = pose.isEmpty() ? null : Identifier.tryParse(pose);
            Optional<Animation> found = id == null ? Optional.empty() : PlayerAnimResources.getAnimationOptional(id);
            if (found.isEmpty()) {
                this.controller.stopTriggeredAnimation();
                return;
            }
            this.mirror.enabled = mirrored;
            this.controller.triggerAnimation(found.get());
        }
    }
}
