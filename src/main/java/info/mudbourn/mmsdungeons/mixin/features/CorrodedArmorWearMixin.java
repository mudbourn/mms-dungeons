package info.mudbourn.mmsdungeons.mixin.features;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// Doubles the durability that worn armor loses while the wearer is Corroded.
@Mixin(LivingEntity.class)
public abstract class CorrodedArmorWearMixin {

    private static final int WEAR_MULTIPLIER = 2;

    @ModifyArg(
        method = "doHurtEquipment",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"
        ),
        index = 0
    )
    private int mmsDungeons$corrodedWear(int amount) {
        LivingEntity self = (LivingEntity) (Object) this;
        return self.hasEffect(FangsNClawsEffects.CORRODED) ? amount * WEAR_MULTIPLIER : amount;
    }
}
