package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsItems;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

// An arrow that flies 20% faster and launches whatever it strikes.
public class VelocityArrow extends AbstractArrow {

    private static final float SPEED_FACTOR = 1.2f;
    private static final double LAUNCH_SPEED = 3.0;

    public VelocityArrow(EntityType<? extends VelocityArrow> type, Level level) {
        super(type, level);
    }

    public VelocityArrow(Level level, LivingEntity shooter, ItemStack ammo, ItemStack weapon) {
        super(FangsNClawsMobs.VELOCITY_ARROW, shooter, level, ammo, weapon);
    }

    public VelocityArrow(Level level, double x, double y, double z, ItemStack ammo) {
        super(FangsNClawsMobs.VELOCITY_ARROW, x, y, z, level, ammo, null);
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity * SPEED_FACTOR, inaccuracy);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(FangsNClawsItems.VELOCITY_ARROW);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Vec3 launch = this.getDeltaMovement().normalize().scale(LAUNCH_SPEED);
        super.onHitEntity(result);
        result.getEntity().setDeltaMovement(launch);
        result.getEntity().needsSync = true;
    }
}
