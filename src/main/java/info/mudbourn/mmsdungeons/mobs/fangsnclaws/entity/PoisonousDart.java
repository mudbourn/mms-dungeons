package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEffects;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsItems;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

// A thrown dart that hurts a little and leaves its target venomed.
public class PoisonousDart extends AbstractArrow {

    private static final float DAMAGE = 2.0f;
    private static final int MOB_VENOM_TICKS = 100;
    private static final int PLAYER_VENOM_TICKS = 200;

    public PoisonousDart(EntityType<? extends PoisonousDart> type, Level level) {
        super(type, level);
    }

    public PoisonousDart(Level level, LivingEntity shooter) {
        super(FangsNClawsMobs.POISONOUS_DART, shooter, level, new ItemStack(FangsNClawsItems.POISONOUS_DART), null);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(FangsNClawsItems.POISONOUS_DART);
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return FangsNClawsSounds.DART_HIT;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity owner = this.getOwner();
        Entity hit = result.getEntity();
        if (this.level() instanceof ServerLevel level && hit instanceof LivingEntity living && hit != owner) {
            living.hurtServer(level, this.damageSources().arrow(this, owner != null ? owner : this), DAMAGE);
            if (!living.isDeadOrDying()) {
                FangsNClawsEffects.venom(living, owner instanceof DartGoblin ? MOB_VENOM_TICKS : PLAYER_VENOM_TICKS);
            }
        }
        this.playSound(FangsNClawsSounds.DART_HIT, 1.0f, 1.2f / (this.random.nextFloat() * 0.2f + 0.9f));
        this.discard();
    }
}
