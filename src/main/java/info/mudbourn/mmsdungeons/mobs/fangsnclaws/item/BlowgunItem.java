package info.mudbourn.mmsdungeons.mobs.fangsnclaws.item;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEnchantments;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsItems;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.PoisonousDart;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

// A blowgun that fires Poisonous Darts harder the longer it is charged.
public class BlowgunItem extends Item {

    private static final int USE_TICKS = 72000;
    private static final int BASE_CHARGE_TICKS = 20;
    private static final int QUICK_CHARGE_TICKS = 5;
    private static final int MIN_CHARGE_TICKS = 4;
    private static final float MIN_CHARGE = 0.2f;
    private static final float MAX_VELOCITY = 2.2f;
    private static final float[] SPREAD = {0.0f, -10.0f, 10.0f};

    public BlowgunItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (this.findDarts(player).isEmpty() && !player.hasInfiniteMaterials()) {
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
        float charge = this.charge(stack, level, this.getUseDuration(stack, user) - remainingTicks);
        if (charge < MIN_CHARGE || !(level instanceof ServerLevel server)) {
            return false;
        }
        ItemStack darts = this.findDarts(player);
        if (darts.isEmpty() && !player.hasInfiniteMaterials()) {
            return false;
        }
        boolean multishot = FangsNClawsEnchantments.level(stack, level, Enchantments.MULTISHOT) > 0;
        int count = multishot ? SPREAD.length : 1;
        for (int i = 0; i < count; i++) {
            PoisonousDart dart = new PoisonousDart(server, player);
            if (i > 0) {
                dart.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            dart.shootFromRotation(
                player,
                player.getXRot(),
                player.getYRot() + SPREAD[i],
                0.0f,
                charge * MAX_VELOCITY,
                1.0f
            );
            server.addFreshEntity(dart);
        }
        stack.hurtAndBreak(1, player, player.getUsedItemHand());
        server.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            FangsNClawsSounds.BLOWGUN_SHOOT,
            SoundSource.PLAYERS,
            0.8f,
            1.4f + server.getRandom().nextFloat() * 0.2f
        );
        if (!player.hasInfiniteMaterials()) {
            darts.shrink(1);
        }
        return true;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingTicks) {
        if (level.isClientSide()) {
            return;
        }
        int duration = this.chargeDuration(stack, level);
        int ticks = this.getUseDuration(stack, user) - remainingTicks;
        if (ticks == Math.round(duration * MIN_CHARGE)) {
            int quickCharge = FangsNClawsEnchantments.level(stack, level, Enchantments.QUICK_CHARGE);
            level.playSound(
                null,
                user.getX(),
                user.getY(),
                user.getZ(),
                startSound(quickCharge),
                SoundSource.PLAYERS,
                0.5f,
                1.0f
            );
        }
    }

    private static SoundEvent startSound(int quickCharge) {
        Holder<SoundEvent> sound = switch (quickCharge) {
            case 1 -> SoundEvents.CROSSBOW_QUICK_CHARGE_1;
            case 2 -> SoundEvents.CROSSBOW_QUICK_CHARGE_2;
            case 3 -> SoundEvents.CROSSBOW_QUICK_CHARGE_3;
            default -> SoundEvents.CROSSBOW_LOADING_START;
        };
        return sound.value();
    }

    private int chargeDuration(ItemStack stack, Level level) {
        int quickCharge = FangsNClawsEnchantments.level(stack, level, Enchantments.QUICK_CHARGE);
        return Math.max(MIN_CHARGE_TICKS, BASE_CHARGE_TICKS - QUICK_CHARGE_TICKS * quickCharge);
    }

    private float charge(ItemStack stack, Level level, int ticks) {
        int duration = this.chargeDuration(stack, level);
        return Math.min((float) ticks / duration, 1.0f);
    }

    private ItemStack findDarts(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.is(FangsNClawsItems.POISONOUS_DART)) {
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }
}
