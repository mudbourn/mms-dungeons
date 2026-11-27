package info.mudbourn.mmsdungeons.mobs.factions.entity;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Going down instead of dying: a downed member lies on the ground until a crewmate within 64 blocks revives it, gets itself up once when no crewmate is near, and bleeds out the next time.
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
    private static final double CRAWL_SPEED = 0.5;
    private static final double MEET_RANGE = 16.0;
    private static final double FLEE_RANGE = 6.0;

    private final FactionMember mob;
    private int downedTicks;
    private int aloneTicks;
    private int reviveProgress;
    private boolean selfRevived;
    private @Nullable DamageSource cause;
    private @Nullable FactionMember helper;

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
        this.mob.setAction(FactionMember.ACTION_NONE);
        this.mob.setRollTicks(0);
        this.mob.flight().land();
        this.mob.stopRiding();
        this.mob.getNavigation().stop();
        this.mob.setAggressive(false);
        this.mob.setDowned(true);
        this.mob.setCrawling(true);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, this.mob.getX(), this.mob.getY(0.5), this.mob.getZ(), 8, 0.3, 0.2, 0.3, 0.1);
        return true;
    }

    // Counts down while downed: waits for its helper, gets up alone once, and gives up when it already has.
    void tick(ServerLevel level) {
        if (!this.mob.downed()) {
            return;
        }
        this.downedTicks++;
        if (!this.mob.crawling()) {
            this.mob.setCrawling(true);
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
        if (this.mob.tickCount % 20 == 0) {
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, this.mob.getX(), this.mob.getY(0.3), this.mob.getZ(), 2, 0.3, 0.1, 0.3, 0.05);
        }
    }

    // One tick of a crewmate's help; the member stands once the help is done.
    void help(ServerLevel level, FactionMember reviver) {
        if (++this.reviveProgress % 10 == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.mob.getX(), this.mob.getY(0.4), this.mob.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
            reviver.swing(InteractionHand.MAIN_HAND, reviver.getMainHandItem().getAttackAnimation(), true);
        }
        if (this.reviveProgress >= REVIVE_TICKS) {
            this.revive(level);
        }
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
        level.sendParticles(ParticleTypes.HEART, this.mob.getX(), this.mob.getY(1.0), this.mob.getZ(), 5, 0.4, 0.3, 0.4, 0.0);
        this.mob.playSound(SoundEvents.PLAYER_LEVELUP, 0.6f, 1.5f);
    }

    // Dies of its wounds, credited to whoever downed it.
    private void bleedOut(ServerLevel level) {
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

    // Holds a downed member on the ground: it crawls to meet its helper, or away from whatever downed it.
    static final class DownedGoal extends Goal {

        private final FactionMember mob;

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
            if (helper != null && this.mob.distanceTo(helper) < MEET_RANGE) {
                this.mob.getLookControl().setLookAt(helper);
                if (this.mob.distanceTo(helper) > REVIVE_REACH) {
                    this.mob.getNavigation().moveTo(helper, CRAWL_SPEED);
                } else {
                    this.mob.getNavigation().stop();
                }
            } else if (threat != null && threat.isAlive() && this.mob.distanceTo(threat) < FLEE_RANGE) {
                this.mob.getLookControl().setLookAt(threat);
                Vec3 away = this.mob.position().add(this.mob.position().subtract(threat.position()).normalize().scale(4.0));
                this.mob.getNavigation().moveTo(away.x, away.y, away.z, CRAWL_SPEED);
            } else {
                this.mob.getNavigation().stop();
            }
        }
    }

    // Sends a member to the downed crewmate that picked it as helper, and kneels to revive it.
    static final class ReviveGoal extends Goal {

        private final FactionMember mob;
        private @Nullable FactionMember patient;

        ReviveGoal(FactionMember mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.mob.downed() || this.mob.tickCount % SEARCH_TICKS != 0) {
                return false;
            }
            this.patient = this.mob.level().getEntitiesOfClass(
                    FactionMember.class,
                    this.mob.getBoundingBox().inflate(HELP_RANGE),
                    member -> member.downed() && member.isAlive() && member.downedState().helper() == this.mob
                )
                .stream()
                .min(Comparator.comparingDouble(this.mob::distanceToSqr))
                .orElse(null);
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
        }

        @Override
        public void stop() {
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
                this.mob.getNavigation().moveTo(patient, 1.2);
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
