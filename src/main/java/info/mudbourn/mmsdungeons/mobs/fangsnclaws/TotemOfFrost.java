package info.mudbourn.mmsdungeons.mobs.fangsnclaws;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

// A held Totem of Frost heals its bearer at 25% health or less and freezes the enemies around.
public final class TotemOfFrost {

    private static final float TRIGGER_HEALTH = 0.25f;
    private static final double RANGE = 10.0;
    private static final int FREEZE_TICKS = 100;
    private static final int FREEZE_SLOWNESS = 9;

    private TotemOfFrost() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !tryActivate(entity));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
            if (entity.getHealth() > 0.0f && entity.getHealth() <= entity.getMaxHealth() * TRIGGER_HEALTH) {
                tryActivate(entity);
            }
        });
    }

    private static boolean tryActivate(LivingEntity entity) {
        if (!(entity instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        ItemStack totem = heldTotem(player);
        if (totem.isEmpty()) {
            return false;
        }
        totem.shrink(1);
        player.setHealth(player.getMaxHealth());
        AABB area = player.getBoundingBox().inflate(RANGE);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (victim.isAlive() && isEnemyOf(player, victim)) {
                freeze(victim);
            }
        }
        playEffects(level, player);
        return true;
    }

    private static ItemStack heldTotem(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(FangsNClawsItems.TOTEM_OF_FROST)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isEnemyOf(Player player, LivingEntity victim) {
        return victim instanceof Enemy || victim instanceof Mob mob && mob.getTarget() == player;
    }

    private static void freeze(LivingEntity victim) {
        victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FREEZE_TICKS, FREEZE_SLOWNESS));
        victim.setTicksFrozen(victim.getTicksRequiredToFreeze() + FREEZE_TICKS);
    }

    private static void playEffects(ServerLevel level, Player player) {
        double y = player.getY() + player.getBbHeight() * 0.5;
        level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), y, player.getZ(), 120, 3.0, 1.5, 3.0, 0.3);
        level.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
            player.getX(),
            y,
            player.getZ(),
            80,
            2.5,
            1.2,
            2.5,
            0.2
        );
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.5f, 0.5f);
    }
}
