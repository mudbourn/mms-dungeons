package info.mudbourn.mmsdungeons.mixin.combat;

import info.mudbourn.mmsdungeons.mobs.swing.ArmedSwings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
abstract class MobSwingMixin implements ArmedSwings.Swinger {

    @Unique
    private final ArmedSwings.State mmsDungeons$swingState = new ArmedSwings.State();

    @Override
    public ArmedSwings.State mmsDungeons$swing() {
        return this.mmsDungeons$swingState;
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$startSwing(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
        Boolean result = ArmedSwings.intercept((Mob) (Object) this, level, target);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void mmsDungeons$landSwing(CallbackInfo ci) {
        ArmedSwings.tick((Mob) (Object) this);
    }
}
