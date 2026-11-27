package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.factions.FactionClass;
import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The mob side of each Origins origin's powers, used in a fight the way a player of that origin uses them.
final class FactionPowers {

    private static final Identifier GROVEKEEPER = Identifier.fromNamespaceAndPath("mms_origins", "floran_henchman");
    private static final Identifier CRIT_MODIFIER = MmsDungeons.id("origin_crit");
    private static final Identifier WET_MODIFIER = MmsDungeons.id("water_power");
    private static final double CRIT_BONUS = 0.5;
    private static final double WET_BONUS = 2.0;
    private static final int FIREBALL_COOLDOWN = 200;
    private static final int FIREBALL_VOLLEY = 3;
    private static final int FIREBALL_GAP = 5;
    private static final double FIREBALL_MIN = 5.0;
    private static final double FIREBALL_MAX = 24.0;
    private static final int BEAM_COOLDOWN = 200;
    private static final int BEAM_LOCK_TICKS = 30;
    private static final int BEAM_ARC_DELAY = 10;
    private static final double BEAM_RANGE = 16.0;
    private static final float BEAM_DAMAGE = 4.0f;
    private static final double BEAM_ARC_RADIUS = 3.0;
    private static final float BEAM_ARC_SHARE = 0.75f;
    private static final int GROVEKEEPER_COOLDOWN = 600;
    private static final int GROVEKEEPER_MAX = 2;
    private static final double MOUNT_RANGE = 6.0;
    private static final int HAZE_COOLDOWN = 1200;
    private static final int HAZE_TICKS = 180;
    private static final double HAZE_RADIUS = 5.0;
    private static final float BOSS_HEALTH = 50.0f;
    private static final int SHELL_CHARGE = 15;
    private static final int BULLET_COST = 2;
    private static final int SHELL_REGROW_TICKS = 40;
    private static final int BULLET_GAP = 30;
    private static final double BULLET_RANGE = 24.0;
    private static final int DODGE_COOLDOWN = 200;
    private static final float DODGE_CHANCE = 0.3f;
    private static final int SHELL_POTIONS = 3;
    private static final int HEAL_THROW_COOLDOWN = 100;
    private static final double HEAL_RANGE = 12.0;
    private static final float HEAL_HEALTH = 0.5f;
    private static final int WEB_COOLDOWN = 160;
    private static final double WEB_MIN = 4.0;
    private static final double WEB_MAX = 24.0;
    private static final float VENOM_CHANCE = 0.2f;
    private static final int VENOM_LULL = 100;
    private static final int PHANTOM_COOLDOWN = 600;
    private static final int PHANTOM_LIFE = 600;
    private static final int WAIL_COOLDOWN = 600;
    private static final int WAIL_UNSEEN_TICKS = 60;
    private static final double WAIL_RANGE = 32.0;
    private static final int PEARL_COOLDOWN = 60;
    private static final double PEARL_THREAT = 4.0;
    private static final int POUNCE_COOLDOWN = 80;
    private static final double KINSMEN_RANGE = 16.0;
    private static final double GILDED_TOUGHNESS = 2.0;

    private final FactionMember mob;
    private final List<Summon> summons = new ArrayList<>();
    private int cooldown = 40;
    private int secondCooldown = 40;
    private int volley;
    private int volleyGap;
    private @Nullable LivingEntity beamTarget;
    private int beamTicks;
    private @Nullable LivingEntity arcCenter;
    private int arcTicks;
    private @Nullable Vec3 hazeAt;
    private int hazeTicks;
    private int shellCharge = SHELL_CHARGE;
    private int shellPotions;
    private int dodgeCooldown;
    private int venomLull;
    private int unseenTicks;

    private record Summon(Entity entity, int expires) {
    }

    FactionPowers(FactionMember mob) {
        this.mob = mob;
    }

    // Gear and lasting stats that come with the origin when it is set at spawn.
    void onSpawn() {
        FactionOrigin origin = this.mob.origin();
        if (origin == FactionOrigin.SHULK) {
            this.shellPotions = SHELL_POTIONS;
        }
        if (origin == FactionOrigin.PIGLIN) {
            AttributeInstance toughness = this.mob.getAttribute(Attributes.ARMOR_TOUGHNESS);
            Identifier id = MmsDungeons.id("gilded_bearing");
            if (toughness != null && !toughness.hasModifier(id)) {
                toughness.addPermanentModifier(new AttributeModifier(id, GILDED_TOUGHNESS, AttributeModifier.Operation.ADD_VALUE));
            }
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

    // Every tick, in or out of a fight: cooldowns, lingering effects and summons that have run their time.
    void tickAlways(ServerLevel level) {
        this.cooldown--;
        this.secondCooldown--;
        this.dodgeCooldown--;
        this.venomLull--;
        if (this.mob.origin() == FactionOrigin.SHULK && this.mob.tickCount % SHELL_REGROW_TICKS == 0) {
            this.shellCharge = Math.min(SHELL_CHARGE, this.shellCharge + 1);
        }
        this.tickArc(level);
        this.tickHaze(level);
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
        LivingEntity target = this.mob.getTarget();
        if (this.mob.tickCount % 20 == 0 && target != null && target.isAlive()) {
            for (Summon summon : this.summons) {
                if (summon.entity() instanceof Mob ally && (ally.getTarget() == null || this.mob.considersEntityAsAlly(ally.getTarget()))) {
                    ally.setTarget(target);
                }
            }
        }
    }

    // In a fight, while the member is free to act: the origin's active powers.
    void tick(ServerLevel level, LivingEntity target) {
        boolean sees = this.mob.getSensing().hasLineOfSight(target);
        this.unseenTicks = sees ? 0 : this.unseenTicks + 1;
        double distance = this.mob.distanceTo(target);
        switch (this.mob.origin()) {
            case BLAZEBORN -> this.fireballs(level, target, sees, distance);
            case FISHMAN -> this.beam(level, target, sees, distance);
            case FLORAN -> this.grovekeepers(level);
            case TRUFFLE -> this.spores(level, sees, distance);
            case SHULK -> this.shell(level, target, sees, distance);
            case ARACHNID -> this.web(level, target, sees, distance);
            case PHANTOM -> this.phantom(level, target, sees, distance);
            case ENDERIAN -> this.pearl(level, target, distance);
            case BEASTFOLK -> this.pounce(level, target, sees, distance);
            default -> {
            }
        }
    }

    // Blazeborn: three blaze fireballs in a row, slightly scattered, every 10 seconds.
    private void fireballs(ServerLevel level, LivingEntity target, boolean sees, double distance) {
        if (this.volley > 0) {
            if (--this.volleyGap <= 0 && sees) {
                Vec3 eye = this.mob.getEyePosition();
                double spread = Math.sqrt(distance) * 0.3;
                Vec3 aim = new Vec3(
                    target.getX() - eye.x + this.mob.getRandom().triangle(0.0, spread),
                    target.getY(0.5) - eye.y,
                    target.getZ() - eye.z + this.mob.getRandom().triangle(0.0, spread)
                );
                SmallFireball fireball = new SmallFireball(level, this.mob, aim.normalize());
                fireball.setPos(eye.x, eye.y - 0.2, eye.z);
                level.addFreshEntity(fireball);
                this.mob.playSound(SoundEvents.BLAZE_SHOOT, 1.0f, 1.0f);
                this.volley--;
                this.volleyGap = FIREBALL_GAP;
            }
            return;
        }
        if (this.cooldown <= 0 && sees && distance >= FIREBALL_MIN && distance <= FIREBALL_MAX) {
            this.volley = FIREBALL_VOLLEY;
            this.volleyGap = 0;
            this.cooldown = FIREBALL_COOLDOWN;
        }
    }

    // Fishman: Guardian Beam locks on for 1.5 seconds, strikes through armor, then arcs to enemies around the target.
    private void beam(ServerLevel level, LivingEntity target, boolean sees, double distance) {
        if (this.beamTarget != null) {
            if (this.beamTarget != target || !sees || distance > BEAM_RANGE) {
                this.beamTarget = null;
                return;
            }
            if (this.beamTicks % 2 == 0) {
                this.line(level, this.mob.getEyePosition(), target.getEyePosition(), ParticleTypes.BUBBLE_POP);
            }
            if (++this.beamTicks >= BEAM_LOCK_TICKS) {
                target.hurtServer(level, this.mob.damageSources().indirectMagic(this.mob, this.mob), BEAM_DAMAGE);
                this.line(level, this.mob.getEyePosition(), target.getEyePosition(), ParticleTypes.ELECTRIC_SPARK);
                this.arcCenter = target;
                this.arcTicks = BEAM_ARC_DELAY;
                this.beamTarget = null;
                this.cooldown = BEAM_COOLDOWN;
            }
            return;
        }
        if (this.cooldown <= 0 && sees && distance <= BEAM_RANGE) {
            this.beamTarget = target;
            this.beamTicks = 0;
            this.mob.playSound(SoundEvents.GUARDIAN_ATTACK, 1.0f, 1.2f);
        }
    }

    private void tickArc(ServerLevel level) {
        LivingEntity center = this.arcCenter;
        if (center == null || --this.arcTicks > 0) {
            return;
        }
        this.arcCenter = null;
        AABB area = center.getBoundingBox().inflate(BEAM_ARC_RADIUS);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, area, other -> other != center && this.mob.swingEnemy(other))) {
            this.line(level, center.getEyePosition(), other.getEyePosition(), ParticleTypes.ELECTRIC_SPARK);
            other.hurtServer(level, this.mob.damageSources().indirectMagic(this.mob, this.mob), BEAM_DAMAGE * BEAM_ARC_SHARE);
        }
    }

    // Floran: raises up to two Grovekeepers that fight for its faction, and rides one into the fight.
    private void grovekeepers(ServerLevel level) {
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(GROVEKEEPER);
        if (type.isEmpty()) {
            return;
        }
        long alive = this.summons.stream().filter(summon -> summon.entity().getType() == type.get()).count();
        if (this.cooldown <= 0 && alive < GROVEKEEPER_MAX) {
            Entity keeper = type.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (keeper != null) {
                Vec3 spot = this.mob.position().add(this.mob.getRandom().nextDouble() * 4.0 - 2.0, 0.0, this.mob.getRandom().nextDouble() * 4.0 - 2.0);
                keeper.snapTo(spot.x, spot.y, spot.z, this.mob.getYRot(), 0.0f);
                this.recruit(level, keeper, Integer.MAX_VALUE);
                level.sendParticles(ParticleTypes.COMPOSTER, spot.x, spot.y + 1.0, spot.z, 20, 0.5, 1.0, 0.5, 0.05);
                this.mob.playSound(SoundEvents.CREAKING_ACTIVATE, 1.0f, 1.0f);
            }
            this.cooldown = GROVEKEEPER_COOLDOWN;
        }
        if (this.mob.isPassenger() || this.mob.factionClass() == FactionClass.PRIEST) {
            return;
        }
        this.summons.stream()
            .map(Summon::entity)
            .filter(entity -> entity.getType() == type.get() && entity.getPassengers().isEmpty() && this.mob.distanceTo(entity) < MOUNT_RANGE)
            .findFirst()
            .ifPresent(keeper -> {
                this.mob.flight().land();
                this.mob.startRiding(keeper);
            });
    }

    // Truffle: drift spores keep it fast, and a spore haze mends allies, weakens enemies and turns nearby beasts to its side.
    private void spores(ServerLevel level, boolean sees, double distance) {
        if (this.mob.tickCount % 40 == 0) {
            this.mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 1, false, false));
        }
        if (this.cooldown <= 0 && sees && distance < HAZE_RADIUS + 1.0) {
            this.hazeAt = this.mob.position();
            this.hazeTicks = HAZE_TICKS;
            this.cooldown = HAZE_COOLDOWN;
            this.mob.playSound(SoundEvents.PUFFER_FISH_BLOW_UP, 1.0f, 0.6f);
        }
    }

    private void tickHaze(ServerLevel level) {
        Vec3 at = this.hazeAt;
        if (at == null) {
            return;
        }
        if (--this.hazeTicks <= 0) {
            this.hazeAt = null;
            return;
        }
        if (this.hazeTicks % 4 == 0) {
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, at.x, at.y + 1.0, at.z, 12, HAZE_RADIUS * 0.5, 0.8, HAZE_RADIUS * 0.5, 0.0);
        }
        if (this.hazeTicks % 20 != 0) {
            return;
        }
        LivingEntity aim = this.mob.getTarget();
        AABB area = new AABB(at, at).inflate(HAZE_RADIUS, 2.0, HAZE_RADIUS);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (FactionCleric.ally(this.mob, living)) {
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0), this.mob);
                continue;
            }
            boolean player = living instanceof Player;
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, player ? 30 : 60, 0), this.mob);
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, player ? 30 : 60, player ? 0 : 1), this.mob);
            boolean beast = living instanceof Mob other && !(living instanceof FactionMember) && living.getMaxHealth() <= BOSS_HEALTH;
            if (beast && aim != null && aim != living && aim.isAlive()) {
                ((Mob) living).setTarget(aim);
            }
        }
    }

    // Shulk: homing shulker bullets from a regrowing charge, and spare healing potions thrown to hurt allies.
    private void shell(ServerLevel level, LivingEntity target, boolean sees, double distance) {
        if (this.secondCooldown <= 0 && this.shellCharge >= BULLET_COST && sees && distance > 3.0 && distance <= BULLET_RANGE) {
            ShulkerBullet bullet = new ShulkerBullet(level, this.mob, target, Direction.Axis.Y);
            level.addFreshEntity(bullet);
            this.mob.playSound(SoundEvents.SHULKER_SHOOT, 1.0f, 1.0f);
            this.shellCharge -= BULLET_COST;
            this.secondCooldown = BULLET_GAP;
        }
        if (this.shellPotions <= 0 || this.cooldown > 0) {
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
            this.cooldown = HEAL_THROW_COOLDOWN;
        }
    }

    // Arachnid: a web shot that snares the target and hauls it in.
    private void web(ServerLevel level, LivingEntity target, boolean sees, double distance) {
        if (this.cooldown > 0 || !sees || distance < WEB_MIN || distance > WEB_MAX) {
            return;
        }
        Vec3 pull = this.mob.position().subtract(target.position()).normalize().scale(0.9);
        target.push(pull.x, 0.3, pull.z);
        target.needsSync = true;
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2), this.mob);
        this.line(level, this.mob.getEyePosition(), target.getEyePosition(), ParticleTypes.ITEM_COBWEB);
        this.mob.playSound(SoundEvents.SPIDER_AMBIENT, 1.0f, 1.4f);
        this.cooldown = WEB_COOLDOWN;
    }

    // Phantom: calls two phantoms out of the dark against a player, and wails to show everything nearby when the target hides.
    private void phantom(ServerLevel level, LivingEntity target, boolean sees, double distance) {
        boolean sunlit = level.isBrightOutside() && level.canSeeSky(this.mob.blockPosition());
        if (this.cooldown <= 0 && target instanceof Player && sees && distance <= BULLET_RANGE && !sunlit) {
            for (int i = 0; i < 2; i++) {
                Entity phantom = EntityTypes.PHANTOM.create(level, EntitySpawnReason.MOB_SUMMONED);
                if (phantom != null) {
                    phantom.snapTo(this.mob.getX() + (i * 2 - 1), this.mob.getY() + 3.0, this.mob.getZ(), this.mob.getYRot(), 0.0f);
                    this.recruit(level, phantom, this.mob.tickCount + PHANTOM_LIFE);
                }
            }
            level.sendParticles(ParticleTypes.SMOKE, this.mob.getX(), this.mob.getY(1.0), this.mob.getZ(), 20, 0.6, 0.6, 0.6, 0.02);
            this.cooldown = PHANTOM_COOLDOWN;
        }
        if (this.secondCooldown <= 0 && this.unseenTicks >= WAIL_UNSEEN_TICKS) {
            AABB area = this.mob.getBoundingBox().inflate(WAIL_RANGE);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, other -> other != this.mob && !other.isShiftKeyDown() && !this.mob.considersEntityAsAlly(other))) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), this.mob);
            }
            this.mob.playSound(SoundEvents.PHANTOM_SWOOP, 1.5f, 0.6f);
            this.secondCooldown = WAIL_COOLDOWN;
        }
    }

    // Enderian: pearls away from an enemy that gets close.
    private void pearl(ServerLevel level, LivingEntity target, double distance) {
        if (this.cooldown > 0 || distance > PEARL_THREAT) {
            return;
        }
        Vec3 from = this.mob.position();
        Vec3 away = from.subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize();
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = (this.mob.getRandom().nextDouble() - 0.5) * 1.6;
            Vec3 dir = new Vec3(away.x * Math.cos(angle) - away.z * Math.sin(angle), 0.0, away.x * Math.sin(angle) + away.z * Math.cos(angle));
            Vec3 spot = from.add(dir.scale(8.0 + this.mob.getRandom().nextDouble() * 4.0));
            if (this.mob.randomTeleport(spot.x, spot.y, spot.z, true, state -> true)) {
                level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0, from.z, 30, 0.3, 0.6, 0.3, 0.2);
                this.mob.playSound(SoundEvents.ENDER_PEARL_THROW, 1.0f, 1.0f);
                this.mob.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
                this.mob.getNavigation().stop();
                break;
            }
        }
        this.cooldown = PEARL_COOLDOWN;
    }

    // Beastfolk: a sprung leap, in at a target for fighters and away from one for archers.
    private void pounce(ServerLevel level, LivingEntity target, boolean sees, double distance) {
        if (this.cooldown > 0 || !sees || !this.mob.onGround() || this.mob.isPassenger()) {
            return;
        }
        boolean melee = this.mob.factionClass().melee();
        if (melee ? distance < 4.0 || distance > 10.0 : distance > 3.0) {
            return;
        }
        Vec3 toward = target.position().subtract(this.mob.position()).multiply(1.0, 0.0, 1.0).normalize();
        Vec3 leap = (melee ? toward : toward.scale(-1.0)).scale(1.1);
        this.mob.setDeltaMovement(leap.x, 0.5, leap.z);
        level.sendParticles(ParticleTypes.POOF, this.mob.getX(), this.mob.getY(), this.mob.getZ(), 8, 0.3, 0.1, 0.3, 0.02);
        this.mob.playSound(SoundEvents.GOAT_LONG_JUMP, 1.0f, 1.2f);
        this.cooldown = POUNCE_COOLDOWN;
    }

    // Shulk: a hit from a living attacker sometimes blinks it away unhurt. True when the hit was dodged.
    boolean dodge(DamageSource source) {
        if (this.mob.origin() != FactionOrigin.SHULK || this.dodgeCooldown > 0 || !(source.getEntity() instanceof LivingEntity)) {
            return false;
        }
        if (this.mob.getRandom().nextFloat() >= DODGE_CHANCE) {
            return false;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            double x = this.mob.getX() + (this.mob.getRandom().nextDouble() - 0.5) * 8.0;
            double z = this.mob.getZ() + (this.mob.getRandom().nextDouble() - 0.5) * 8.0;
            if (this.mob.randomTeleport(x, this.mob.getY(), z, true, state -> true)) {
                this.mob.playSound(SoundEvents.SHULKER_TELEPORT, 1.0f, 1.0f);
                this.dodgeCooldown = DODGE_COOLDOWN;
                return true;
            }
        }
        return false;
    }

    // Piglin: nearby piglins and brutes rally against whatever hurts it.
    void rallyKin(ServerLevel level, LivingEntity attacker) {
        if (this.mob.origin() != FactionOrigin.PIGLIN || attacker instanceof AbstractPiglin) {
            return;
        }
        for (AbstractPiglin piglin : level.getEntitiesOfClass(AbstractPiglin.class, this.mob.getBoundingBox().inflate(KINSMEN_RANGE))) {
            piglin.setTarget(attacker);
        }
    }

    // A melee hit with the origin's bonuses: critical hits for piglins, beastfolk, airborne avians and burning blazeborn, and a fishman's water power.
    boolean hit(ServerLevel level, Entity target, Predicate<Entity> strike) {
        AttributeInstance damage = this.mob.getAttribute(Attributes.ATTACK_DAMAGE);
        boolean crit = this.mob.getRandom().nextFloat() < this.critChance();
        boolean wet = this.mob.origin() == FactionOrigin.FISHMAN && this.mob.isInWaterOrRain();
        if (damage != null && crit) {
            damage.addTransientModifier(new AttributeModifier(CRIT_MODIFIER, CRIT_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (damage != null && wet) {
            damage.addTransientModifier(new AttributeModifier(WET_MODIFIER, WET_BONUS, AttributeModifier.Operation.ADD_VALUE));
        }
        boolean landed;
        try {
            landed = strike.test(target);
        } finally {
            if (damage != null) {
                damage.removeModifier(CRIT_MODIFIER);
                damage.removeModifier(WET_MODIFIER);
            }
        }
        if (landed && crit) {
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
            this.mob.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
        }
        if (landed && target instanceof LivingEntity living && this.mob.origin() == FactionOrigin.ARACHNID && this.venomLull <= 0
            && this.mob.getRandom().nextFloat() < VENOM_CHANCE) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), this.mob);
            this.venomLull = VENOM_LULL;
        }
        return landed;
    }

    private float critChance() {
        return switch (this.mob.origin()) {
            case PIGLIN -> 0.28f + 0.18f * this.goldPieces();
            case BEASTFOLK -> 0.21f;
            case AVIAN -> this.mob.onGround() ? 0.0f : 0.28f;
            case BLAZEBORN -> this.mob.isOnFire() ? 0.35f : 0.0f;
            default -> 0.0f;
        };
    }

    private int goldPieces() {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack piece = this.mob.getItemBySlot(slot);
            if (BuiltInRegistries.ITEM.getKey(piece.getItem()).getPath().startsWith("golden_")) {
                count++;
            }
        }
        return count;
    }

    // Marks a summoned creature as the faction's, so the crew and the creature leave each other be, and tracks it until it expires.
    private void recruit(ServerLevel level, Entity summoned, int expires) {
        summoned.addTag(FactionMember.summonTag(this.mob.faction()));
        if (summoned instanceof Mob ally && this.mob.getTarget() != null) {
            ally.setTarget(this.mob.getTarget());
        }
        level.addFreshEntity(summoned);
        this.summons.add(new Summon(summoned, expires));
    }

    private void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
        Vec3 step = to.subtract(from);
        int points = Math.max(2, (int) (step.length() * 2.0));
        for (int i = 0; i <= points; i++) {
            Vec3 at = from.add(step.scale((double) i / points));
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
