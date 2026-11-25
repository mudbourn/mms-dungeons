package info.mudbourn.mmsdungeons.mobs.kobolds.entity;

import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldTags;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldBowGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldCrossbowGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldPotionGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldRevengeGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldTargetGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldTradeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class KoboldEngineer extends AbstractKobold {

    public KoboldEngineer(EntityType<KoboldEngineer> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(0, new KoboldRevengeGoal(this));
        this.goalSelector.addGoal(1, new KoboldPotionGoal(this));
        this.goalSelector.addGoal(1, new KoboldCrossbowGoal<>(this, 1.0, 12.0f));
        this.goalSelector.addGoal(1, new KoboldBowGoal<>(this, 1.0, 20, 15.0f));
        this.goalSelector.addGoal(1, new KoboldTradeGoal(this, "engineer_loot"));
        this.targetSelector.addGoal(2, new KoboldTargetGoal(this));
    }

    @Override
    public boolean isPreferredWeapon(ItemStack stack) {
        return stack.is(KoboldTags.ENGINEER_WEAPONS);
    }
}
