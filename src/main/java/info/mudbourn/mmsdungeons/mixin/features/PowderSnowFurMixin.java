package info.mudbourn.mmsdungeons.mixin.features;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsArmor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Lets Fur Boots walk on powder snow.
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowFurMixin {

    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void mmsDungeons$furBoots(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity living && FangsNClawsArmor.wearsFurBoots(living)) {
            cir.setReturnValue(true);
        }
    }
}
