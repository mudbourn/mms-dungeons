package info.mudbourn.mmsdungeons.mobs.kobolds.entity;

import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldBowGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldCrossbowGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldMeleeGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldPotionGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldRevengeGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldShieldGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldTargetGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldTradeGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldTridentGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldWarriorGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class Kobold extends AbstractKobold {

    public Kobold(EntityType<Kobold> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(0, new KoboldRevengeGoal(this));
        this.goalSelector.addGoal(1, new KoboldPotionGoal(this));
        this.goalSelector.addGoal(1, new KoboldShieldGoal(this));
        this.goalSelector.addGoal(1, new KoboldTridentGoal(this, 1.0, 40, 12.0f));
        this.goalSelector.addGoal(1, new KoboldCrossbowGoal(this, 1.0, 12.0f));
        this.goalSelector.addGoal(1, new KoboldBowGoal(this, 1.0, 20, 15.0f));
        this.goalSelector.addGoal(1, new KoboldTradeGoal(this, "trader_loot"));
        this.goalSelector.addGoal(1, new KoboldWarriorGoal(this));
        this.goalSelector.addGoal(2, new KoboldMeleeGoal(this, 1.2, false));
        this.targetSelector.addGoal(2, new KoboldTargetGoal(this));
    }
}
