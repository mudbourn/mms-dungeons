package info.mudbourn.mmsdungeons.mobs.knightquest.entity;

import info.mudbourn.mmsdungeons.mobs.knightquest.KnightQuestMobs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

// An axe thrown by a Swampman. It cannot be picked up.
public class SwampmanAxe extends AbstractArrow {

    public SwampmanAxe(EntityType<? extends SwampmanAxe> type, Level level) {
        super(type, level);
    }

    public SwampmanAxe(Level level, LivingEntity shooter) {
        super(KnightQuestMobs.SWAMPMAN_AXE, shooter, level, new ItemStack(Items.IRON_AXE), null);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.IRON_AXE);
    }
}
