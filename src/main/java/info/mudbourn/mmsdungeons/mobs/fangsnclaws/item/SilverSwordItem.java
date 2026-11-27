package info.mudbourn.mmsdungeons.mobs.fangsnclaws.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// A sword whose every hit adds magic damage that ignores armor.
public class SilverSwordItem extends Item {

    private static final float MAGIC_DAMAGE = 2.0f;

    public SilverSwordItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel level) {
            target.setInvulnerableTime(0);
            target.hurtServer(level, level.damageSources().indirectMagic(attacker, attacker), MAGIC_DAMAGE);
        }
    }
}
