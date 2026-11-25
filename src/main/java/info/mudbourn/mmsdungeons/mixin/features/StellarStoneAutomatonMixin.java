package info.mudbourn.mmsdungeons.mixin.features;

import fr.factionbedrock.aerialhell.Entity.Monster.StellarStoneAutomatonEntity;
import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Makes the automaton sound like any other mob, the way a zombie does. Aerial Hell ships its sounds in stereo, which
// Minecraft plays at full volume however far away the source is; these mono copies fade out over the normal 16 blocks.
// Its idle sound was the activation screech repeating every few seconds, so it stays silent.
@Mixin(StellarStoneAutomatonEntity.class)
public abstract class StellarStoneAutomatonMixin {

    @Unique
    private static final SoundEvent HURT = SoundEvent.createVariableRangeEvent(MmsDungeons.id("entity.stellar_stone_automaton.hurt"));
    @Unique
    private static final SoundEvent DEATH = SoundEvent.createVariableRangeEvent(MmsDungeons.id("entity.stellar_stone_automaton.death"));

    @Inject(method = "getAmbientSound", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$noIdleScreech(CallbackInfoReturnable<SoundEvent> cir) {
        cir.setReturnValue(null);
    }

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$monoHurt(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        cir.setReturnValue(HURT);
    }

    @Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$monoDeath(CallbackInfoReturnable<SoundEvent> cir) {
        cir.setReturnValue(DEATH);
    }
}
