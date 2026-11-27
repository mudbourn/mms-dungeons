package info.mudbourn.mmsdungeons.client.mobs.factions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.client.mobs.swing.MobAnimations;
import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionMember;
import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionSlots;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Draws a faction member as a skinned player, showing its cosmetic cover in place of the real armor it covers.
public class FactionRenderer extends HumanoidMobRenderer<FactionMember, AvatarRenderState, PlayerModel> {

    private static final boolean FURS = FabricLoader.getInstance().isModLoaded("mms_origins");

    public FactionRenderer(EntityRendererProvider.Context context) {
        super(context, new FactionModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        ArmorModelSet<HumanoidModel<AvatarRenderState>> armor = ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new);
        this.addLayer(new HumanoidArmorLayer<>(this, armor, context.getEquipmentRenderer()));
        this.addLayer(new FactionSlotLayer(this));
        if (FURS) {
            this.addLayer(FactionFurs.layer(this));
            this.addLayer(FactionFurs.beastfolk(this, context));
            this.addLayer(FactionFurs.wings(this, context));
            this.addLayer(FactionFurs.butterflyWings(this, context));
        }
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new FactionRenderState();
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return ((FactionRenderState) state).texture;
    }

    @Override
    public void extractRenderState(FactionMember mob, AvatarRenderState state, float partialTick) {
        super.extractRenderState(mob, state, partialTick);
        FactionRenderState faction = (FactionRenderState) state;
        faction.texture = MmsDungeons.id("textures/entity/factions/" + mob.skin() + ".png");
        // The player model draws the skin record, not the texture location, so it carries the faction skin too.
        state.skin = PlayerSkin.insecure(new ClientAsset.ResourceTexture(MmsDungeons.id("entity/factions/" + mob.skin())), null, null, PlayerModelType.WIDE);
        if (FURS) {
            FactionFurs.stamp(state, mob.origin(), mob.getUUID(), mob.isAggressive());
        }
        faction.action = mob.action();
        this.extractSlot(mob, FactionSlots.belt(), faction.beltItem);
        this.extractSlot(mob, FactionSlots.back(), faction.backItem);
        int roll = mob.rollTicks();
        faction.rollProgress = roll > 0 ? 1.0f - (roll - partialTick) / FactionMember.ROLL_TICKS : 0.0f;
        faction.rollAnimated = FactionsClient.ANIMATED && MobAnimations.hasRoll();
        if (FactionsClient.ANIMATED) {
            MobAnimations.attach(mob, state, partialTick);
        }
        // With AbsolutRevive in play a downed member lies on its back as a downed player does, instead of crawling.
        boolean downedPose = mob.downed() && AbsolutReviveRender.available();
        AbsolutReviveRender.stamp(state, downedPose, mob.collapse(partialTick));
        faction.crawling = mob.crawling() && !downedPose;
        if (downedPose) {
            state.swimAmount = 0.0f;
        }
        // A gliding avian keeps the elytra pose, not the hanging legs of a hovering flier.
        faction.flying = mob.flying() && !mob.isFallFlying();
        // As the player renderer banks a gliding player into its turns: by the angle between where it looks and where it is going.
        state.shouldApplyFlyingYRot = false;
        if (mob.isFallFlying() && state.fallFlyingTimeInTicks > 0.0f) {
            Vec3 look = mob.getViewVector(partialTick);
            Vec3 motion = mob.getDeltaMovement();
            double lookFlat = look.horizontalDistanceSqr();
            double motionFlat = motion.horizontalDistanceSqr();
            if (lookFlat > 0.0 && motionFlat > 0.0) {
                state.shouldApplyFlyingYRot = true;
                double dot = Math.min(1.0, (motion.x * look.x + motion.z * look.z) / Math.sqrt(lookFlat * motionFlat));
                double cross = motion.x * look.z - motion.z * look.x;
                state.flyingYRot = (float) (Math.signum(cross) * Math.acos(dot));
            }
        }
        state.isVisuallySwimming = faction.crawling;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.headEquipment = covered(mob, EquipmentSlot.HEAD, state.headEquipment);
        state.chestEquipment = covered(mob, EquipmentSlot.CHEST, state.chestEquipment);
        state.legsEquipment = covered(mob, EquipmentSlot.LEGS, state.legsEquipment);
        state.feetEquipment = covered(mob, EquipmentSlot.FEET, state.feetEquipment);
    }

    // Resolves the item in a Combat Amenities slot for drawing, or clears it when the mod is not loaded.
    private void extractSlot(FactionMember mob, @Nullable EquipmentSlot slot, ItemStackRenderState target) {
        if (slot == null) {
            target.clear();
            return;
        }
        this.itemModelResolver.updateForLiving(target, mob.getItemBySlot(slot), ItemDisplayContext.FIXED, mob);
    }

    // Lays a crawling member flat, and tumbles a rolling one head over heels when Combat Roll's animation is missing.
    @Override
    protected void setupRotations(AvatarRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        FactionRenderState faction = (FactionRenderState) state;
        if (state.swimAmount > 0.0f) {
            // As the player renderer lies a crawling player down: eased in by the swim amount, without the look pitch used only in water.
            poseStack.rotateDegrees(Axis.XP, Mth.lerp(state.swimAmount, 0.0f, -90.0f));
            if (faction.crawling) {
                poseStack.translate(0.0f, -1.0f, 0.3f);
            }
        } else if (state.isFallFlying) {
            // As the player renderer tips a gliding player flat along its flight line, eased in over its first ten ticks in the air.
            float ease = Mth.clamp(state.fallFlyingTimeInTicks * state.fallFlyingTimeInTicks / 100.0f, 0.0f, 1.0f);
            poseStack.rotateDegrees(Axis.XP, ease * (-90.0f - state.xRot));
            if (state.shouldApplyFlyingYRot) {
                poseStack.rotate(Axis.YP.rotation(state.flyingYRot));
            }
        } else if (faction.rollProgress > 0.0f && !faction.rollAnimated) {
            poseStack.translate(0.0f, 0.5f, 0.0f);
            poseStack.rotateDegrees(Axis.XP, -360.0f * faction.rollProgress);
            poseStack.translate(0.0f, -0.5f, 0.0f);
        }
    }

    private static ItemStack covered(FactionMember mob, EquipmentSlot slot, ItemStack real) {
        ItemStack cover = mob.cover(slot);
        return cover.isEmpty() ? real : cover;
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(FactionMember mob, HumanoidArm arm) {
        boolean usingThisArm = mob.isUsingItem() && mob.getUsedItemHand() == (arm == mob.getMainArm()
            ? InteractionHand.MAIN_HAND
            : InteractionHand.OFF_HAND);
        ItemStack held = arm == mob.getMainArm() ? mob.getMainHandItem() : mob.getOffhandItem();
        if (usingThisArm) {
            if (held.has(DataComponents.BLOCKS_ATTACKS)) {
                return HumanoidModel.ArmPose.BLOCK;
            }
            if (held.getUseAnimation() == ItemUseAnimation.EAT || held.getUseAnimation() == ItemUseAnimation.DRINK) {
                return HumanoidModel.ArmPose.ITEM;
            }
            if (held.getItem() instanceof CrossbowItem) {
                return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            }
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
        if (held.is(Items.CROSSBOW) && mob.isAggressive()) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        return super.getArmPose(mob, arm);
    }
}
