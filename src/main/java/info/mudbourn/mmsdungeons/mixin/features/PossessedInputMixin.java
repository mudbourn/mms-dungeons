package info.mudbourn.mmsdungeons.mixin.features;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentEffects;
import info.mudbourn.mmsdungeons.mobs.orpiment.item.AmberCageItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Flips the movement keys while Possessed and locks them while an Amber Cage is raised.
@Mixin(KeyboardInput.class)
public abstract class PossessedInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void mmsDungeons$bendInput(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ClientInput input = (ClientInput) (Object) this;
        if (this.mmsDungeons$caged(player)) {
            input.keyPresses = new Input(false, false, false, false, false, false, false);
            ((ClientInputAccessor) input).mmsDungeons$setMoveVector(Vec2.ZERO);
        } else if (player.hasEffect(OrpimentEffects.POSSESSED)) {
            Input keys = input.keyPresses;
            input.keyPresses = new Input(keys.backward(), keys.forward(), keys.right(), keys.left(), keys.jump(), keys.shift(), keys.sprint());
            ((ClientInputAccessor) input).mmsDungeons$setMoveVector(input.getMoveVector().negated());
        }
    }

    private boolean mmsDungeons$caged(LocalPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof AmberCageItem && AmberCageItem.isActive(stack)) {
                return true;
            }
        }
        return false;
    }
}
