package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsGear;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// A thrown tomahawk that always flies back to its owner after a hit and wears down as it does.
public class ThrownTomahawk extends ThrownGear {

    public static final float DAMAGE = 6.0f;

    private static final int RETURN_PULL = 3;

    public ThrownTomahawk(EntityType<? extends ThrownTomahawk> type, Level level) {
        super(type, level);
    }

    public ThrownTomahawk(ServerLevel level, LivingEntity shooter, ItemStack stack) {
        super(FangsNClawsMobs.THROWN_TOMAHAWK, shooter, level, stack);
        this.refreshReturnPull();
    }

    public ThrownTomahawk(Level level, double x, double y, double z, ItemStack stack) {
        super(FangsNClawsMobs.THROWN_TOMAHAWK, x, y, z, level, stack);
        this.refreshReturnPull();
    }

    @Override
    protected float thrownDamage() {
        return DAMAGE;
    }

    @Override
    protected int returnPull(ServerLevel level, ItemStack stack) {
        return RETURN_PULL;
    }

    @Override
    protected SoundEvent hitSound() {
        return SoundEvents.PLAYER_ATTACK_STRONG;
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected void onDamaged(LivingEntity victim) {
        ItemStack stack = this.getPickupItemStackOrigin();
        if (this.level() instanceof ServerLevel level && stack.isDamageableItem()) {
            ServerPlayer owner = this.getOwner() instanceof ServerPlayer player ? player : null;
            stack.hurtAndBreak(1, level, owner, broken -> this.discard());
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(FangsNClawsGear.TOMAHAWK);
    }
}
