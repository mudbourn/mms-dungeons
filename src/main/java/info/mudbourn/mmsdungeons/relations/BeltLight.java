package info.mudbourn.mmsdungeons.relations;

import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.LivingEntity;

// Reads the Trinkets belt slot, kept apart so the mod is only touched when it is loaded.
final class BeltLight {

    private static final String GROUP = "legs";
    private static final String SLOT = "belt";

    private BeltLight() {
    }

    static boolean carries(LivingEntity entity) {
        TrinketAttachment attachment = TrinketsApi.getAttachment(entity);
        if (attachment == null) {
            return false;
        }
        for (TrinketInventory inventory : attachment.getInventories().values()) {
            if (GROUP.equals(inventory.slotType().group()) && SLOT.equals(inventory.slotType().name())) {
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                    if (LightSources.isLight(inventory.getItem(slot))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
