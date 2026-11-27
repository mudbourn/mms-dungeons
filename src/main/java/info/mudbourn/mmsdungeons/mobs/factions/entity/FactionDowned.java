package info.mudbourn.mmsdungeons.mobs.factions.entity;

import net.minecraft.world.entity.Mob;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Going down instead of dying: a downed member lies on the ground until a crewmate within 64 blocks, or any player crouching within 3 blocks, revives it, gets itself up once when no crewmate is near, and bleeds out the next time. Any damage while downed kills it. Whatever hunted it holds off while it is down, and one just back up hurries to get its other downed crewmates up.
final class FactionDowned {

    private static final double HELP_RANGE = 64.0;
    private static final int SEARCH_TICKS = 10;
    private static final int BLEED_OUT_TICKS = 1200;
    private static final int SELF_REVIVE_TICKS = 120;
    private static final int GIVE_UP_TICKS = 40;
    private static final int REVIVE_TICKS = 80;
    private static final int PLAYER_CREDIT_TICKS = 100;
    private static final float DOWNED_HEALTH = 0.4f;
    private static final float REVIVED_HEALTH = 0.3f;
    private static final double REVIVE_REACH = 1.8;
    private static final double PLAYER_REVIVE_REACH = 3.0;
    private static final double CRAWL_SPEED = 0.5;
    private static final double MEET_RANGE = 16.0;
    private static final double FLEE_RANGE = 6.0;
    private static final int REPATH_TICKS = 20;
    private static final int HELD_TICKS = 5;
    private static final int URGE_TICKS = 600;
    private static final double PAUSE_RANGE = 48.0;

    // AbsolutRevive lays a downed player still on its back, so members lie still beside it rather than crawl.
    private static final boolean LIE_STILL = FabricLoader.getInstance().isModLoaded("absolutrevive");

    private final FactionMember mob;
    private int downedTicks;
    private int aloneTicks;
    private int reviveProgress;
    private int heldTicks;
    private boolean selfRevived;
    private @Nullable DamageSource cause;
    private @Nullable FactionMember helper;
    private int urgeTicks;
    private final List<Mob> paused = new ArrayList<>();

    FactionDowned(FactionMember mob) {
        this.mob = mob;
    }

    // Called in place of dying: true when the member went down instead.
    boolean goDown(DamageSource source) {
        if (this.mob.downed() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !(this.mob.level() instanceof ServerLevel level)) {
            return false;
        }
        this.cause = source;
        this.downedTicks = 0;
        this.aloneTicks = 0;
        this.reviveProgress = 0;
        this.helper = this.findHelper(level);
        this.mob.setHealth(this.mob.getMaxHealth() * DOWNED_HEALTH);
        this.mob.stopUsingItem();
        this.mob.caster().stop();
        this.mob.powers().floran().stop();
        this.mob.setAction(FactionMember.ACTION_NONE);
        this.mob.setRollTicks(0);
        this.mob.flight().land();
        this.mob.stopRiding();
        this.mob.getNavigation().stop();
        this.mob.setAggressive(false);
        this.mob.setDowned(true);
        this.mob.setCrawling(true);
        return true;
    }

    // Counts down while downed: waits for its helper, gets up alone once, and gives up when it already has.
    void tick(ServerLevel level) {
        if (!this.mob.downed()) {
            if (this.urgeTicks > 0) {
                this.urgeTicks--;
            }
            return;
        }
        this.pauseAggro(level);
        this.downedTicks++;
        if (this.heldTicks > 0) {
            this.heldTicks--;
        }
        if (!this.mob.crawling()) {
            this.mob.setCrawling(true);
        }
        Player player = this.crouchingPlayer(level);
        if (player != null) {
            this.aloneTicks = 0;
            this.helpedBy(level, player);
            return;
        }
        if (this.mob.tickCount % SEARCH_TICKS == 0 && !this.canHelp(this.helper)) {
            this.helper = this.findHelper(level);
        }
        this.aloneTicks = this.helper == null ? this.aloneTicks + 1 : 0;
        if (this.helper == null && !this.selfRevived && this.aloneTicks >= SELF_REVIVE_TICKS) {
            this.selfRevived = true;
            this.revive(level);
            return;
        }
        if (this.helper == null && this.selfRevived && this.aloneTicks >= GIVE_UP_TICKS || this.downedTicks >= BLEED_OUT_TICKS) {
            this.bleedOut(level);
            return;
        }
    }

    // One tick of a crewmate's help; the member stands once the help is done.
    void help(ServerLevel level, FactionMember reviver) {
        this.heldTicks = HELD_TICKS;
        if (++this.reviveProgress >= REVIVE_TICKS) {
            this.revive(level);
        }
    }

    // A player crouching over it, as players revive each other, even one who downed it.
    private @Nullable Player crouchingPlayer(ServerLevel level) {
        for (Player player : level.players()) {
            if (player.isShiftKeyDown()
                && player.isAlive()
                && !player.isSpectator()
                && player.distanceToSqr(this.mob) <= PLAYER_REVIVE_REACH * PLAYER_REVIVE_REACH) {
                return player;
            }
        }
        return null;
    }

    // One tick of a player's help, shown on their action bar; once it stands, it thanks them with a crouch and stays friendly with them.
    private void helpedBy(ServerLevel level, Player player) {
        this.heldTicks = HELD_TICKS;
        this.reviveProgress++;
        player.sendOverlayMessage(Component.translatable("message.mms_dungeons.reviving", this.mob.getDisplayName(), Math.min(100, this.reviveProgress * 100 / REVIVE_TICKS)));
        if (this.reviveProgress >= REVIVE_TICKS) {
            this.revive(level);
            this.mob.peace().thank(player);
        }
    }

    // Someone is reviving it right now, so it lies still for them.
    boolean beingHelped() {
        return this.heldTicks > 0;
    }

    @Nullable FactionMember helper() {
        return this.helper;
    }

    boolean selfRevived() {
        return this.selfRevived;
    }

    int downedTicks() {
        return this.downedTicks;
    }

    // Restores a saved downed state.
    void load(boolean downed, int ticks, boolean usedSelfRevive) {
        this.selfRevived = usedSelfRevive;
        this.downedTicks = ticks;
        if (downed) {
            this.mob.setDowned(true);
            this.mob.setCrawling(true);
        }
    }

    private void revive(ServerLevel level) {
        this.mob.setDowned(false);
        this.mob.setHealth(this.mob.getMaxHealth() * REVIVED_HEALTH);
        this.mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        if (this.mob.roomToStand()) {
            this.mob.setCrawling(false);
        }
        this.cause = null;
        this.helper = null;
        this.reviveProgress = 0;
        this.downedTicks = 0;
        this.aloneTicks = 0;
        this.urgeTicks = URGE_TICKS;
        this.resumeAggro();
    }

    // Whether, freshly back on its feet, it still feels the pull to get its downed crewmates up.
    boolean urged() {
        return this.urgeTicks > 0;
    }

    // An urged crewmate takes over as its helper when it has none closer that is free to come.
    void claim(FactionMember reviver) {
        if (!this.canHelp(this.helper) || reviver.distanceToSqr(this.mob) < this.helper.distanceToSqr(this.mob)) {
            this.helper = reviver;
        }
    }

    // Whatever was after it lets it be while it is down, remembered so the fight picks up again once it stands.
    private void pauseAggro(ServerLevel level) {
        for (Mob hunter : level.getEntitiesOfClass(Mob.class, this.mob.getBoundingBox().inflate(PAUSE_RANGE), other -> other.getTarget() == this.mob)) {
            hunter.setTarget(null);
            if (!this.paused.contains(hunter)) {
                this.paused.add(hunter);
            }
        }
    }

    private void resumeAggro() {
        for (Mob hunter : this.paused) {
            if (hunter.isAlive() && hunter.getTarget() == null && hunter.level() == this.mob.level()) {
                hunter.setTarget(this.mob);
            }
        }
        this.paused.clear();
    }

    // Any damage at all finishes a downed member, credited to whoever dealt it.
    void finish(ServerLevel level, DamageSource source) {
        this.cause = source;
        this.bleedOut(level);
    }

    // Dies of its wounds, credited to whoever downed it.
    private void bleedOut(ServerLevel level) {
        this.paused.clear();
        DamageSource source = this.cause != null ? this.cause : this.mob.damageSources().generic();
        if (source.getEntity() instanceof Player player) {
            this.mob.setLastHurtByPlayer(player, PLAYER_CREDIT_TICKS);
        }
        this.mob.setHealth(0.0f);
        this.mob.die(source);
    }

    private @Nullable FactionMember findHelper(ServerLevel level) {
        return level.getEntitiesOfClass(FactionMember.class, this.mob.getBoundingBox().inflate(HELP_RANGE), this::canHelp)
            .stream()
            .min(Comparator.comparingDouble(this.mob::distanceToSqr))
            .orElse(null);
    }

    private boolean canHelp(@Nullable FactionMember other) {
        return other != null
            && other != this.mob
            && other.isAlive()
            && !other.downed()
            && other.faction() == this.mob.faction()
            && other.level() == this.mob.level()
            && other.distanceToSqr(this.mob) <= HELP_RANGE * HELP_RANGE;
    }

    // Holds a downed member on the ground: it lies still with AbsolutRevive, and otherwise crawls to meet its helper or away from whatever downed it.
    static final class DownedGoal extends Goal {

        private final FactionMember mob;
        private int repath;

        DownedGoal(FactionMember mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.mob.downed();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            FactionMember helper = this.mob.downedState().helper();
            LivingEntity threat = this.mob.getTarget();
            if (LIE_STILL || this.mob.downedState().beingHelped()) {
                this.mob.getNavigation().stop();
                if (helper != null) {
                    this.mob.getLookControl().setLookAt(helper);
                }
            } else if (helper != null && this.mob.distanceTo(helper) < MEET_RANGE) {
                this.mob.getLookControl().setLookAt(helper);
                if (this.mob.distanceTo(helper) > REVIVE_REACH) {
                    this.crawlTo(helper.position());
                } else {
                    this.mob.getNavigation().stop();
                }
            } else if (threat != null && threat.isAlive() && this.mob.distanceTo(threat) < FLEE_RANGE && this.fears(threat)) {
                this.mob.getLookControl().setLookAt(threat);
                this.crawlTo(this.mob.position().add(this.mob.position().subtract(threat.position()).normalize().scale(4.0)));
            } else {
                this.mob.getNavigation().stop();
            }
        }

        // Paths again only when the last path is done or has gone stale; a fresh path every tick sets a slow crawler circling its first waypoint.
        private void crawlTo(Vec3 spot) {
            if (--this.repath > 0 && !this.mob.getNavigation().isDone()) {
                return;
            }
            this.repath = REPATH_TICKS;
            this.mob.getNavigation().moveTo(spot.x, spot.y, spot.z, CRAWL_SPEED);
        }

        // It crawls from whoever downed it, but not from a player it has no quarrel with, who may have come to revive it.
        private boolean fears(LivingEntity threat) {
            return !(threat instanceof Player player) || this.mob.provokedBy(player);
        }
    }

    // Sends a member to the downed crewmate that picked it as helper, and kneels to revive it.
    static final class ReviveGoal extends Goal {

        private final FactionMember mob;
        private @Nullable FactionMember patient;
        private int search;

        ReviveGoal(FactionMember mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.mob.downed() || --this.search > 0) {
                return false;
            }
            this.search = reducedTickDelay(SEARCH_TICKS);
            this.patient = this.mob.level().getEntitiesOfClass(
                    FactionMember.class,
                    this.mob.getBoundingBox().inflate(HELP_RANGE),
                    member -> member.downed() && member.isAlive() && (member.downedState().helper() == this.mob
                        || this.mob.downedState().urged() && member.faction() == this.mob.faction() && !member.downedState().beingHelped())
                )
                .stream()
                .min(Comparator.comparingDouble(this.mob::distanceToSqr))
                .orElse(null);
            if (this.patient != null) {
                this.patient.downedState().claim(this.mob);
            }
            return this.patient != null;
        }

        @Override
        public boolean canContinueToUse() {
            FactionMember patient = this.patient;
            return patient != null && !this.mob.downed() && patient.isAlive() && patient.downed() && patient.downedState().helper() == this.mob;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.mob.stopUsingItem();
            this.mob.caster().stop();
            this.mob.setAction(FactionMember.ACTION_NONE);
            this.mob.setReviving(true);
        }

        @Override
        public void stop() {
            this.mob.setReviving(false);
            this.patient = null;
            this.mob.getNavigation().stop();
            if (this.mob.getPose() == Pose.CROUCHING) {
                this.mob.setPose(Pose.STANDING);
            }
        }

        @Override
        public void tick() {
            FactionMember patient = this.patient;
            if (patient == null || !(this.mob.level() instanceof ServerLevel level)) {
                return;
            }
            this.mob.getLookControl().setLookAt(patient);
            if (this.mob.distanceTo(patient) > REVIVE_REACH) {
                if (this.mob.getPose() == Pose.CROUCHING) {
                    this.mob.setPose(Pose.STANDING);
                }
                this.mob.getNavigation().moveTo(patient, 1.0);
                return;
            }
            this.mob.getNavigation().stop();
            if (this.mob.getPose() == Pose.STANDING) {
                this.mob.setPose(Pose.CROUCHING);
            }
            patient.downedState().help(level, this.mob);
        }
    }
}
