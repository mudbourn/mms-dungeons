package info.mudbourn.mmsdungeons.mobs.fangsnclaws.item;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEffects;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEnchantments;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// A dagger whose hit from behind makes the target bleed.
public class DaggerItem extends Item {

    private static final double BEHIND_DOT = -0.766;
    private static final int COOLDOWN_TICKS = 120;
    private static final int BLEED_TICKS = 200;
    private static final int SPEED_TICKS = 100;
    private static final float CRITICAL_PER_LEVEL = 0.5f;

    private final int bleedAmplifier;

    public DaggerItem(Properties properties, int bleedAmplifier) {
        super(properties);
        this.bleedAmplifier = bleedAmplifier;
    }

    public static boolean isBackstab(LivingEntity target, LivingEntity attacker) {
        Vec3 facing = Vec3.directionFromRotation(target.getXRot(), target.yHeadRot);
        Vec3 toAttacker = attacker.position().subtract(target.position());
        return toAttacker.lengthSqr() >= 1.0E-6 && facing.dot(toAttacker.normalize()) < BEHIND_DOT;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel level
            && attacker instanceof Player player
            && isBackstab(target, attacker)
            && !player.getCooldowns().isOnCooldown(stack)) {
            this.backstab(level, stack, target, player);
        }
    }

    private void backstab(ServerLevel level, ItemStack stack, LivingEntity target, Player player) {
        int quickKiller = FangsNClawsEnchantments.level(stack, level, FangsNClawsEnchantments.QUICK_KILLER);
        float cooldownScale = switch (quickKiller) {
            case 1 -> 0.75f;
            case 2 -> 0.5f;
            default -> 1.0f;
        };
        FangsNClawsEffects.bleed(target, BLEED_TICKS, this.bleedAmplifier);
        player.getCooldowns().addCooldown(stack, Math.round(COOLDOWN_TICKS * cooldownScale));
        level.playSound(
            null,
            target.getX(),
            target.getY(),
            target.getZ(),
            FangsNClawsSounds.BACKSTAB_IMPACT,
            SoundSource.PLAYERS,
            1.0f,
            0.9f + level.getRandom().nextFloat() * 0.2f
        );
        int critical = FangsNClawsEnchantments.level(stack, level, FangsNClawsEnchantments.CRITICAL_BACKSTAB);
        if (critical > 0) {
            float bonus = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * critical * CRITICAL_PER_LEVEL;
            target.setInvulnerableTime(0);
            target.hurtServer(level, level.damageSources().playerAttack(player), bonus);
            level.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS,
                0.8f,
                1.0f
            );
            level.sendParticles(
                ParticleTypes.CRIT,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                8,
                0.3,
                0.3,
                0.3,
                0.1
            );
        }
        if (quickKiller > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, SPEED_TICKS, quickKiller - 1));
        }
    }
}
