package info.mudbourn.mmsdungeons.mixin.origins;

import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionMember;
import io.github.apace100.apoli.power.factory.condition.EntityConditions;
import io.github.apace100.apoli.util.Comparison;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Answers Origins' hunger and saturation conditions for a faction human from the food in its pack, where a player's would read its hunger bar.
@Mixin(value = EntityConditions.class, remap = false)
public abstract class HungerConditionMixin {

    @Inject(method = "lambda$register$18", at = @At("HEAD"), cancellable = true, require = 0)
    private static void mmsDungeons$foodLevel(SerializableData.Instance data, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof FactionMember member) {
            cir.setReturnValue(((Comparison) data.get("comparison")).compare(member.foodLevel(), data.getInt("compare_to")));
        }
    }

    @Inject(method = "lambda$register$19", at = @At("HEAD"), cancellable = true, require = 0)
    private static void mmsDungeons$saturation(SerializableData.Instance data, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof FactionMember member) {
            cir.setReturnValue(((Comparison) data.get("comparison")).compare(member.saturation(), data.getFloat("compare_to")));
        }
    }
}
