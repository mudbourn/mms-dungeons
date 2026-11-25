package info.mudbourn.mmsdungeons.mixin.features;

import fr.factionbedrock.aerialhell.Entity.Monster.StellarStoneAutomatonEntity;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Aerial Hell uses the automaton's activation screech as its idle sound, so it repeats every few seconds. This silences only the idle sound.
@Mixin(StellarStoneAutomatonEntity.class)
public abstract class StellarStoneAutomatonMixin {

    @Inject(method = "getAmbientSound", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$noIdleScreech(CallbackInfoReturnable<SoundEvent> cir) {
        cir.setReturnValue(null);
    }
}
