package info.mudbourn.mmsdungeons.mobs.swing;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.factions.FactionWeapons;
import java.util.List;
import java.util.function.Predicate;
import net.bettercombat.BetterCombatMod;
import net.bettercombat.client.collision.CollisionHelper;
import net.bettercombat.client.collision.OrientedBoundingBox;
import net.bettercombat.client.collision.WeaponHitBoxes;
import net.bettercombat.config.ServerConfig;
import net.bettercombat.utils.SoundHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Lands a mob's Better Combat attack the way Better Combat lands a player's: its weapon hitbox, sweep, damage multiplier and swing sound.
public final class MobStrike {

    private static final double SHOULDER = 0.15;

    private MobStrike() {
    }

    // Hits the target and every other enemy inside the attack's hitbox along the way the mob faces; true when anything was hit.
    public static boolean land(ServerLevel level, Mob mob, LivingEntity target, FactionWeapons.Swing swing, boolean offHand, Predicate<LivingEntity> enemy) {
        ServerConfig config = BetterCombatMod.config;
        SoundHelper.playSound(level, mob, swing.attack().swingSound());
        List<LivingEntity> victims = victims(level, mob, target, swing, enemy);
        if (victims.isEmpty()) {
            return false;
        }
        double bonus = swing.attack().damageMultiplier() - 1.0;
        bonus += (offHand ? config.dual_wielding_off_hand_damage_multiplier : config.dual_wielding_main_hand_damage_multiplier) - 1.0;
        if (config.allow_reworked_sweeping && victims.size() > 1) {
            int extra = Math.min(config.reworked_sweeping_extra_target_count, victims.size() - 1);
            double penalty = config.reworked_sweeping_maximum_damage_penalty / config.reworked_sweeping_extra_target_count * extra;
            AttributeInstance sweep = mob.getAttribute(Attributes.SWEEPING_DAMAGE_RATIO);
            bonus += config.reworked_sweeping_maximum_damage_penalty * (sweep == null ? 0.0 : sweep.getValue()) - penalty;
            if (config.reworked_sweeping_plays_sound) {
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, mob.getSoundSource(), 1.0f, 1.0f);
            }
        }
        AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeModifier modifier = new AttributeModifier(MmsDungeons.id("faction_attack"), bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (damage != null && bonus != 0.0) {
            damage.addOrReplacePermanentModifier(modifier);
        }
        boolean hit = false;
        for (LivingEntity victim : victims) {
            if (config.allow_fast_attacks) {
                victim.setInvulnerableTime(0);
            }
            hit |= mob.doHurtTarget(level, victim);
        }
        if (damage != null) {
            damage.removeModifier(modifier.id());
        }
        return hit;
    }

    // Enemies inside the oriented hitbox and attack angle, with a clear line from the shoulder unless Better Combat allows hits through walls.
    private static List<LivingEntity> victims(ServerLevel level, Mob mob, LivingEntity target, FactionWeapons.Swing swing, Predicate<LivingEntity> enemy) {
        double range = swing.range();
        double angle = Mth.clamp(swing.attack().angle(), 0.0, 360.0);
        boolean spin = angle > 180.0;
        Vec3 origin = mob.getEyePosition().subtract(0.0, mob.getBbHeight() * SHOULDER, 0.0);
        Vec3 size = WeaponHitBoxes.createHitbox(swing.attack().hitbox(), range, spin);
        OrientedBoundingBox box = new OrientedBoundingBox(origin, size, mob.getXRot(), mob.getYHeadRot());
        if (!spin) {
            box = box.offsetAlongAxisZ(size.z / 2.0);
        }
        box.updateVertex();
        OrientedBoundingBox hitbox = box;
        AABB search = mob.getBoundingBox().inflate(range * BetterCombatMod.config.target_search_range_multiplier + 1.0);
        return level.getEntitiesOfClass(LivingEntity.class, search, entity -> entity != mob && entity.isAlive() && entity.isPickable() && entity.isAttackable())
            .stream()
            .filter(entity -> entity == target || !mob.isAlliedTo(entity) && enemy.test(entity))
            .filter(entity -> hitbox.intersects(entity.getBoundingBox().inflate(entity.getPickRadius()))
                || hitbox.contains(entity.position().add(0.0, entity.getBbHeight() / 2.0f, 0.0)))
            .filter(entity -> inArc(level, mob, origin, hitbox.axisZ, range, angle, entity))
            .toList();
    }

    private static boolean inArc(ServerLevel level, Mob mob, Vec3 origin, Vec3 facing, double range, double angle, Entity entity) {
        Vec3 nearest = CollisionHelper.distanceVector(origin, entity.getBoundingBox());
        Vec3 middle = entity.position().add(0.0, entity.getBbHeight() / 2.0f, 0.0).subtract(origin);
        if (nearest.length() > range) {
            return false;
        }
        double half = angle / 2.0;
        boolean inAngle = angle == 0.0 || CollisionHelper.angleBetween(middle, facing) <= half || CollisionHelper.angleBetween(nearest, facing) <= half;
        if (!inAngle) {
            return false;
        }
        return BetterCombatMod.config.allow_attacking_thru_walls || clear(level, mob, origin, origin.add(nearest)) || clear(level, mob, origin, origin.add(middle));
    }

    private static boolean clear(ServerLevel level, Mob mob, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob)).getType() != HitResult.Type.BLOCK;
    }
}
