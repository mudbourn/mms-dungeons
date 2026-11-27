package info.mudbourn.mmsdungeons.mobs.factions.entity;

import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;

// The back and belt slots Combat Amenities adds to the equipment slots, found by name so the mod stays optional.
public final class FactionSlots {

    private static final EquipmentSlot BACK = find("COMBATAMENITIES_BACKSLOT");
    private static final EquipmentSlot BELT = find("COMBATAMENITIES_BELTSLOT");

    private FactionSlots() {
    }

    public static @Nullable EquipmentSlot back() {
        return BACK;
    }

    public static @Nullable EquipmentSlot belt() {
        return BELT;
    }

    private static @Nullable EquipmentSlot find(String name) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.name().equals(name)) {
                return slot;
            }
        }
        return null;
    }
}
