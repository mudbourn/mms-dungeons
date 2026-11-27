package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.mobs.factions.FactionClass;
import info.mudbourn.mmsdungeons.mobs.factions.FactionWeapons;
import info.mudbourn.mmsdungeons.mobs.swing.MobStrike;
import java.util.EnumSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Fights for every faction class: melee with weapon reach and combos, archers at range, casters through their cast controller, and charged skills.
public class FactionCombatGoal extends Goal {

    private enum Skill {
        NONE,
        BASH,
        THROW,
        SLAM
    }

    private static final float SKILL_CHANCE = 0.04f;
    private static final double CLERIC_RANGE = 9.0;
    private static final float LOW_HEALTH = 0.25f;
    private static final double BASH_RANGE = 3.5;
    private static final double THROW_MIN = 4.0;
    private static final double THROW_MAX = 16.0;
    private static final double SLAM_RADIUS = 3.0;
    private static final int WEAPON_AWAY_TICKS = 50;
    private static final int BOW_DRAW_TICKS = 20;
    private static final int STRAFE_MIN = 20;
    private static final double SEE_DOT = 0.2;
    private static final double ARCHER_MIN = 6.0;
    private static final double ARCHER_MAX = 16.0;
    private static final int CROSSBOW_AIM_TICKS = 15;
    private static final double PILOT_REACH = 3.0;
    private static final double PILOT_RUN_DISTANCE = 8.0;
    private static final int PILOT_STRIKE_TICKS = 20;
    private static final int CROSSBOW_RELOAD_TICKS = 10;
    private static final float BOW_VELOCITY = 1.6f;
    private static final float CROSSBOW_VELOCITY = 3.15f;
    private static final int GUARD_MIN = 10;
    private static final int GUARD_MAX = 30;
    private static final int GUARD_CALM = 6;
    private static final int GUARD_REST = 20;
    private static final double GUARD_RANGE = 4.5;
    private static final double PROJECTILE_RANGE = 10.0;
    private static final double AIM_DOT = 0.9;
    private static final double SIDEARM_DRAW = 6.0;
    private static final double SIDEARM_HOLSTER = 4.0;
    private static final int SIDEARM_CALM_TICKS = 40;

    private final FactionMember mob;
    private int attackCooldown;
    private int combo;
    private Skill skill = Skill.NONE;
    private int skillTicks;
    private int skillCooldown = 60;
    private int bowTicks;
    private int strafeTicks;
    private boolean strafeRight;
    private int hitDelay;
    private FactionWeapons.@Nullable Swing pendingSwing;
    private boolean pendingOffHand;
    private int guardTicks;
    private int guardRest;
    private int calmTicks;
    private int sidearmCalm;

    public FactionCombatGoal(FactionMember mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && !this.mob.downed();
    }

    @Override
    public void start() {
        this.mob.setAggressive(true);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.setSprinting(false);
        this.mob.holsterSidearm();
        this.mob.stopUsingItem();
        this.mob.caster().stop();
        this.mob.setAction(FactionMember.ACTION_NONE);
        this.mob.getNavigation().stop();
        this.skill = Skill.NONE;
        this.bowTicks = 0;
        this.hitDelay = 0;
        this.pendingSwing = null;
        this.guardTicks = 0;
        this.guardRest = 0;
        this.calmTicks = 0;
        this.mob.tactics().reset();
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !(this.mob.level() instanceof ServerLevel level)) {
            return;
        }
        this.attackCooldown--;
        this.skillCooldown--;
        if (!this.mob.tactics().rolling()) {
            this.look(target);
        }
        if (this.hitDelay > 0 && --this.hitDelay == 0) {
            this.landHit(level, target);
        }
        if (this.skill != Skill.NONE) {
            this.tickSkill(level, target);
            return;
        }
        if (this.mob.piloting()) {
            this.tickPilot(level, target);
            return;
        }
        if (this.mob.armless()) {
            this.mob.powers().tick(level, target);
            return;
        }
        if (this.mob.tactics().tick(level, target)) {
            return;
        }
        this.mob.powers().tick(level, target);
        FactionClass type = this.mob.factionClass();
        if (this.updateSidearm(target)) {
            if (type == FactionClass.CLERIC) {
                this.mob.cleric().tick(level);
            }
            this.tickArcher(target);
            return;
        }
        if (type.melee()) {
            this.tickMelee(level, target);
        } else if (type == FactionClass.ARCHER) {
            this.tickArcher(target);
        } else if (type == FactionClass.CLERIC) {
            this.tickCleric(level, target);
        } else {
            this.tickCaster(level, target);
        }
    }

    // Draws the sidearm against a target that fights from range or is out of reach, and puts it away once the target is close or no longer needs it.
    private boolean updateSidearm(LivingEntity target) {
        double distance = this.mob.distanceTo(target);
        boolean needed = this.needsRange(target);
        if (this.mob.sidearmDrawn()) {
            this.sidearmCalm = needed ? 0 : this.sidearmCalm + 1;
            if (distance < SIDEARM_HOLSTER || this.sidearmCalm > SIDEARM_CALM_TICKS) {
                this.mob.holsterSidearm();
                this.bowTicks = 0;
                return false;
            }
            return true;
        }
        if (this.mob.hasSidearm() && needed && distance > SIDEARM_DRAW && !this.mob.weaponThrown() && this.skill == Skill.NONE) {
            this.mob.drawSidearm();
            this.sidearmCalm = 0;
            this.bowTicks = 0;
            return true;
        }
        return false;
    }

    private boolean needsRange(LivingEntity target) {
        Path path = this.mob.getNavigation().getPath();
        boolean cutOff = path != null && !path.canReach() && target.getY() - this.mob.getY() > 1.5;
        boolean aloft = !target.onGround() && !target.isInWater() && target.getY() - this.mob.getY() > 3.0;
        return cutOff || aloft || rangedFighter(target);
    }

    // Whether a creature fights from range: a ranged weapon or staff in hand, or a kind that shoots or casts.
    private static boolean rangedFighter(LivingEntity target) {
        if (target instanceof FactionMember member) {
            return !member.factionClass().melee() || member.sidearmDrawn();
        }
        boolean armed = rangedItem(target.getMainHandItem()) || rangedItem(target.getOffhandItem());
        if (target instanceof Player) {
            return armed;
        }
        boolean shooter = target instanceof RangedAttackMob
            && !(target instanceof AbstractSkeleton || target instanceof Drowned || target instanceof AbstractPiglin);
        return armed || shooter || target instanceof Blaze || target instanceof Ghast || target instanceof Shulker
            || target instanceof Breeze || target instanceof SpellcasterIllager;
    }

    private static boolean rangedItem(ItemStack stack) {
        if (stack.getItem() instanceof ProjectileWeaponItem || stack.getItem() instanceof TridentItem) {
            return true;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.contains("staff") || path.contains("wand");
    }

    // Watches the target while it can see it; out of sight it looks where it is going, as a player hunting someone would.
    private void look(LivingEntity target) {
        Path path = this.mob.getNavigation().getPath();
        if (this.mob.getSensing().hasLineOfSight(target) || path == null || path.isDone()) {
            this.mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
            return;
        }
        Vec3 next = path.getNextEntityPos(this.mob);
        this.mob.getLookControl().setLookAt(next.x, this.mob.getEyeY(), next.z, 30.0f, 30.0f);
    }

    // Piloting a Grovekeeper: steers it at the target, runs it over open ground, and strikes through it.
    private void tickPilot(ServerLevel level, LivingEntity target) {
        this.mob.powers().tick(level, target);
        Entity golem = this.mob.getVehicle();
        if (golem == null) {
            return;
        }
        double distance = golem.distanceTo(target);
        double reach = (golem.getBbWidth() + target.getBbWidth()) * 0.5 + PILOT_REACH;
        this.mob.setSprinting(distance > PILOT_RUN_DISTANCE);
        if (distance > reach * 0.8) {
            this.mob.getNavigation().moveTo(target, 1.0);
        } else {
            this.mob.getNavigation().stop();
        }
        if (distance <= reach && this.attackCooldown <= 0) {
            FactionGrovekeeper.strike(this.mob, target);
            this.attackCooldown = PILOT_STRIKE_TICKS;
        }
    }

    private void tickMelee(ServerLevel level, LivingEntity target) {
        if (this.mob.flight().lancer() && this.mob.flight().airborne()) {
            return;
        }
        double distance = this.mob.distanceTo(target);
        ItemStack main = this.mob.getMainHandItem();
        double reach = FactionWeapons.reach(main) + target.getBbWidth() * 0.5;
        if (distance > reach * 0.8) {
            Vec3 approach = this.mob.tactics().approachPoint(level, target);
            this.mob.getNavigation().moveTo(approach.x, approach.y, approach.z, 1.0);
        } else if (this.attackCooldown > 4) {
            this.mob.getNavigation().stop();
            this.mob.getMoveControl().strafe(-0.2f, 0.45f * this.mob.tactics().flankSide());
        } else {
            this.mob.getNavigation().stop();
        }
        boolean sees = this.mob.getSensing().hasLineOfSight(target);
        if (this.skillCooldown <= 0 && sees && this.mob.getRandom().nextFloat() < SKILL_CHANCE) {
            boolean lowPlayer = target instanceof Player && target.getHealth() <= target.getMaxHealth() * LOW_HEALTH;
            if (lowPlayer && distance >= THROW_MIN && distance <= THROW_MAX && !main.isEmpty()) {
                this.begin(Skill.THROW, 15);
                return;
            }
            if (this.mob.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS) && distance < BASH_RANGE) {
                this.begin(Skill.BASH, 12);
                return;
            }
            if (FactionWeapons.heavy(main) && distance < reach + 1.0) {
                this.begin(Skill.SLAM, 20);
                return;
            }
        }
        boolean guarding = this.tickGuard(level, target, distance, reach);
        if (!guarding && distance <= reach && this.attackCooldown <= 0 && sees && !this.mob.weaponThrown()) {
            this.swingAt(level, target);
        }
    }

    // A shield user raises its shield when the target winds up or a projectile closes in, holds it briefly, then lowers it to attack.
    private boolean tickGuard(ServerLevel level, LivingEntity target, double distance, double reach) {
        if (!this.mob.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)) {
            return false;
        }
        boolean raised = this.mob.isUsingItem() && this.mob.getUseItem().has(DataComponents.BLOCKS_ATTACKS);
        if (!raised) {
            this.guardTicks = 0;
            this.calmTicks = 0;
            if (this.guardRest > 0) {
                this.guardRest--;
            } else if (this.attackCooldown > 3 && this.threatened(level, target, distance)) {
                this.mob.startUsingItem(InteractionHand.OFF_HAND);
                this.guardTicks = 0;
                return true;
            }
            return false;
        }
        this.guardTicks++;
        this.calmTicks = this.threatened(level, target, distance) ? 0 : this.calmTicks + 1;
        boolean readyToSwing = this.attackCooldown <= 0 && distance <= reach;
        boolean held = this.guardTicks >= GUARD_MIN;
        if (this.guardTicks >= GUARD_MAX || held && (this.calmTicks >= GUARD_CALM || readyToSwing)) {
            this.lowerGuard();
            return false;
        }
        return true;
    }

    private void lowerGuard() {
        this.mob.stopUsingItem();
        this.guardTicks = 0;
        this.calmTicks = 0;
        this.guardRest = GUARD_REST;
    }

    private boolean threatened(ServerLevel level, LivingEntity target, double distance) {
        if (target.isSwinging() && distance < GUARD_RANGE) {
            return true;
        }
        ItemStack drawn = target.getUseItem();
        boolean aiming = target.isUsingItem() && (drawn.getItem() instanceof BowItem || drawn.getItem() instanceof CrossbowItem);
        if (aiming) {
            Vec3 toMob = this.mob.getEyePosition().subtract(target.getEyePosition()).normalize();
            if (target.getLookAngle().dot(toMob) > AIM_DOT) {
                return true;
            }
        }
        return this.mob.tickCount % 2 == 0 && this.projectileComing(level);
    }

    private boolean projectileComing(ServerLevel level) {
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, this.mob.getBoundingBox().inflate(PROJECTILE_RANGE))) {
            if (projectile.getOwner() == this.mob || projectile.getOwner() != null && this.mob.considersEntityAsAlly(projectile.getOwner())) {
                continue;
            }
            // Only a projectile in front of it can be seen coming.
            if (this.mob.getLookAngle().dot(projectile.position().subtract(this.mob.getEyePosition()).normalize()) < SEE_DOT) {
                continue;
            }
            Vec3 motion = projectile.getDeltaMovement();
            Vec3 toMob = this.mob.getBoundingBox().getCenter().subtract(projectile.position());
            if (motion.lengthSqr() > 0.04 && motion.dot(toMob) > 0.0 && motion.normalize().dot(toMob.normalize()) > AIM_DOT) {
                return true;
            }
        }
        return false;
    }

    // Lands a Better Combat swing once its upswing has played, through the weapon's hitbox, so a target that steps or rolls clear is missed.
    private void landHit(ServerLevel level, LivingEntity target) {
        FactionWeapons.Swing swing = this.pendingSwing;
        this.pendingSwing = null;
        boolean free = this.skill == Skill.NONE && this.mob.rollTicks() == 0 && !this.mob.consuming();
        if (swing != null && free && !this.mob.weaponThrown()) {
            MobStrike.land(level, this.mob, target, swing, this.pendingOffHand, this.mob::swingEnemy);
        }
    }

    // One swing of the combo, alternating hands when dual wielding: a Better Combat attack when the weapon has one, else a plain hit.
    private void swingAt(ServerLevel level, LivingEntity target) {
        boolean dual = !this.mob.getOffhandItem().isEmpty() && !this.mob.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS);
        boolean offHand = dual && this.combo % 2 == 1;
        InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = this.mob.getItemInHand(hand);
        ItemStack other = this.mob.getItemInHand(offHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        int interval = this.attackInterval();
        FactionWeapons.Swing swing = FactionWeapons.swing(held, other, offHand, this.combo);
        if (swing == null) {
            byte style = FactionWeapons.style(this.combo);
            this.mob.setAction(offHand ? (byte) (style | FactionMember.OFF_HAND_FLAG) : style);
            this.mob.swing(hand, held.getAttackAnimation(), true);
            this.mob.doHurtTarget(level, target);
        } else {
            byte animatedHand = swing.twoHanded() ? FactionMember.SWING_TWO_HANDED : offHand ? FactionMember.SWING_OFF_HAND : FactionMember.SWING_MAIN_HAND;
            this.mob.setAction(FactionMember.ACTION_NONE);
            this.mob.playSwing(swing.attack().animation(), interval, swing.upswing(), animatedHand);
            this.pendingSwing = swing;
            this.pendingOffHand = offHand;
            this.hitDelay = Math.max(1, Math.round(interval * swing.upswing()));
        }
        this.combo++;
        this.attackCooldown = interval;
    }

    // Which way the archer sidesteps: it keeps one way for one to two seconds, then may switch, rather than jittering.
    private float strafeSide() {
        if (--this.strafeTicks <= 0) {
            this.strafeTicks = STRAFE_MIN + this.mob.getRandom().nextInt(STRAFE_MIN);
            if (this.mob.getRandom().nextFloat() < 0.6f) {
                this.strafeRight = !this.strafeRight;
            }
        }
        return this.strafeRight ? 1.0f : -1.0f;
    }

    private int attackInterval() {
        double speed = this.mob.getAttributeValue(Attributes.ATTACK_SPEED);
        return Math.max(8, Mth.ceil(20.0 / Math.max(0.5, speed)));
    }

    private void tickArcher(LivingEntity target) {
        double distance = this.mob.distanceTo(target);
        boolean sees = this.mob.getSensing().hasLineOfSight(target);
        if (!sees || distance > ARCHER_MAX) {
            this.mob.getNavigation().moveTo(target, 1.0);
        } else if (distance < ARCHER_MIN) {
            this.mob.getNavigation().stop();
            this.mob.getMoveControl().strafe(-0.6f, this.strafeSide() * 0.4f);
        } else {
            this.mob.getNavigation().stop();
            this.mob.getMoveControl().strafe(0.0f, this.strafeSide() * 0.35f);
        }
        if (!sees) {
            this.mob.stopUsingItem();
            this.bowTicks = 0;
            return;
        }
        if (this.mob.getMainHandItem().getItem() instanceof CrossbowItem) {
            this.tickCrossbow(target);
            return;
        }
        if (this.attackCooldown > 0) {
            return;
        }
        if (!this.mob.isUsingItem()) {
            this.mob.startUsingItem(InteractionHand.MAIN_HAND);
        }
        if (++this.bowTicks >= BOW_DRAW_TICKS) {
            this.mob.stopUsingItem();
            shootArrow(this.mob, target, BowItem.getPowerForTime(this.bowTicks));
            this.bowTicks = 0;
            this.attackCooldown = 20;
        }
    }

    // A crossbow loads for its full charge time, holds its aim a moment, then fires the loaded bolt.
    private void tickCrossbow(LivingEntity target) {
        ItemStack crossbow = this.mob.getMainHandItem();
        if (!CrossbowItem.isCharged(crossbow)) {
            if (!this.mob.isUsingItem()) {
                if (this.attackCooldown <= 0) {
                    this.mob.startUsingItem(InteractionHand.MAIN_HAND);
                }
            } else if (this.mob.getTicksUsingItem() >= CrossbowItem.getChargeDuration(crossbow, this.mob)) {
                this.mob.releaseUsingItem();
                this.attackCooldown = CROSSBOW_AIM_TICKS;
            }
            return;
        }
        if (this.attackCooldown > 0) {
            return;
        }
        crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        shootArrow(this.mob, target, 1.0f);
        this.attackCooldown = CROSSBOW_RELOAD_TICKS;
    }

    private void tickCaster(ServerLevel level, LivingEntity target) {
        FactionCaster caster = this.mob.caster();
        double distance = this.mob.distanceTo(target);
        double preferred = caster.preferredRange();
        if (caster.busy()) {
            this.mob.getNavigation().stop();
        } else if (distance > preferred + 3.0 || !this.mob.getSensing().hasLineOfSight(target)) {
            this.mob.getNavigation().moveTo(target, 1.0);
        } else if (distance < preferred - 4.0) {
            this.mob.getNavigation().stop();
            this.mob.getMoveControl().strafe(-0.6f, 0.0f);
        } else {
            this.mob.getNavigation().stop();
        }
        caster.tick(level, target);
        if (!caster.busy() && distance < 2.5 && this.attackCooldown <= 0) {
            this.swingAt(level, target);
        }
    }

    // A cleric stands back like a mage and throws buff potions at allies; it only swings when an enemy is on top of it.
    private void tickCleric(ServerLevel level, LivingEntity target) {
        double distance = this.mob.distanceTo(target);
        if (distance < CLERIC_RANGE - 4.0) {
            this.mob.getNavigation().stop();
            this.mob.getMoveControl().strafe(-0.6f, 0.0f);
        } else if (distance > CLERIC_RANGE + 6.0) {
            this.mob.getNavigation().moveTo(target, 1.0);
        } else {
            this.mob.getNavigation().stop();
        }
        this.mob.cleric().tick(level);
        if (distance < 2.5 && this.attackCooldown <= 0) {
            this.swingAt(level, target);
        }
    }

    private void begin(Skill skill, int ticks) {
        this.skill = skill;
        this.skillTicks = ticks;
        this.hitDelay = 0;
        this.pendingSwing = null;
        if (skill == Skill.BASH) {
            if (!this.mob.isUsingItem()) {
                this.mob.startUsingItem(InteractionHand.OFF_HAND);
            }
            this.mob.setAction(FactionMember.ACTION_BASH);
            return;
        }
        this.mob.stopUsingItem();
        this.mob.setAction(FactionMember.ACTION_CHARGE);
    }

    private void tickSkill(ServerLevel level, LivingEntity target) {
        this.mob.getNavigation().stop();
        if (--this.skillTicks > 0) {
            return;
        }
        Skill done = this.skill;
        this.skill = Skill.NONE;
        this.mob.setAction(FactionMember.ACTION_NONE);
        switch (done) {
            case BASH -> this.bash(level, target);
            case THROW -> this.throwWeapon(level, target);
            case SLAM -> this.slam(level);
            default -> {
            }
        }
    }

    // A shield bash knocks the target back hard and slows it.
    private void bash(ServerLevel level, LivingEntity target) {
        this.lowerGuard();
        this.skillCooldown = 100;
        if (this.mob.distanceTo(target) > BASH_RANGE) {
            return;
        }
        this.mob.swing(InteractionHand.OFF_HAND, this.mob.getItemInHand(InteractionHand.OFF_HAND).getAttackAnimation(), true);
        float damage = (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6f;
        DamageSource source = this.mob.damageSources().mobAttack(this.mob);
        if (target.hurtServer(level, source, damage)) {
            Vec3 push = target.position().subtract(this.mob.position()).normalize();
            target.knockback(1.6, -push.x, -push.z, source, damage);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), this.mob);
        }
    }

    private void throwWeapon(ServerLevel level, LivingEntity target) {
        this.skillCooldown = 160;
        ItemStack weapon = this.mob.getMainHandItem();
        if (weapon.isEmpty()) {
            return;
        }
        float damage = (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5f;
        ThrownWeapon thrown = new ThrownWeapon(level, this.mob, weapon.copyWithCount(1), damage);
        double dx = target.getX() - this.mob.getX();
        double dy = target.getY(0.5) - thrown.getY();
        double dz = target.getZ() - this.mob.getZ();
        thrown.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.12, dz, 1.4f, 2.0f);
        level.addFreshEntity(thrown);
        this.mob.swing(InteractionHand.MAIN_HAND, this.mob.getItemInHand(InteractionHand.MAIN_HAND).getAttackAnimation(), true);
        this.mob.throwWeapon(WEAPON_AWAY_TICKS);
    }

    // A heavy weapon slam hurts and lifts every enemy around the fighter.
    private void slam(ServerLevel level) {
        this.skillCooldown = 140;
        this.mob.setAction(FactionMember.ACTION_SLAM);
        this.mob.swing(InteractionHand.MAIN_HAND, this.mob.getItemInHand(InteractionHand.MAIN_HAND).getAttackAnimation(), true);
        float damage = (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.4f;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, this.mob.getBoundingBox().inflate(SLAM_RADIUS))) {
            if (victim == this.mob || this.mob.isAlliedTo(victim)) {
                continue;
            }
            if (victim.hurtServer(level, this.mob.damageSources().mobAttack(this.mob), damage)) {
                victim.push(0.0, 0.5, 0.0);
            }
        }
    }

    public static void shootArrow(FactionMember mob, LivingEntity target, float power) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack weapon = mob.getMainHandItem();
        AbstractArrow arrow = ProjectileUtil.getMobArrow(mob, new ItemStack(Items.ARROW), power, weapon);
        boolean crossbow = weapon.getItem() instanceof CrossbowItem;
        float velocity = crossbow ? CROSSBOW_VELOCITY : BOW_VELOCITY;
        // Leads a moving target by where it will be when the arrow arrives, as a practiced archer does.
        double flight = mob.distanceTo(target) / velocity;
        double dx = target.getX() + (target.getX() - target.xo) * flight - mob.getX();
        double dy = target.getY(0.3333) - arrow.getY();
        double dz = target.getZ() + (target.getZ() - target.zo) * flight - mob.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        double arc = 0.2 * (BOW_VELOCITY / velocity) * (BOW_VELOCITY / velocity);
        arrow.shoot(dx, dy + flat * arc, dz, velocity, 14 - level.getDifficulty().getId() * 4);
        level.playSound(null, mob.blockPosition(), crossbow ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.ARROW_SHOOT, mob.getSoundSource(), 1.0f, 1.0f / (mob.getRandom().nextFloat() * 0.4f + 0.8f));
        level.addFreshEntity(arrow);
    }
}
