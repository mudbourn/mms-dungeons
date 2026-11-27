package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The faction member's hands on its origin's real powers: it presses the power keys a player of the origin would press, when that player would press them.
final class FactionPowers {

    private static final Identifier CRIT_MODIFIER = MmsDungeons.id("origin_crit");
    private static final double CRIT_BONUS = 0.5;
    private static final Identifier CLIMBING = Identifier.fromNamespaceAndPath("mms_origins", "climbing_toggle");
    static final Identifier SILK = Identifier.fromNamespaceAndPath("mms_origins", "master_of_webs_silk_resource");
    static final Identifier CLAWS = Identifier.fromNamespaceAndPath("mms_origins", "claw_mode");
    private static final Identifier SOUL = Identifier.fromNamespaceAndPath("mms_origins", "soulbound_energy");
    static final Identifier SPORES_DRAWN = Identifier.fromNamespaceAndPath("mms_origins", "spore_shift_drawn");
    private static final int GESTURE_HOLD = 11;
    static final int LEAP_CHARGE = 15;
    private static final int TOGGLE_GAP = 40;
    private static final int BULLET_GAP = 10;
    private static final int CHORD_GAP = 240;
    private static final double WEB_MIN = 4.0;
    private static final double WEB_RANGE = 24.0;
    private static final int WEB_GAP = 100;
    private static final int WEB_SILK_INTERVAL = 5;
    private static final double WEB_REEL_SPEED = 0.4;
    private static final double WEB_MAX_REEL_SPEED = 0.8;
    private static final int WEB_RAMP_DELAY = 20;
    private static final int WEB_RAMP_TICKS = 30;
    private static final double WEB_ARRIVE = 1.5;
    private static final double PEARL_THREAT = 4.0;
    private static final double KINSMEN_RANGE = 16.0;
    private static final int SHELL_POTIONS = 3;
    private static final int HEAL_THROW_COOLDOWN = 100;
    private static final double HEAL_RANGE = 12.0;
    private static final float HEAL_HEALTH = 0.5f;
    private static final int SUMMON_SEARCH_TICKS = 5;
    private static final int WAIL_UNSEEN_TICKS = 60;

    private final FactionMember mob;
    private final FactionFloran floran;
    private final List<Summon> summons = new ArrayList<>();
    private @Nullable String keyA;
    private @Nullable String keyB;
    private int keyTicks;
    private boolean keySneak;
    private boolean shellUp;
    private int toggleGap;
    private int gap;
    private int chordGap;
    private int unseenTicks;
    private int phantomSearch;
    private @Nullable LivingEntity tether;
    private int tetherTicks;
    private int shellPotions;
    private int potionCooldown;

    private record Summon(Entity entity, int expires) {
    }

    FactionPowers(FactionMember mob) {
        this.mob = mob;
        this.floran = new FactionFloran(mob);
    }

    FactionFloran floran() {
        return this.floran;
    }

    // A shulk's Hoarder slots carry spare healing potions for its crew.
    void onSpawn() {
        if (this.mob.origin() == FactionOrigin.SHULK) {
            this.shellPotions = SHELL_POTIONS;
        }
    }

    int shellPotions() {
        return this.shellPotions;
    }

    void setShellPotions(int potions) {
        this.shellPotions = potions;
    }

    // A shulk's spare healing potions, dropped with its loot.
    List<ItemStack> leftovers() {
        List<ItemStack> left = new ArrayList<>();
        for (int i = 0; i < this.shellPotions; i++) {
            left.add(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING));
        }
        this.shellPotions = 0;
        return left;
    }

    // Every tick, in or out of a fight: the keys it is holding, its web, and its summons.
    void tickAlways(ServerLevel level) {
        this.toggleGap--;
        this.gap--;
        this.chordGap--;
        this.potionCooldown--;
        if (this.keyTicks > 0) {
            this.keyTicks--;
            this.mob.holdKey(this.keyA);
            if (this.keyB != null) {
                this.mob.holdKey(this.keyB);
            }
            if (this.keySneak) {
                this.mob.holdKey(FactionOriginPowers.SNEAK);
            }
        } else if (this.shellUp) {
            this.mob.holdKey(FactionOriginPowers.SNEAK);
        }
        this.shellUp = false;
        if (this.mob.origin() == FactionOrigin.FLORAN) {
            this.floran.tick(level);
        }
        this.tickWeb(level);
        if (this.phantomSearch > 0 && --this.phantomSearch == 0) {
            this.takePhantoms(level);
        }
        this.summons.removeIf(summon -> {
            if (!summon.entity().isAlive() || summon.entity().isRemoved()) {
                return true;
            }
            if (this.mob.tickCount >= summon.expires() || !this.mob.isAlive()) {
                summon.entity().discard();
                return true;
            }
            return false;
        });
        if (this.mob.tickCount % 20 == 0) {
            this.directSummons(this.mob.getTarget());
        }
    }

    // Holds one or two power keys for a number of ticks, sneaking or not, unless it is already holding some.
    void hold(String key, @Nullable String second, int ticks, boolean sneak) {
        if (this.keyTicks > 0) {
            return;
        }
        this.keyA = key;
        this.keyB = second;
        this.keyTicks = ticks;
        this.keySneak = sneak;
    }

    void tap(String key, boolean sneak) {
        this.hold(key, null, 1, sneak);
    }

    boolean holding() {
        return this.keyTicks > 0;
    }

    // In a fight, while the member is free to act: the origin's active powers.
    void tick(ServerLevel level, LivingEntity target) {
        if (!FactionOrigin.POWERS) {
            return;
        }
        boolean sees = this.mob.getSensing().hasLineOfSight(target);
        this.unseenTicks = sees ? 0 : this.unseenTicks + 1;
        double distance = this.mob.distanceTo(target);
        boolean melee = this.mob.factionClass().melee();
        switch (this.mob.origin()) {
            case ARACHNID -> this.arachnid(target, sees, distance, melee);
            case BEASTFOLK -> this.beastfolk(target, sees, distance, melee);
            case BLAZEBORN -> this.blazeborn(target, sees, distance, melee);
            case ENDERIAN -> this.enderian(target, distance);
            case FISHMAN -> this.fishman(target, sees, distance, melee);
            case FLORAN -> this.floran.fight(level, target);
            case PHANTOM -> this.phantom(level, target, sees, distance);
            case SHULK -> this.shulk(level, target, sees, distance);
            case TRUFFLE -> this.truffle(target, sees, distance);
            default -> {
            }
        }
    }

    // Arachnid: Climbing on the move, Venom up close (one key swaps them), and Master of Webs to tether and reel in a target.
    private void arachnid(LivingEntity target, boolean sees, double distance, boolean melee) {
        boolean venom = !FactionOriginPowers.active(this.mob, CLIMBING);
        boolean wantVenom = melee && distance < 4.0 && sees && !this.mob.horizontalCollision;
        if (venom != wantVenom && this.toggleGap <= 0) {
            this.tap(FactionOriginPowers.SECONDARY, false);
            this.toggleGap = TOGGLE_GAP;
        }
        if (this.tether == null && this.gap <= 0 && sees && distance >= WEB_MIN && distance <= WEB_RANGE
            && FactionOriginPowers.resource(this.mob, SILK) > 0) {
            this.tether = target;
            this.tetherTicks = 0;
            FactionOriginPowers.spend(this.mob, SILK, 1);
            this.gap = WEB_GAP;
        }
    }

    // A web on a creature reels it in, faster after a second, spending silk as it holds.
    private void tickWeb(ServerLevel level) {
        LivingEntity caught = this.tether;
        if (caught == null) {
            return;
        }
        boolean lost = !caught.isAlive() || this.mob.distanceTo(caught) > WEB_RANGE || !this.mob.getSensing().hasLineOfSight(caught);
        if (lost || this.mob.distanceTo(caught) <= WEB_ARRIVE || this.mob.downed() || FactionOriginPowers.resource(this.mob, SILK) <= 0) {
            this.tether = null;
            return;
        }
        this.tetherTicks++;
        if (this.tetherTicks % WEB_SILK_INTERVAL == 0) {
            FactionOriginPowers.spend(this.mob, SILK, 1);
        }
        double ramp = Mth.clamp((this.tetherTicks - WEB_RAMP_DELAY) / (double) WEB_RAMP_TICKS, 0.0, 1.0);
        double speed = Mth.lerp(ramp, WEB_REEL_SPEED, WEB_MAX_REEL_SPEED);
        Vec3 pull = this.mob.getEyePosition().subtract(caught.position()).normalize().scale(speed);
        caught.setDeltaMovement(caught.getDeltaMovement().scale(0.5).add(pull));
        caught.needsSync = true;
    }

    // Beastfolk: claws out for the fight, and Acrobatics' charged leap, in at a target for fighters and away from one for archers.
    private void beastfolk(LivingEntity target, boolean sees, double distance, boolean melee) {
        if (!FactionOriginPowers.active(this.mob, CLAWS) && this.toggleGap <= 0) {
            this.tap(FactionOriginPowers.SECONDARY, false);
            this.toggleGap = TOGGLE_GAP;
            return;
        }
        boolean leap = melee ? distance >= 4.0 && distance <= 10.0 : distance <= 3.0;
        if (leap && sees && this.mob.onGround() && !this.mob.isPassenger() && !this.holding()) {
            Vec3 away = this.mob.position().multiply(2.0, 1.0, 2.0).subtract(target.position().multiply(1.0, 0.0, 1.0));
            if (melee) {
                this.aim(target.getEyePosition(), -10.0f);
            } else {
                this.aim(new Vec3(away.x, this.mob.getEyeY(), away.z), -15.0f);
            }
            this.hold(FactionOriginPowers.PRIMARY, null, LEAP_CHARGE, false);
        }
    }

    // Blazeborn: Burning Wrath to fight ablaze, Firecharged's three fireballs at range, and the wildfire shield chord now and then.
    private void blazeborn(LivingEntity target, boolean sees, double distance, boolean melee) {
        if (!sees || this.holding()) {
            return;
        }
        this.aim(target.getEyePosition(), 0.0f);
        if (melee && distance < 6.0 && !this.mob.isOnFire()) {
            this.tap(FactionOriginPowers.PRIMARY, false);
        } else if (distance >= 8.0 && distance <= 20.0 && this.chordGap <= 0) {
            this.hold(FactionOriginPowers.PRIMARY, FactionOriginPowers.SECONDARY, 3, false);
            this.chordGap = CHORD_GAP;
        } else if (distance >= 5.0 && distance <= 24.0) {
            this.tap(FactionOriginPowers.SECONDARY, false);
        }
    }

    // Enderian: an ender pearl thrown away from a foe that closes in.
    private void enderian(LivingEntity target, double distance) {
        if (distance > PEARL_THREAT || this.holding()) {
            return;
        }
        Vec3 away = this.mob.position().subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize();
        Vec3 spot = this.mob.getEyePosition().add(away.scale(10.0));
        this.aim(spot, -25.0f);
        this.tap(FactionOriginPowers.PRIMARY, false);
    }

    // Fishman: Guardian Beam held on a target in sight, and a dash at one a little way off.
    private void fishman(LivingEntity target, boolean sees, double distance, boolean melee) {
        if (!sees) {
            return;
        }
        this.aim(target.getEyePosition(), 0.0f);
        if (distance <= 16.0) {
            this.mob.holdKey(FactionOriginPowers.PRIMARY);
        }
        if (melee && distance >= 6.0 && distance <= 12.0 && this.gap <= 0) {
            this.tap(FactionOriginPowers.SECONDARY, false);
            this.gap = TOGGLE_GAP;
        }
    }

    // Phantom: Summon Phantoms out of the sun, and Spectral Wail when the target has slipped out of sight.
    private void phantom(ServerLevel level, LivingEntity target, boolean sees, double distance) {
        if (this.holding()) {
            return;
        }
        if (sees && distance <= WEB_RANGE && FactionOriginPowers.resource(this.mob, SOUL) >= 40) {
            this.hold(FactionOriginPowers.PRIMARY, null, GESTURE_HOLD, true);
            this.phantomSearch = GESTURE_HOLD + SUMMON_SEARCH_TICKS;
        } else if (this.unseenTicks >= WAIL_UNSEEN_TICKS) {
            this.hold(FactionOriginPowers.SECONDARY, null, GESTURE_HOLD, false);
            this.unseenTicks = 0;
        }
    }

    // Takes the phantoms its summon just called into its crew.
    private void takePhantoms(ServerLevel level) {
        AABB area = this.mob.getBoundingBox().inflate(4.0, 6.0, 4.0);
        for (Phantom phantom : level.getEntitiesOfClass(Phantom.class, area, phantom -> phantom.tickCount <= GESTURE_HOLD + SUMMON_SEARCH_TICKS)) {
            this.track(phantom);
        }
    }

    // Shulk: Shulker Bullets at a target in view, Sturdy Skin's shell grown while it holds at range, and its spare potions for hurt crewmates.
    private void shulk(ServerLevel level, LivingEntity target, boolean sees, double distance) {
        if (sees && distance > 3.0 && distance <= WEB_RANGE && this.gap <= 0 && !this.holding()) {
            this.aim(target.getEyePosition(), 0.0f);
            this.tap(FactionOriginPowers.SECONDARY, false);
            this.gap = BULLET_GAP;
        }
        this.shellUp = distance > 8.0 && !this.mob.isAggressive();
        if (this.shellPotions <= 0 || this.potionCooldown > 0) {
            return;
        }
        LivingEntity hurt = level.getEntitiesOfClass(
                LivingEntity.class,
                this.mob.getBoundingBox().inflate(HEAL_RANGE),
                other -> other != this.mob
                    && FactionCleric.ally(this.mob, other)
                    && other.getHealth() < other.getMaxHealth() * HEAL_HEALTH
                    && this.mob.getSensing().hasLineOfSight(other)
            )
            .stream()
            .min(Comparator.comparingDouble(other -> other.getHealth() / other.getMaxHealth()))
            .orElse(null);
        if (hurt != null) {
            this.mob.cleric().throwHealing(level, hurt);
            this.shellPotions--;
            this.potionCooldown = HEAL_THROW_COOLDOWN;
        }
    }

    // Truffle: Drifting spores drawn for the fight, and Mind Haze loosed when the foe is close.
    private void truffle(LivingEntity target, boolean sees, double distance) {
        if (this.holding()) {
            return;
        }
        if (FactionOriginPowers.resource(this.mob, SPORES_DRAWN) == 0 && this.toggleGap <= 0) {
            this.tap(FactionOriginPowers.SECONDARY, false);
            this.toggleGap = TOGGLE_GAP;
        } else if (sees && distance < 5.0) {
            this.tap(FactionOriginPowers.PRIMARY, false);
        }
    }

    // Piglin: Bastion Kin, so nearby piglins and brutes rally against whatever hurts it.
    void rallyKin(ServerLevel level, LivingEntity attacker) {
        if (this.mob.origin() != FactionOrigin.PIGLIN || attacker instanceof AbstractPiglin) {
            return;
        }
        for (AbstractPiglin piglin : level.getEntitiesOfClass(AbstractPiglin.class, this.mob.getBoundingBox().inflate(KINSMEN_RANGE))) {
            piglin.setTarget(attacker);
        }
    }

    // A melee hit, critical at the chance its origin's crit powers give, as Critical Strike rolls it for a player.
    boolean hit(ServerLevel level, Entity target, Predicate<Entity> strike) {
        AttributeInstance damage = this.mob.getAttribute(Attributes.ATTACK_DAMAGE);
        float chance = FactionOrigin.POWERS && this.mob.origin() != FactionOrigin.NONE ? FactionOriginPowers.critChance(this.mob) : 0.0f;
        boolean crit = this.mob.getRandom().nextFloat() < chance;
        if (damage != null && crit) {
            damage.addTransientModifier(new AttributeModifier(CRIT_MODIFIER, CRIT_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        boolean landed;
        try {
            landed = strike.test(target);
        } finally {
            if (damage != null) {
                damage.removeModifier(CRIT_MODIFIER);
            }
        }
        return landed;
    }

    // Turns body, head and pitch toward a point, so powers that fire along the look go there.
    void aim(Vec3 at, float pitchOffset) {
        Vec3 delta = at.subtract(this.mob.getEyePosition());
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0f;
        float pitch = (float) -(Mth.atan2(delta.y, delta.horizontalDistance()) * Mth.RAD_TO_DEG) + pitchOffset;
        this.mob.setYRot(yaw);
        this.mob.setYHeadRot(yaw);
        this.mob.setYBodyRot(yaw);
        this.mob.setXRot(pitch);
    }

    // Points summons at the member's target, and calls them off a player the member is cowed by unless that player struck them.
    private void directSummons(@Nullable LivingEntity target) {
        for (Summon summon : this.summons) {
            if (!(summon.entity() instanceof Mob ally)) {
                continue;
            }
            if (ally.getTarget() instanceof Player player && ally.getLastHurtByMob() != player && this.mob.cowedBy(player)) {
                ally.setTarget(null);
            }
            if (target != null && target.isAlive() && (ally.getTarget() == null || this.mob.considersEntityAsAlly(ally.getTarget()))) {
                ally.setTarget(target);
            }
        }
    }

    // Takes a creature its powers brought into the world as one of the crew's summons.
    void track(Entity summoned) {
        summoned.addTag(FactionMember.summonTag(this.mob.faction()));
        if (summoned instanceof Mob ally && this.mob.getTarget() != null) {
            ally.setTarget(this.mob.getTarget());
        }
        this.summons.add(new Summon(summoned, Integer.MAX_VALUE));
    }
}
