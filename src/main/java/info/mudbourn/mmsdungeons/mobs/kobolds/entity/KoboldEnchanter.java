package info.mudbourn.mmsdungeons.mobs.kobolds.entity;

import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldTags;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldPotionGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldTradeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class KoboldEnchanter extends AbstractKobold {

    public KoboldEnchanter(EntityType<KoboldEnchanter> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.2));
        this.goalSelector.addGoal(1, new KoboldTradeGoal(this, "enchanter_loot"));
        this.goalSelector.addGoal(1, new KoboldPotionGoal(this));
    }

    @Override
    protected boolean canReplaceCurrentItem(ItemStack drop, ItemStack held, EquipmentSlot slot) {
        return (drop.is(KoboldTags.ARMOR) || drop.is(Items.EMERALD)) && super.canReplaceCurrentItem(drop, held, slot);
    }
}
