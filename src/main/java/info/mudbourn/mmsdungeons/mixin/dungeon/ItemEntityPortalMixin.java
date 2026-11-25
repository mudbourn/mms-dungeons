package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.DungeonPortalFrames;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Lets a dropped Recovery Cross light a dungeon portal frame.
@Mixin(ItemEntity.class)
public abstract class ItemEntityPortalMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void mmsDungeons$lightPortal(CallbackInfo ci) {
        DungeonPortalFrames.onItemTick((ItemEntity) (Object) this);
    }
}
