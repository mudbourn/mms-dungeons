package info.mudbourn.mmsdungeons.mixin.features;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Opens a mob's target goal list to code that adds goals after the mob is built.
@Mixin(Mob.class)
public interface MobAccessor {

    @Accessor("targetSelector")
    GoalSelector mmsDungeons$getTargetSelector();
}
