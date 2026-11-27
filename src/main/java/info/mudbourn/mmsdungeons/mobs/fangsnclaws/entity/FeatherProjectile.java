package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsItems;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

// A giant feather thrown by a Shrike.
public class FeatherProjectile extends AbstractArrow {

    private static final float DAMAGE = 6.0f;

    public FeatherProjectile(EntityType<? extends FeatherProjectile> type, Level level) {
        super(type, level);
    }

    public FeatherProjectile(Level level, LivingEntity shooter) {
        super(FangsNClawsMobs.FEATHER_PROJECTILE, shooter, level, new ItemStack(FangsNClawsItems.GIANT_FEATHER), null);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(FangsNClawsItems.GIANT_FEATHER);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity owner = this.getOwner();
        Entity hit = result.getEntity();
        if (this.level() instanceof ServerLevel level && hit instanceof LivingEntity living && hit != owner) {
            living.hurtServer(level, this.damageSources().arrow(this, owner != null ? owner : this), DAMAGE);
        }
        this.discard();
    }
}
