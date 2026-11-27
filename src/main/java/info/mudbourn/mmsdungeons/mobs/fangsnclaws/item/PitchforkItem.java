package info.mudbourn.mmsdungeons.mobs.fangsnclaws.item;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEffects;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.ThrownPitchfork;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

// A pitchfork that is thrown like a trident and brands whatever it hits with flame.
public class PitchforkItem extends Item implements ProjectileItem {

    private static final int USE_TICKS = 72000;
    private static final int MIN_CHARGE_TICKS = 10;
    private static final float THROW_POWER = 2.5f;

    private final boolean hellfire;

    public PitchforkItem(Properties properties, boolean hellfire) {
        super(properties);
        this.hellfire = hellfire;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TRIDENT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.nextDamageWillBreak()) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingTicks) {
        if (!(user instanceof Player player)) {
            return false;
        }
        int charge = this.getUseDuration(stack, user) - remainingTicks;
        if (charge < MIN_CHARGE_TICKS || stack.nextDamageWillBreak()) {
            return false;
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (level instanceof ServerLevel server) {
            stack.hurtWithoutBreaking(1, player);
            ItemStack thrown = stack.consumeAndReturn(1, player);
            ThrownPitchfork pitchfork = Projectile.spawnProjectileFromRotation(
                ThrownPitchfork::new,
                server,
                thrown,
                player,
                0.0f,
                THROW_POWER,
                1.0f
            );
            if (player.hasInfiniteMaterials()) {
                pitchfork.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            server.playSound(null, pitchfork, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        return true;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (this.hellfire) {
            FangsNClawsEffects.hellfireFlamebrand(target, attacker);
        } else {
            FangsNClawsEffects.flamebrand(target, attacker);
        }
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        ThrownPitchfork pitchfork = new ThrownPitchfork(
            level,
            position.x(),
            position.y(),
            position.z(),
            stack.copyWithCount(1)
        );
        pitchfork.pickup = AbstractArrow.Pickup.ALLOWED;
        return pitchfork;
    }
}
