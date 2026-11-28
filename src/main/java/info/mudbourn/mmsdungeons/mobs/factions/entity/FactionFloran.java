package info.mudbourn.mmsdungeons.mobs.factions.entity;

import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.factions.FactionClass;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jspecify.annotations.Nullable;

// The floran's AI on its real MMS Origins powers: it spends the real Sun Power, presses Sunlit Fury's key, and drives Grovekeepers and Leaf Form, which a player steers by hand.
final class FactionFloran {

    private static final Identifier SUN = Identifier.fromNamespaceAndPath("mms_origins", "sun_power");
    private static final Identifier FURY_ACTIVE = Identifier.fromNamespaceAndPath("mms_origins", "sunlit_fury_active");
    private static final Identifier LEAF_MORPH = Identifier.fromNamespaceAndPath("mms_origins", "leaf_form_morph");
    private static final int SUN_MAX = 84;
    private static final double NIGHT_SHARE = 0.5;
    private static final double NOON = 6000.0;
    private static final double HALF_DAY = 6000.0;
    private static final double DAY_LENGTH = 24000.0;
    private static final int DRINK_SUN = 10;
    private static final int SUMMON_COST = 30;
    private static final int SUMMON_CHARGE_TICKS = 30;
    private static final int PILOTED_REFUND = SUMMON_COST / 2;
    private static final int SLAIN_REFUND_TICKS = 600;
    private static final double FURY_RANGE = 6.0;
    private static final int FURY_SPARE = 10;
    private static final int LEAF_MORPH_TICKS = 10;
    private static final int LEAF_WAIT_TICKS = 40;
    private static final double MOUNT_RANGE = 6.0;
    private static final double LEAF_RUN_BONUS = 0.25;
    private static final Identifier LEAF_RUN = MmsDungeons.id("leaf_run");
    private static final TagKey<Block> LEAF_CLIMBABLE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("mms_origins", "leaf_climbable"));

    private final FactionMember mob;
    private final List<int[]> refunds = new ArrayList<>();
    private int charge;
    private int morph = -1;
    private int leafWait;
    private boolean journey;
    private @Nullable Entity ridden;

    FactionFloran(FactionMember mob) {
        this.mob = mob;
    }

    // A floran met in the world has been gathering sun already: anywhere up to what the hour allows.
    void spawn(RandomSource random) {
        this.setSun(random.nextInt(this.cap() + 1));
    }

    int sun() {
        return Math.max(0, FactionOriginPowers.resource(this.mob, SUN));
    }

    private void setSun(int sun) {
        FactionOriginPowers.set(this.mob, SUN, Math.clamp(sun, 0, SUN_MAX));
    }

    boolean fury() {
        return FactionOriginPowers.resource(this.mob, FURY_ACTIVE) > 0;
    }

    // Every tick: Sunlit Fury put away out of a fight, the leaf-form morph and delayed refunds.
    void tick(ServerLevel level) {
        if (this.fury() && (this.mob.getTarget() == null || this.mob.downed())) {
            this.setFury(false);
        }
        this.tickLeafForm(level);
        this.refunds.removeIf(refund -> {
            if (--refund[0] > 0) {
                return false;
            }
            this.refund(refund[1]);
            return true;
        });
        this.ridden = this.mob.getVehicle();
    }

    // In a fight: Sunlit Fury for melee, a Grovekeeper charged up while there is sun to spend, and leaf form to pilot one.
    void fight(ServerLevel level, LivingEntity target) {
        boolean raise = FactionMember.ORIGINS && FactionGrovekeeper.roomFor(this.mob, level);
        int spare = raise ? SUMMON_COST + FURY_SPARE : FURY_SPARE;
        boolean close = this.mob.distanceTo(target) < FURY_RANGE;
        if (!this.fury() && close && this.mob.factionClass().melee() && !this.mob.piloting() && this.sun() > spare) {
            this.setFury(true);
        }
        if (this.charge > 0 || raise && this.sun() >= SUMMON_COST && !this.leafForm()) {
            this.chargeSummon(level, raise);
        }
        if (this.wantsWater(level) && !this.mob.consuming() && !this.leafForm()) {
            this.mob.startDrinkingWater();
        }
        if (FactionMember.ORIGINS && this.mob.factionClass() != FactionClass.PRIEST && !this.mob.isPassenger() && this.morph < 0
            && FactionGrovekeeper.freeKeeper(this.mob, level, MOUNT_RANGE) != null) {
            this.beginLeafForm();
        }
    }

    // Holds the summon key: the evoker's chant starts, and after a second and a half a Grovekeeper rises for 30 sun.
    private void chargeSummon(ServerLevel level, boolean room) {
        if (!room || this.sun() < SUMMON_COST || this.mob.downed()) {
            this.charge = 0;
            return;
        }
        if (++this.charge >= SUMMON_CHARGE_TICKS) {
            this.charge = 0;
            Entity keeper = FactionGrovekeeper.raise(this.mob, level);
            this.setSun(this.sun() - SUMMON_COST);
            this.mob.powers().track(keeper);
        }
    }

    // A Grovekeeper left: refunds half at once if it was being ridden, else the full cost, 30 seconds later when it was slain.
    void keeperGone(Entity keeper) {
        if (keeper == this.ridden) {
            this.refund(PILOTED_REFUND);
        } else if (keeper.getRemovalReason() == Entity.RemovalReason.KILLED) {
            this.refunds.add(new int[] {SLAIN_REFUND_TICKS, SUMMON_COST});
        } else if (keeper.getRemovalReason() == Entity.RemovalReason.DISCARDED) {
            this.refund(SUMMON_COST);
        }
    }

    // Whether a drink would let it raise a Grovekeeper it otherwise could not.
    private boolean wantsWater(ServerLevel level) {
        int sun = this.sun();
        return FactionMember.ORIGINS && !this.fury() && sun < SUMMON_COST && sun + DRINK_SUN >= SUMMON_COST
            && FactionGrovekeeper.roomFor(this.mob, level);
    }

    // Folded into leaves: no items, no jumping, and able to ride its own Grovekeeper.
    boolean leafForm() {
        return this.morph >= LEAF_MORPH_TICKS;
    }

    boolean changing() {
        return this.morph >= 0 && this.morph < LEAF_MORPH_TICKS;
    }

    void stop() {
        this.charge = 0;
        this.setFury(false);
        if (this.morph >= 0) {
            this.leaveLeafForm();
        }
    }

    // Presses Sunlit Fury's key as a player does to turn it on or off; the real power drains the sun and strengthens the blows.
    private void setFury(boolean on) {
        if (this.fury() != on) {
            FactionOriginPowers.key(this.mob, FactionOriginPowers.PRIMARY, true);
        }
    }

    // Whether it is in leaf form to cross ground rather than to ride a Grovekeeper.
    boolean journeying() {
        return this.journey;
    }

    // Folds into leaf form to travel along the ground, or unfolds again.
    void journey(boolean on) {
        if (on && this.morph < 0 && !this.mob.isPassenger()) {
            this.journey = true;
            this.beginLeafForm();
        } else if (!on && this.journey) {
            this.leaveLeafForm();
        }
    }

    private void beginLeafForm() {
        this.morph = 0;
        this.leafWait = 0;
        this.mob.stopUsingItem();
    }

    private void leaveLeafForm() {
        this.morph = -1;
        this.journey = false;
        this.mob.setLeafFormShown(false);
        FactionOriginPowers.set(this.mob, LEAF_MORPH, 0);
        AttributeInstance speed = this.mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(LEAF_RUN);
        }
        if (this.mob.roomToStand()) {
            this.mob.setCrawling(false);
        }
    }

    // Folds over half a second, slowed, then crawls to its nearest free Grovekeeper and climbs in; returns once it has nothing to ride.
    private void tickLeafForm(ServerLevel level) {
        if (this.morph < 0) {
            return;
        }
        if (this.journey && (this.mob.downed() || this.mob.isPassenger())) {
            this.leaveLeafForm();
            return;
        }
        if (this.changing()) {
            this.mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 3, 3, false, false, false));
            if (++this.morph == LEAF_MORPH_TICKS) {
                this.mob.setCrawling(true);
                this.mob.setLeafFormShown(true);
                FactionOriginPowers.set(this.mob, LEAF_MORPH, LEAF_MORPH_TICKS);
            }
            return;
        }
        if (this.journey) {
            this.runAsLeaves();
            return;
        }
        if (this.mob.isPassenger()) {
            this.leafWait = 0;
            return;
        }
        Entity keeper = FactionMember.ORIGINS ? FactionGrovekeeper.freeKeeper(this.mob, level, MOUNT_RANGE) : null;
        if (keeper != null && this.mob.distanceTo(keeper) < 2.5) {
            FactionGrovekeeper.carry(keeper, this.mob);
        } else if (keeper != null && this.leafWait++ < LEAF_WAIT_TICKS) {
            this.mob.getNavigation().moveTo(keeper, 1.0);
        } else {
            this.leaveLeafForm();
        }
    }

    // Leaf form's pace on the move: close to a running player's, but only while buried in natural blocks as a player's is.
    private void runAsLeaves() {
        AttributeInstance speed = this.mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean buried = this.buried();
        if (buried && !speed.hasModifier(LEAF_RUN)) {
            speed.addTransientModifier(new AttributeModifier(LEAF_RUN, LEAF_RUN_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!buried) {
            speed.removeModifier(LEAF_RUN);
        }
    }

    private boolean buried() {
        BlockPos feet = this.mob.blockPosition();
        return this.mob.level().getBlockState(feet).is(LEAF_CLIMBABLE) || this.mob.level().getBlockState(feet.above()).is(LEAF_CLIMBABLE);
    }

    private void refund(int amount) {
        this.setSun(this.sun() + amount);
    }

    // How much sun the hour lets it hold: half the reserve at night, all of it at noon.
    private int cap() {
        return (int) Math.round(SUN_MAX * (NIGHT_SHARE + (1.0 - NIGHT_SHARE) * this.daylight()));
    }

    private double daylight() {
        double time = this.mob.level().getOverworldClockTime() % DAY_LENGTH;
        return Math.max(0.0, 1.0 - Math.abs(time - NOON) / HALF_DAY);
    }
}
