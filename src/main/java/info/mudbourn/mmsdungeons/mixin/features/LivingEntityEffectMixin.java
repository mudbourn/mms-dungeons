package info.mudbourn.mmsdungeons.mixin.features;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsArmor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Lets Scorpion armor refuse the effect its piece protects against.
@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectMixin {

    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$scorpionArmor(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (FangsNClawsArmor.blocksEffect((LivingEntity) (Object) this, effect.getEffect())) {
            cir.setReturnValue(false);
        }
    }
}
