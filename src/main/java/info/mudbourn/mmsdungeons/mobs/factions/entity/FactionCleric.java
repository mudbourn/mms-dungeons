package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.mobs.factions.FactionGear;
import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import info.mudbourn.mmsdungeons.relations.MobRelations;
import info.mudbourn.mmsdungeons.relations.Relation;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A cleric's potion work: picks the ally who needs help most and the buff that fits their fight, then throws it.
public final class FactionCleric {

    private static final double SEARCH_RANGE = 12.0;
    private static final int COOLDOWN = 100;
    private static final int FAIRY_COOLDOWN = 70;
    private static final float HURT_HEALTH = 0.6f;
    private static final int LINGER_TICKS = 100;
    private static final double LINGER_RADIUS = 3.0;
    private static final int CLUSTER_FOR_LINGER = 3;

    private final FactionMember mob;
    private int cooldown = 40;
    private @Nullable Vec3 lingerAt;
    private @Nullable Holder<MobEffect> lingerEffect;
    private int lingerAmplifier;
    private int lingerTicks;

    FactionCleric(FactionMember mob) {
        this.mob = mob;
    }

    // Whether the cleric's potions may land on this creature: its own faction, or a kind the relations call allied.
    public static boolean ally(FactionMember cleric, LivingEntity other) {
        if (other == cleric) {
            return true;
        }
        if (!other.isAlive() || other == cleric.getTarget()) {
            return false;
        }
        if (other instanceof Mob mob && mob.getTarget() == cleric) {
            return false;
        }
        if (other instanceof FactionMember member) {
            return member.faction() == cleric.faction() || MobRelations.between(cleric, other) == Relation.ALLIED;
        }
        return MobRelations.between(cleric, other) == Relation.ALLIED;
    }

    public void tick(ServerLevel level) {
        this.tickLinger(level);
        if (--this.cooldown > 0) {
            return;
        }
        Choice choice = this.choose(level);
        if (choice == null) {
            this.cooldown = 20;
            return;
        }
        this.throwAt(level, choice);
        this.cooldown = this.mob.origin() == FactionOrigin.FAIRY ? FAIRY_COOLDOWN : COOLDOWN;
    }

    private record Choice(LivingEntity ally, Holder<MobEffect> effect, Holder<Potion> look, boolean lingering) {
    }

    // Scores each ally in sight and returns the most needed buff, or nothing when everyone is fine.
    private @Nullable Choice choose(ServerLevel level) {
        AABB area = this.mob.getBoundingBox().inflate(SEARCH_RANGE);
        Choice best = null;
        int bestScore = 0;
        for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, area, other -> ally(this.mob, other))) {
            if (ally != this.mob && !this.mob.getSensing().hasLineOfSight(ally)) {
                continue;
            }
            int score = 0;
            Holder<MobEffect> effect = null;
            Holder<Potion> look = null;
            LivingEntity enemy = ally instanceof Mob mob ? mob.getTarget() : null;
            if (ally.getHealth() < ally.getMaxHealth() * HURT_HEALTH) {
                score = 100 - (int) (ally.getHealth() / ally.getMaxHealth() * 100);
                effect = MobEffects.INSTANT_HEALTH;
                look = Potions.HEALING;
            } else if ((ally.isOnFire() || ally.isInLava() || enemy != null && enemy.fireImmune()) && !ally.fireImmune() && !ally.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                score = 45;
                effect = MobEffects.FIRE_RESISTANCE;
                look = Potions.FIRE_RESISTANCE;
            } else if (ally.hurtTime > 0 && enemy != null && !ally.hasEffect(MobEffects.RESISTANCE)) {
                score = 35;
                effect = MobEffects.RESISTANCE;
                look = Potions.TURTLE_MASTER;
            } else if (enemy != null && ally.distanceTo(enemy) < 5.0 && ally != this.mob && !ally.hasEffect(MobEffects.STRENGTH)) {
                score = 30;
                effect = MobEffects.STRENGTH;
                look = Potions.STRENGTH;
            } else if (enemy != null && ally.distanceTo(enemy) > 9.0 && !ally.hasEffect(MobEffects.SPEED)) {
                score = 20;
                effect = MobEffects.SPEED;
                look = Potions.SWIFTNESS;
            } else if (enemy != null && ally.getHealth() < ally.getMaxHealth() && !ally.hasEffect(MobEffects.REGENERATION)) {
                score = 15;
                effect = MobEffects.REGENERATION;
                look = Potions.REGENERATION;
            }
            if (effect != null && score > bestScore) {
                bestScore = score;
                best = new Choice(ally, effect, look, this.clustered(level, ally));
            }
        }
        return best;
    }

    // Whether enough allies stand together around this one to be worth a lingering bottle.
    private boolean clustered(ServerLevel level, LivingEntity ally) {
        AABB area = ally.getBoundingBox().inflate(LINGER_RADIUS);
        return level.getEntitiesOfClass(LivingEntity.class, area, other -> ally(this.mob, other)).size() >= CLUSTER_FOR_LINGER;
    }

    private void throwAt(ServerLevel level, Choice choice) {
        int tier = this.mob.tier();
        int amplifier = tier >= 3 ? 1 : 0;
        int duration = (15 + 10 * Math.min(tier, FactionGear.LUNAR_TIER)) * 20;
        ItemStack bottle = PotionContents.createItemStack(choice.lingering() ? Items.LINGERING_POTION : Items.SPLASH_POTION, choice.look());
        LivingEntity ally = choice.ally();
        if (ally == this.mob) {
            ThrownBuff self = new ThrownBuff(level, this.mob, bottle, choice.effect(), duration, amplifier, choice.lingering());
            self.shoot(0.0, -1.0, 0.0, 0.2f, 0.0f);
            level.addFreshEntity(self);
        } else {
            ThrownBuff potion = new ThrownBuff(level, this.mob, bottle, choice.effect(), duration, amplifier, choice.lingering());
            Vec3 lead = ally.getDeltaMovement();
            double dx = ally.getX() + lead.x - this.mob.getX();
            double dy = ally.getEyeY() - 1.1 - potion.getY();
            double dz = ally.getZ() + lead.z - this.mob.getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            potion.shoot(dx, dy + flat * 0.2, dz, 0.75f, 2.0f);
            level.addFreshEntity(potion);
        }
        this.mob.getLookControl().setLookAt(ally, 30.0f, 30.0f);
        this.mob.swing(InteractionHand.MAIN_HAND, this.mob.getMainHandItem().getAttackAnimation(), true);
        this.mob.playSound(SoundEvents.WITCH_THROW, 1.0f, 0.8f + this.mob.getRandom().nextFloat() * 0.4f);
    }

    // Throws one healing bottle at an ally, as a shulk does with the spare potions its shell carries.
    void throwHealing(ServerLevel level, LivingEntity ally) {
        this.throwAt(level, new Choice(ally, MobEffects.INSTANT_HEALTH, Potions.HEALING, false));
    }

    public void linger(Vec3 at, Holder<MobEffect> effect, int amplifier) {
        this.lingerAt = at;
        this.lingerEffect = effect;
        this.lingerAmplifier = amplifier;
        this.lingerTicks = LINGER_TICKS;
    }

    // Keeps a lingering bottle's spot glowing and tops up allies inside it every second.
    private void tickLinger(ServerLevel level) {
        if (this.lingerAt == null || this.lingerEffect == null) {
            return;
        }
        if (--this.lingerTicks <= 0) {
            this.lingerAt = null;
            return;
        }
        if (this.lingerTicks % 5 == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.lingerAt.x, this.lingerAt.y + 0.2, this.lingerAt.z, 4, LINGER_RADIUS * 0.5, 0.1, LINGER_RADIUS * 0.5, 0.0);
        }
        if (this.lingerTicks % 20 != 0) {
            return;
        }
        AABB area = new AABB(this.lingerAt, this.lingerAt).inflate(LINGER_RADIUS, 1.5, LINGER_RADIUS);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, other -> ally(this.mob, other))) {
            living.addEffect(new MobEffectInstance(this.lingerEffect, 60, this.lingerAmplifier), this.mob);
        }
    }
}
