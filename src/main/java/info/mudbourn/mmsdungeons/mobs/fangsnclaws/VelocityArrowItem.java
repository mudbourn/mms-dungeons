package info.mudbourn.mmsdungeons.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.VelocityArrow;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// The Velocity Arrow item, which fires its own arrow entity.
public class VelocityArrowItem extends ArrowItem {

    public VelocityArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, ItemStack weapon) {
        return new VelocityArrow(level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        ItemStack ammo = stack.copyWithCount(1);
        VelocityArrow arrow = new VelocityArrow(level, position.x(), position.y(), position.z(), ammo);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }
}
