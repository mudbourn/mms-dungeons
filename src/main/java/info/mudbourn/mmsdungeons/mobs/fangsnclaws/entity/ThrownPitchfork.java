package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEffects;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsGear;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

// A thrown pitchfork that brands what it hits with flame and returns with Loyalty.
public class ThrownPitchfork extends ThrownGear {

    private static final float FIRE_DAMAGE = 5.0f;
    private static final float HELLFIRE_DAMAGE = 7.0f;

    public ThrownPitchfork(EntityType<? extends ThrownPitchfork> type, Level level) {
        super(type, level);
    }

    public ThrownPitchfork(ServerLevel level, LivingEntity shooter, ItemStack stack) {
        super(FangsNClawsMobs.THROWN_PITCHFORK, shooter, level, stack);
        this.refreshReturnPull();
    }

    public ThrownPitchfork(Level level, double x, double y, double z, ItemStack stack) {
        super(FangsNClawsMobs.THROWN_PITCHFORK, x, y, z, level, stack);
        this.refreshReturnPull();
    }

    private boolean hellfire() {
        return this.getPickupItemStackOrigin().is(FangsNClawsGear.HELLFIRE_PITCHFORK);
    }

    @Override
    protected float thrownDamage() {
        return this.hellfire() ? HELLFIRE_DAMAGE : FIRE_DAMAGE;
    }

    @Override
    protected int returnPull(ServerLevel level, ItemStack stack) {
        return EnchantmentHelper.getTridentReturnToOwnerAcceleration(level, stack, this);
    }

    @Override
    protected SoundEvent hitSound() {
        return SoundEvents.TRIDENT_HIT;
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected DamageSource damageSource(Entity owner) {
        return this.damageSources().trident(this, owner == null ? this : owner);
    }

    @Override
    protected void onDamaged(LivingEntity victim) {
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        if (this.hellfire()) {
            FangsNClawsEffects.hellfireFlamebrand(victim, owner);
        } else {
            FangsNClawsEffects.flamebrand(victim, owner);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(FangsNClawsGear.FIRE_PITCHFORK);
    }
}
