package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.factions.FactionClass;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

// The floran's powers by MMS Origins' rules: Sun Power, Bark Skin, Sunlit Vigor, Sunlit Fury, Grovekeepers and Leaf Form.
final class FactionFloran {

    private static final int SUN_MAX = 84;
    private static final double NIGHT_SHARE = 0.5;
    private static final double NOON = 6000.0;
    private static final double HALF_DAY = 6000.0;
    private static final double DAY_LENGTH = 24000.0;
    private static final double SHARPNESS = 3.0;
    private static final double INTAKE_PEAK = 0.4;
    private static final double GROVE_PEAK = 0.2;
    private static final double BLESSING_RATE = 1.0 / 6.0;
    private static final int BASK_INTERVAL = 12;
    private static final int DRINK_SUN = 10;
    private static final int DRINK_FLOOR = 32;
    private static final int DRINK_CAP = 42;
    private static final int SUMMON_COST = 30;
    private static final int SUMMON_CHARGE_TICKS = 30;
    private static final int PILOTED_REFUND = SUMMON_COST / 2;
    private static final int SLAIN_REFUND_TICKS = 600;
    private static final double BARK_NIGHT = 1.0;
    private static final double BARK_PEAK = 5.0;
    private static final double BARK_THRESHOLD = 0.25;
    private static final int BARK_INTERVAL = 10;
    private static final float VIGOR_CHANCE = 0.35f;
    private static final int FURY_DRAIN_INTERVAL = 8;
    private static final double FURY_DAMAGE = 3.0;
    private static final double FURY_RANGE = 6.0;
    private static final int FURY_SPARE = 10;
    private static final float BLESSED_FURY_SHIELD = 0.75f;
    private static final double BLESSED_FURY_SPEED = 0.2;
    private static final int LEAF_MORPH_TICKS = 10;
    private static final int LEAF_WAIT_TICKS = 40;
    private static final float LEAF_FRAILTY = 1.25f;
    private static final float RIDING_FRAILTY = 1.15f;
    private static final double MOUNT_RANGE = 6.0;
    private static final double LEAF_RUN_BONUS = 0.25;
    private static final Identifier LEAF_RUN = MmsDungeons.id("leaf_run");
    private static final Identifier BARK_SKIN = MmsDungeons.id("bark_skin");
    private static final Identifier FURY_STRENGTH = MmsDungeons.id("sunlit_fury");
    private static final Identifier FURY_SPEED = MmsDungeons.id("sunlit_fury_speed");
    private static final TagKey<Biome> ENCHANTED_GROVES = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("mms_origins", "enchanted_groves"));

    private final FactionMember mob;
    private final List<int[]> refunds = new ArrayList<>();
    private int sun;
    private boolean fury;
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
        this.sun = random.nextInt(this.cap() + 1);
    }

    int sun() {
        return this.sun;
    }

    boolean fury() {
        return this.fury;
    }

    void load(int sun, boolean fury) {
        this.sun = Math.clamp(sun, 0, SUN_MAX);
        this.fury = fury && this.sun > 0;
    }

    // Every tick: sun intake, Bark Skin, Sunlit Fury's drain, the leaf-form morph and delayed refunds.
    void tick(ServerLevel level) {
        this.gatherSun(level);
        if (this.mob.tickCount % BARK_INTERVAL == 0) {
            this.barkSkin();
        }
        this.tickFury(level);
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
        if (!this.fury && close && this.mob.factionClass().melee() && !this.mob.piloting() && this.sun > spare) {
            this.setFury(true);
        }
        if (this.charge > 0 || raise && this.sun >= SUMMON_COST && !this.leafForm()) {
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
        if (!room || this.sun < SUMMON_COST || this.mob.downed()) {
            this.charge = 0;
            return;
        }
        if (++this.charge >= SUMMON_CHARGE_TICKS) {
            this.charge = 0;
            Entity keeper = FactionGrovekeeper.raise(this.mob, level);
            this.sun -= SUMMON_COST;
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

    // Drinking a potion or water: 10 sun, up to half the reserve, while Sunlit Fury is off.
    void drink() {
        if (this.sun < DRINK_CAP && !this.fury) {
            this.sun = this.sun < DRINK_FLOOR ? this.sun + DRINK_SUN : DRINK_CAP;
        }
    }

    // Whether a drink would let it raise a Grovekeeper it otherwise could not.
    private boolean wantsWater(ServerLevel level) {
        return FactionMember.ORIGINS && !this.fury && this.sun < SUMMON_COST && this.sun + DRINK_SUN >= SUMMON_COST
            && FactionGrovekeeper.roomFor(this.mob, level);
    }

    // Sunlit Vigor's critical hits in the sun, which Sunlit Fury replaces.
    float critChance() {
        return !this.fury && this.exposedToSun() ? VIGOR_CHANCE : 0.0f;
    }

    // Sunlit Fury's extra melee damage, doubled under Mael's Blessing.
    double furyDamage() {
        if (!this.fury || this.sun <= 0) {
            return 0.0;
        }
        return this.blessed() ? FURY_DAMAGE * 2.0 : FURY_DAMAGE;
    }

    Identifier furyModifier() {
        return FURY_STRENGTH;
    }

    // Leaf form's frailty, more while riding a Grovekeeper, and Sunlit Fury's guard under Mael's Blessing.
    float damageTakenScale() {
        float scale = 1.0f;
        if (this.leafForm()) {
            scale *= LEAF_FRAILTY;
            if (this.mob.piloting()) {
                scale *= RIDING_FRAILTY;
            }
        }
        if (this.fury && this.sun > 0 && this.blessed()) {
            scale *= BLESSED_FURY_SHIELD;
        }
        return scale;
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

    private void gatherSun(ServerLevel level) {
        if (!this.exposedToSun() || this.sun >= this.cap()) {
            return;
        }
        double strength = this.strength();
        if (!this.fury && this.mob.tickCount % BASK_INTERVAL == 0) {
            this.sun++;
        }
        if (this.sun < this.cap() && this.mob.getRandom().nextDouble() < INTAKE_PEAK * strength) {
            this.sun++;
        }
        boolean grove = level.getBiome(this.mob.blockPosition()).is(ENCHANTED_GROVES);
        if (grove && this.sun < this.cap() && this.mob.getRandom().nextDouble() < GROVE_PEAK * strength) {
            this.sun++;
        }
    }

    private void barkSkin() {
        AttributeInstance armor = this.mob.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        double base = this.sun >= BARK_THRESHOLD * SUN_MAX ? BARK_NIGHT : 0.0;
        double amount = base + (BARK_PEAK - base) * this.strength();
        armor.removeModifier(BARK_SKIN);
        if (amount > 0.0) {
            armor.addTransientModifier(new AttributeModifier(BARK_SKIN, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private void tickFury(ServerLevel level) {
        if (this.fury && (this.sun <= 0 || this.mob.getTarget() == null || this.mob.downed())) {
            this.setFury(false);
        }
        if (this.fury && this.mob.tickCount % FURY_DRAIN_INTERVAL == 0) {
            int keepers = FactionMember.ORIGINS ? FactionGrovekeeper.active(this.mob, level) : 0;
            this.sun = Math.max(0, this.sun - Math.min(3, keepers + 1));
        }
        AttributeInstance speed = this.mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && this.mob.tickCount % 5 == 0) {
            boolean quick = this.fury && this.sun > 0 && this.blessed();
            if (quick && !speed.hasModifier(FURY_SPEED)) {
                speed.addTransientModifier(new AttributeModifier(FURY_SPEED, BLESSED_FURY_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (!quick) {
                speed.removeModifier(FURY_SPEED);
            }
        }
    }

    private void setFury(boolean on) {
        if (this.fury == on) {
            return;
        }
        this.fury = on;
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

    // Leaf form's pace on the move: close to a running player's.
    private void runAsLeaves() {
        AttributeInstance speed = this.mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(LEAF_RUN)) {
            speed.addTransientModifier(new AttributeModifier(LEAF_RUN, LEAF_RUN_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private void refund(int amount) {
        this.sun = Math.min(SUN_MAX, this.sun + amount);
    }

    // Origins' exposed to the sun: daytime, open sky above the head, and no rain.
    private boolean exposedToSun() {
        BlockPos head = BlockPos.containing(this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
        return this.mob.level().isBrightOutside() && this.mob.level().canSeeSky(head) && !this.mob.level().isRainingAt(head);
    }

    // Mael's Blessing: open sky while the sun is high enough to fill one sun every six ticks.
    private boolean blessed() {
        return this.mob.level().canSeeSky(this.mob.blockPosition().above()) && INTAKE_PEAK * this.strength() >= BLESSING_RATE;
    }

    // How much sun the hour lets it hold: half the reserve at night, all of it at noon.
    private int cap() {
        return (int) Math.round(SUN_MAX * (NIGHT_SHARE + (1.0 - NIGHT_SHARE) * this.daylight()));
    }

    private double daylight() {
        double time = this.mob.level().getOverworldClockTime() % DAY_LENGTH;
        return Math.max(0.0, 1.0 - Math.abs(time - NOON) / HALF_DAY);
    }

    // The sun's strength on MMS Origins' curve, from nothing at sunrise and sunset to 1 at noon.
    private double strength() {
        return Math.pow(this.daylight(), SHARPNESS);
    }
}
